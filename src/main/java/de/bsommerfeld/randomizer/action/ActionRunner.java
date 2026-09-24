package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Player;
import de.bsommerfeld.randomizer.input.GameInput;
import de.bsommerfeld.randomizer.input.Keys;
import de.bsommerfeld.randomizer.input.Keys.Key;
import de.bsommerfeld.randomizer.input.UserKeys;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * The randomizer itself. On its own thread it waits a random time, plays a random action and
 * starts over, until stopped.
 *
 * <p>An action that comes due while the gate is closed or CS2 is not in front is dropped, not
 * queued. Queued actions would all fire the moment freezetime ends, which is predictable.
 *
 * <p>The user's own keys win. When the user lets go of the key an action holds, that key goes up
 * and down again at once, so their key up does not end the action. When the action ends while the
 * user holds the same key, the key up is left out, because the game would take it for theirs.
 */
public final class ActionRunner {

    /** What the user can change while the randomizer runs. Read anew for every wait and every pick. */
    public record Settings(int minWaitSeconds, int maxWaitSeconds, List<Action> enabledActions) {
    }

    /** {@link Thread#sleep(long)}, replaceable so tests do not wait. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /** A {@link Step} with its command resolved to the key CS2 has on it. */
    private record KeyPress(String keyName, Key key, Press press) {
    }

    /** An action with the routes it can take right now, every one of them on keys that can be sent. */
    private record Candidate(Action action, List<List<KeyPress>> routes) {
    }

    /** A clicked key is down this long per click. */
    private static final int CLICK_DOWN_MIN_MILLIS = 30;
    private static final int CLICK_DOWN_MAX_MILLIS = 60;

    private final GameInput input;
    private final BooleanSupplier gateOpen;
    private final Supplier<Player> player;
    private final UserKeys userKeys;
    private final RandomGenerator random;
    private final Sleeper sleeper;
    private final Clock clock;

    private volatile Settings settings = new Settings(5, 30, List.of());
    private volatile Consumer<PlayedAction> onPlayed = played -> { };
    private Thread thread;

    /**
     * Guards {@link #heldKey} and the key events around it. The hold runs on the runner's thread,
     * the user's key up arrives on the watching one.
     */
    private final Object keyLock = new Object();
    private Key heldKey;

    public ActionRunner(GameInput input, BooleanSupplier gateOpen, Supplier<Player> player,
                        UserKeys userKeys) {
        this(input, gateOpen, player, userKeys, new Random(), Thread::sleep, Clock.systemDefaultZone());
    }

    ActionRunner(GameInput input, BooleanSupplier gateOpen, Supplier<Player> player,
                 UserKeys userKeys, RandomGenerator random, Sleeper sleeper, Clock clock) {
        this.input = input;
        this.gateOpen = gateOpen;
        this.player = player;
        this.userKeys = userKeys;
        this.random = random;
        this.sleeper = sleeper;
        this.clock = clock;
        userKeys.onRelease(this::pressAgainAfterUser);
    }

    public void configure(Settings settings) {
        this.settings = settings;
    }

    /** Registers the one handler that hears about every played action. It runs on the runner's thread. */
    public void onPlayed(Consumer<PlayedAction> handler) {
        this.onPlayed = handler;
    }

    public synchronized void start(BoundKeys keys) {
        if (thread == null) {
            userKeys.start();
            thread = Thread.startVirtualThread(() -> runUntilInterrupted(keys));
        }
    }

    /**
     * Returns once the thread has ended, so a key held at that moment is up again. That matters on
     * app exit, where the JVM would otherwise be gone before the release.
     */
    public synchronized void stop() {
        if (thread == null) {
            return;
        }
        thread.interrupt();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
        userKeys.stop(); // only now, the thread's last key up still asks whether the user holds the key
    }

    public synchronized boolean isRunning() {
        return thread != null;
    }

