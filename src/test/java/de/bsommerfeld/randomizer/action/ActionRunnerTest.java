package de.bsommerfeld.randomizer.action;

import com.cs2gsi.nodes.Player;
import com.cs2gsi.nodes.Weapon;
import com.google.gson.JsonParser;
import de.bsommerfeld.randomizer.input.GameInput;
import de.bsommerfeld.randomizer.input.Keys;
import de.bsommerfeld.randomizer.input.Keys.Key;
import de.bsommerfeld.randomizer.input.UserKeys;
import de.bsommerfeld.randomizer.vdf.VdfParser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionRunnerTest {

    private static final Action JUMP = Action.tap("Springen", "+jump");
    private static final Action DROP = Action.tap("Drop weapon", "drop");
    private static final Action SHOOT = new Action("Shoot", "+attack", 200, 1500);
    private static final Key SPACE = Keys.key("SPACE").orElseThrow();

    /** +jump sits on SPACE, +attack only on the wheel and drop on nothing. */
    private static final BoundKeys KEYS = BoundKeys.of(List.of(
            VdfParser.parse("\"config\" { \"bindings\" { \"SPACE\" \"+jump\" \"MWHEELUP\" \"+attack\" } }")));

    /** +jump on SPACE and +reload on R, for a route of two keys. */
    private static final BoundKeys JUMP_AND_RELOAD_KEYS = BoundKeys.of(List.of(
            VdfParser.parse("\"config\" { \"bindings\" { \"SPACE\" \"+jump\" \"r\" \"+reload\" } }")));
    private static final Action JUMP_THEN_RELOAD = new Action("Nachladen", "+reload", anyPlayer -> List.of(
            List.of(new Step("+jump", Press.tap()), new Step("+reload", Press.tap()))));

    private static final Clock NOON =Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC);

    private final RecordingInput input = new RecordingInput();
    private final List<Long> sleeps = new ArrayList<>();
    private final List<PlayedAction> played = new ArrayList<>();
    private final FakeUserKeys userKeys = new FakeUserKeys();
    private boolean gateOpen = true;
    private Player player = new Player(); // what the GSI library reports before the first game state

    @Test
    void waitsThenPressesHoldsAndReleasesTheBoundKey() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 30, List.of(JUMP)));

        runner.cycle(KEYS);

        assertEquals(List.of("release " + SPACE, "press " + SPACE, "release " + SPACE), input.events,
                "every key down has a key up before it");
        assertEquals(2, sleeps.size());
        assertTrue(sleeps.get(0) >= 5_000 && sleeps.get(0) <= 30_000, "wait was " + sleeps.get(0));
        assertTrue(sleeps.get(1) >= 30 && sleeps.get(1) <= 60, "hold was " + sleeps.get(1));
        assertEquals(List.of(new PlayedAction(LocalTime.NOON, JUMP, "SPACE", sleeps.get(1).intValue())), played);
    }

    @Test
    void theHeldKeyGoesDownAgainTheMomentTheUserLetsGoOfIt() throws InterruptedException {
        Action forward = new Action("Forward", "+jump", 1000, 1000);
        ActionRunner runner = runner(millis -> {
            sleeps.add(millis);
            if (sleeps.size() == 2) { // the first sleep is the wait, the second the hold
                userKeys.letGoOf(Keys.key("w").orElseThrow());
                userKeys.letGoOf(SPACE);
            }
        }, new ActionRunner.Settings(5, 5, List.of(forward)));

        runner.cycle(KEYS);
        userKeys.letGoOf(SPACE);

        assertEquals(List.of("release " + SPACE, "press " + SPACE, // the action begins
                        "release " + SPACE, "press " + SPACE,      // the user let go of SPACE
                        "release " + SPACE),                       // the action ends
                input.events, "w is not the held key, and the last SPACE came after the action");
        assertEquals(List.of(5_000L, 1_000L), sleeps, "held as long as drawn, in one piece");
    }

    @Test
    void theKeyStaysDownWhenTheUserHoldsItThemselvesAtTheEnd() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(JUMP)));
        userKeys.held.add(SPACE);

        runner.cycle(KEYS);

        assertEquals(List.of("release " + SPACE, "press " + SPACE), input.events,
                "a key up at the end would end the user's own hold in the game");
        assertEquals(1, played.size(), "the action still counts as played");
    }

    @Test
    void aSpammedKeyIsClickedForAsLongAsTheWindowLasts() throws InterruptedException {
        Action fan = Action.byWeapon("Zweitfeuer", "+jump", anyWeapon -> Press.spammed(1000, 1000));
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(fan)));

        runner.cycle(KEYS);

        PlayedAction entry = played.getFirst();
        assertTrue(input.clicks() >= 5 && input.clicks() <= 12, "clicks in one second: " + input.clicks());
        assertTrue(entry.heldMillis() > 1000 - 140 - 60 && entry.heldMillis() < 1000 + 60,
                "first down to last up: " + entry.heldMillis());
        for (int i = 0; i < input.events.size(); i++) { // each click is up, down, up
            assertEquals((i % 3 == 1 ? "press " : "release ") + SPACE, input.events.get(i), "event " + i);
        }
        long spent = sleeps.stream().skip(1).mapToLong(Long::longValue).sum(); // the first sleep is the wait
        assertEquals(entry.heldMillis(), spent, "the log shows the time that passed, not the window drawn");
    }

    @Test
    void aPacedKeyWaitsOutTheBoltBetweenTwoClicks() throws InterruptedException {
        Action awp = Action.byWeapon("Shoot", "+jump", anyWeapon -> Press.paced(4000, 4000, 1500, 1800));
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(awp)));

        runner.cycle(KEYS);

        assertTrue(input.clicks() == 2 || input.clicks() == 3, "clicks in four seconds: " + input.clicks());
        List<Long> pauses = sleeps.stream().skip(1).filter(millis -> millis > 60).toList();
        assertEquals(input.clicks() - 1, pauses.size());
        assertTrue(pauses.stream().allMatch(pause -> pause >= 1500 && pause <= 1800), pauses.toString());
    }

    @Test
    void theActionChoosesItsPressByTheWeaponInHand() throws InterruptedException {
        Action shoot = Action.byWeapon("Shoot", "+jump",
                weapon -> "weapon_glock".equals(weapon.name) ? Press.spammed(500, 500) : Press.held(500, 500));
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(shoot)));

        runner.cycle(KEYS);
        int clicksWithoutWeapon = input.clicks();
        player = playerCarrying("{\"name\":\"weapon_glock\",\"type\":\"Pistol\",\"state\":\"active\"}");
        runner.cycle(KEYS);

        assertEquals(1, clicksWithoutWeapon, "no weapon known: held once");
        assertTrue(input.clicks() - clicksWithoutWeapon > 1, "glock: spammed, was " + input.events);
    }

    @Test
    void aRouteOfSeveralKeysIsPressedInOrderWithOneLogLinePerKey() throws InterruptedException {
        BoundKeys keys = BoundKeys.of(List.of(
                VdfParser.parse("\"config\" { \"bindings\" { \"SPACE\" \"+jump\" \"r\" \"+reload\" } }")));
        Key r = Keys.key("r").orElseThrow();
        Action jumpThenReload = new Action("Nachladen", "+reload", anyPlayer -> List.of(
                List.of(new Step("+jump", Press.tap()), new Step("+reload", Press.held(1500, 1500)))));
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(jumpThenReload)));

        runner.cycle(keys);

        assertEquals(List.of("release " + SPACE, "press " + SPACE, "release " + SPACE,
                "release " + r, "press " + r, "release " + r), input.events);
        assertEquals(List.of("SPACE", "R"), played.stream().map(PlayedAction::key).toList());
        assertEquals(1500, played.getLast().heldMillis());
    }

    @Test
    void aStepGoesByItsFirstCommandThatHasAKey() throws InterruptedException {
        Action flash = new Action("Flash", "slot7", anyPlayer -> List.of(
                List.of(new Step(List.of("slot7", "slot4"), Press.tap()))));
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(flash)));

        runner.cycle(BoundKeys.of(List.of(VdfParser.parse("\"config\" { \"bindings\" { \"4\" \"slot4\" } }"))));
        runner.cycle(BoundKeys.of(List.of(
                VdfParser.parse("\"config\" { \"bindings\" { \"4\" \"slot4\" \"7\" \"slot7\" } }"))));
        runner.cycle(KEYS);

        assertEquals(List.of("4", "7"), played.stream().map(PlayedAction::key).toList(), "neither bound: not played");
    }

    @Test
    void anActionWithoutARouteOrWithAnUnboundKeyOnEveryRouteIsNotInTheDraw() throws InterruptedException {
        Action pointless = new Action("Nachladen", "+jump", anyPlayer -> List.of());
        Action needsDrop = new Action("Nachladen", "+jump", anyPlayer -> List.of(
                List.of(new Step("drop", Press.tap()), new Step("+jump", Press.tap()))));
        ActionRunner runner = runner(sleeps::add,
                new ActionRunner.Settings(5, 5, List.of(pointless, needsDrop, JUMP)));

        for (int i = 0; i < 20; i++) {
            runner.cycle(KEYS);
        }

        assertEquals(20, played.size());
        assertTrue(played.stream().allMatch(entry -> entry.action().equals(JUMP)), played.toString());
    }

    @Test
    void aDueActionIsDroppedWhileTheGateIsClosedOrCs2IsNotInFront() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 30, List.of(JUMP)));

        gateOpen = false;
        runner.cycle(KEYS);
        gateOpen = true;
        input.cs2Foreground = false;
        runner.cycle(KEYS);

        assertEquals(List.of(), input.events);
        assertEquals(List.of(), played);
        assertEquals(2, sleeps.size(), "both cycles still waited");
    }

    @Test
    void neverPicksWhatIsDisabledUnboundOrOnAnUnsupportedKey() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 30, List.of(JUMP, DROP, SHOOT)));

        for (int i = 0; i < 50; i++) {
            runner.cycle(KEYS);
        }

        assertEquals(50, played.size());
        assertTrue(played.stream().allMatch(entry -> entry.action().equals(JUMP)), played.toString());

        runner.configure(new ActionRunner.Settings(5, 30, List.of(DROP, SHOOT)));
        runner.cycle(KEYS);
        assertEquals(50, played.size(), "with jump switched off nothing is left to play");
    }

    @Test
    void aStopDuringTheHoldStillReleasesTheKey() {
        ActionRunner runner = runner(millis -> {
            sleeps.add(millis);
            if (sleeps.size() == 2) { // the first sleep is the wait, the second the hold
                throw new InterruptedException();
            }
        }, new ActionRunner.Settings(5, 30, List.of(JUMP)));

        assertThrows(InterruptedException.class, () -> runner.cycle(KEYS));

        assertEquals(List.of("release " + SPACE, "press " + SPACE, "release " + SPACE), input.events);
        assertEquals(List.of(), played, "a cut-off action is not logged");
    }

    @Test
    void theRestOfARouteIsLeftOutOnceTheGameNoLongerTakesInput() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(JUMP_THEN_RELOAD)));
        runner.onPlayed(entry -> {
            played.add(entry);
            gateOpen = false; // the player died during the jump
        });

        runner.cycle(JUMP_AND_RELOAD_KEYS);

        assertEquals(List.of("SPACE"), played.stream().map(PlayedAction::key).toList());
        assertEquals(List.of("release " + SPACE, "press " + SPACE, "release " + SPACE), input.events);
    }

    @Test
    void aStopBetweenTwoKeysOfARouteSendsNoFurtherKey() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(JUMP_THEN_RELOAD)));
        runner.onPlayed(entry -> Thread.currentThread().interrupt()); // stop() between the jump and the reload

        runner.cycle(JUMP_AND_RELOAD_KEYS);
        boolean stillInterrupted = Thread.interrupted(); // clears the flag for the tests after this one

        assertTrue(stillInterrupted, "the interrupt stays for the loop to end on");
        assertEquals(List.of("release " + SPACE, "press " + SPACE, "release " + SPACE), input.events);
    }

    @Test
    void aBurstEndsWithTheClickBeforeTheGateCloses() throws InterruptedException {
        Action burst = new Action("Shoot", "+jump",
                anyPlayer -> List.of(List.of(new Step("+jump", Press.spammed(5000, 5000)))));
        ActionRunner runner = runner(millis -> {
            sleeps.add(millis);
            if (sleeps.size() == 3) { // the wait, the first click, the pause after it
                gateOpen = false;
            }
        }, new ActionRunner.Settings(5, 5, List.of(burst)));

        runner.cycle(KEYS);

        assertEquals(1, input.clicks());
        assertEquals(sleeps.get(1).intValue(), played.getFirst().heldMillis(), "the pause after the last click does not count");
    }

    @Test
    void aKeyDownThatWindowsBlockedIsNoPlayedAction() {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(5, 5, List.of(JUMP)));
        input.blocked = true;

        assertThrows(IllegalStateException.class, () -> runner.cycle(KEYS));

        assertEquals(List.of(), played);
        assertEquals(List.of("release " + SPACE, "press " + SPACE), input.events, "nothing went down, nothing has to come up");
    }

    @Test
    void aFailedCycleDoesNotEndTheRandomizer() throws InterruptedException {
        AtomicInteger gateCalls = new AtomicInteger();
        CountDownLatch nextCycle = new CountDownLatch(1);
        ActionRunner runner = new ActionRunner(input, () -> {
            if (gateCalls.incrementAndGet() == 1) {
                throw new IllegalStateException("a game state nobody expected");
            }
            nextCycle.countDown();
            return false;
        }, () -> player, userKeys);
        runner.configure(new ActionRunner.Settings(0, 0, List.of(JUMP)));

        runner.start(KEYS);
        try {
            assertTrue(nextCycle.await(5, TimeUnit.SECONDS), "the loop went on after the failure");
        } finally {
            runner.stop();
        }
    }

    @Test
    void equalBoundsAreNoError() throws InterruptedException {
        ActionRunner runner = runner(sleeps::add, new ActionRunner.Settings(7, 7, List.of()));

        runner.cycle(KEYS);

        assertEquals(List.of(7_000L), sleeps);
    }

    @Test
    void stopsAndStartsAgain() {
        ActionRunner runner = new ActionRunner(input, () -> gateOpen, () -> player, userKeys);
        runner.configure(new ActionRunner.Settings(3600, 3600, List.of(JUMP)));

        runner.start(KEYS);
        assertTrue(runner.isRunning());
        assertTrue(userKeys.watching, "the user's keys are watched while the randomizer runs");
        runner.stop();
        assertFalse(runner.isRunning());
        assertFalse(userKeys.watching, "and not a moment longer");

        runner.start(KEYS);
        assertTrue(runner.isRunning(), "the old randomizer would not start a second time");
        runner.stop();
        assertEquals(List.of(), input.events);
    }

    private ActionRunner runner(ActionRunner.Sleeper sleeper, ActionRunner.Settings settings) {
        ActionRunner runner = new ActionRunner(input, () -> gateOpen, () -> player, userKeys,
                new Random(42), sleeper, NOON);
        runner.configure(settings);
        runner.onPlayed(played::add);
        return runner;
    }

    static Weapon weapon(String name, String type) {
        return new Weapon(JsonParser.parseString(
                "{\"name\":\"%s\",\"type\":\"%s\",\"state\":\"active\"}".formatted(name, type)).getAsJsonObject());
    }

    /** A player who carries the given weapons, each one the JSON of a GSI weapon node. */
    static Player playerCarrying(String... weaponNodes) {
        StringBuilder weapons = new StringBuilder();
        for (int i = 0; i < weaponNodes.length; i++) {
            weapons.append(i == 0 ? "" : ",").append("\"weapon_").append(i).append("\":").append(weaponNodes[i]);
        }
        return new Player(JsonParser.parseString("{\"weapons\":{" + weapons + "}}").getAsJsonObject());
    }

    private static final class FakeUserKeys implements UserKeys {
        private final Set<Key> held = new HashSet<>();
        private Consumer<Key> onRelease = key -> { };
        private boolean watching;

        private void letGoOf(Key key) {
            held.remove(key);
            onRelease.accept(key);
        }

        @Override
        public void start() {
            watching = true;
        }

        @Override
        public void stop() {
            watching = false;
        }

        @Override
        public boolean holds(Key key) {
            return held.contains(key);
        }

        @Override
        public void onRelease(Consumer<Key> listener) {
            onRelease = listener;
        }
    }

    private static final class RecordingInput implements GameInput {
        private final List<String> events = new ArrayList<>();
        private boolean cs2Foreground = true;
        private boolean blocked;

        /** One key down per click. Key ups are no measure, every key down has one before it as well. */
        private int clicks() {
            return (int) events.stream().filter(event -> event.startsWith("press ")).count();
        }

        @Override
        public boolean press(Key key) {
            events.add("press " + key);
            return !blocked;
        }

        @Override
        public void release(Key key) {
            events.add("release " + key);
        }

        @Override
        public boolean isCs2Foreground() {
            return cs2Foreground;
        }
    }
}