    private void runUntilInterrupted(BoundKeys keys) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                cycle(keys);
            } catch (InterruptedException stopped) {
                return; // stop() woke the wait or the hold. The hold's finally has released the key by now.
            } catch (RuntimeException failed) {
                // ponytail: a failed cycle only reaches stderr and the next one runs. Show it in the tab if it ever repeats.
                System.err.println("Randomizer cycle failed: " + failed);
            }
        }
    }

    /** One random wait, then one action if the game takes input right now. */
    void cycle(BoundKeys keys) throws InterruptedException {
        Settings wait = settings;
        sleeper.sleep(1000L * between(wait.minWaitSeconds(), wait.maxWaitSeconds()));
        if (mayPress()) {
            Optional<Candidate> pick = pick(candidates(settings.enabledActions(), player.get(), keys));
            if (pick.isPresent()) {
                play(pick.get().action(), pick(pick.get().routes()).orElseThrow());
            }
        }
    }

    /** The enabled actions that have a route right now. The draw is over actions, so three routes make none likelier. */
    private static List<Candidate> candidates(List<Action> enabled, Player player, BoundKeys keys) {
        return enabled.stream()
                .map(action -> new Candidate(action, pressableRoutes(action.routes().apply(player), keys)))
                .filter(candidate -> !candidate.routes().isEmpty())
                .toList();
    }

    /** The routes whose every step has a key that can be sent. One missing key rules the whole route out. */
    private static List<List<KeyPress>> pressableRoutes(List<List<Step>> routes, BoundKeys keys) {
        return routes.stream()
                .map(route -> route.stream().map(step -> keyPress(step, keys)).toList())
                .filter(route -> route.stream().allMatch(Optional::isPresent))
                .map(route -> route.stream().map(Optional::orElseThrow).toList())
                .toList();
    }

    /** The step on the key of its first command that has a pressable key, empty when none has one. */
    private static Optional<KeyPress> keyPress(Step step, BoundKeys keys) {
        return step.commands().stream()
                .flatMap(command -> keys.pressableKeyFor(command).stream())
                .findFirst()
                .map(keyName -> new KeyPress(keyName, Keys.key(keyName).orElseThrow(), step.press()));
    }

    private <T> Optional<T> pick(List<T> options) {
        return options.isEmpty()
                ? Optional.empty()
                : Optional.of(options.get(random.nextInt(options.size())));
    }

    /**
     * Asked before every key down of an action, not only before its first: a burst or a route takes
     * seconds, and the player can die, open the chat, switch windows or press stop in between.
     */
    private boolean mayPress() {
        return !Thread.currentThread().isInterrupted() && gateOpen.getAsBoolean() && input.isCs2Foreground();
    }

    /** One key press after the other, each with its own line in the log. */
    private void play(Action action, List<KeyPress> route) throws InterruptedException {
        for (KeyPress keyPress : route) {
            if (!mayPress()) {
                return;
            }
            Press press = keyPress.press();
            int millis = between(press.minMillis(), press.maxMillis());
            int spentMillis = press.clicks() ? click(keyPress.key(), millis, press) : hold(keyPress.key(), millis);
            onPlayed.accept(new PlayedAction(LocalTime.now(clock), action, keyPress.keyName(), spentMillis));
        }
    }

    /**
     * Holds {@code key} for {@code millis} and returns that time. The key comes up even when stop()
     * cuts the hold short, unless the user holds it at that moment. Then their own key up ends it.
     */
    private int hold(Key key, int millis) throws InterruptedException {
        synchronized (keyLock) {
            if (!pressAnew(key)) {
                throw new IllegalStateException("Windows blocked the key down of " + key + ", does CS2 run as admin?");
            }
            heldKey = key;
        }
        try {
            sleeper.sleep(millis);
        } finally {
            synchronized (keyLock) {
                heldKey = null;
                if (!userKeys.holds(key)) {
                    input.release(key);
                }
            }
        }
        return millis;
    }

    /** Runs on the watching thread each time the user lets go of a key. */
    private void pressAgainAfterUser(Key released) {
        synchronized (keyLock) {
            if (released.equals(heldKey)) {
                pressAnew(released);
            }
        }
    }

    /**
     * Key up, then key down. CS2 reads its input through SDL, which keeps the button state of every
     * input source apart and drops a key down from a source whose key it already has down. The
     * randomizer is one such source, the user's mouse another. After the user's key up ended the
     * action, a bare key down from here changes nothing in the randomizer's state and never reaches
     * the game. The key up first makes the key down count. It also makes up for a key up that an
     * earlier hold left out. Returns false when Windows did not take the key down.
     */
    private boolean pressAnew(Key key) {
        input.release(key);
        return input.press(key);
    }

    /**
     * Clicks the key at least once and then again for as long as the next click still starts within
     * {@code window} milliseconds. Before each further click comes a pause from the range in {@code press}.
     * Returns the time from the first key down to the last key up.
     */
    private int click(Key key, int window, Press press) throws InterruptedException {
        int elapsed = 0;
        while (true) {
            elapsed += hold(key, between(CLICK_DOWN_MIN_MILLIS, CLICK_DOWN_MAX_MILLIS));
            int pause = between(press.minPauseMillis(), press.maxPauseMillis());
            if (elapsed + pause >= window) {
                return elapsed;
            }
            sleeper.sleep(pause);
            if (!mayPress()) {
                return elapsed;
            }
            elapsed += pause;
        }
    }

    /** Uniform within both bounds. A max below min counts as min, so equal or crossed bounds never throw. */
    private int between(int min, int max) {
        return min + random.nextInt(Math.max(max, min) - min + 1);
    }
}
