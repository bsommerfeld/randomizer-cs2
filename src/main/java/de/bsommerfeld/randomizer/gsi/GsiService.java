package de.bsommerfeld.randomizer.gsi;

import com.cs2gsi.GSIConfigResult;
import com.cs2gsi.GameState;
import com.cs2gsi.GameStateListener;
import com.cs2gsi.events.CS2GameEvent;
import com.cs2gsi.events.provider.ProviderTimestampChanged;
import com.cs2gsi.events.provider.ProviderUpdated;
import de.bsommerfeld.randomizer.gsi.event.EventFormatter;
import de.bsommerfeld.randomizer.gsi.json.Json;

import java.time.Clock;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The app's side of the CS2 GSI library (<a href="https://github.com/bustolio/CS2-GSI">bustolio/CS2-GSI</a>).
 * Starts and stops its local HTTP listener and hands the incoming game states and events on.
 *
 * <p>Every handler runs on the library's listener thread, not on the JavaFX thread.
 */
public final class GsiService implements AutoCloseable {

    public static final int DEFAULT_PORT = 4000;

    /** Results in the file gamestate_integration_randomizer.cfg in the CS2 cfg folder. */
    private static final String GSI_CONFIG_NAME = "randomizer";

    private final GameStateListener listener;
    private final EventFormatter eventFormatter;
    private final InstantSource clock;

    public GsiService() {
        this(DEFAULT_PORT);
    }

    public GsiService(int port) {
        this(port, Clock.systemUTC());
    }

    /** The listener stamps every game state with {@code clock} and {@link #silence()} reads it, so both tell the same time. */
    GsiService(int port, InstantSource clock) {
        this.listener = new GameStateListener(port, clock);
        this.eventFormatter = new EventFormatter();
        this.clock = clock;
    }

    /** Starts the listener; false if e.g. the port is already in use. */
    public boolean start() {
        return listener.start();
    }

    /** Stops the listener; calling {@link #start()} again afterwards is supported. */
    public void stop() {
        listener.stop();
    }

    public boolean isRunning() {
        return listener.isRunning();
    }

    public int getPort() {
        return listener.getPort();
    }

    /**
     * Writes the GSI config into the CS2 cfg folder (the library locates CS2 on its own) and reports
     * what that did to the file. CS2 reads it on startup only, so CREATED and UPDATED mean the game
     * needs one restart. A file that already has the right content is left alone.
     */
    public GSIConfigResult installConfigFile() {
        return listener.installGSIConfigFile(GSI_CONFIG_NAME);
    }

    /** Registers a handler that receives the pretty JSON of every game-state update. */
    public void onGameStateJson(Consumer<String> handler) {
        listener.onNewGameState(state -> handler.accept(Json.prettify(state.toString())));
    }

    /** Registers a handler that receives every game-state update as the parsed {@link GameState}. */
    public void onGameState(Consumer<GameState> handler) {
        listener.onNewGameState(handler::accept);
    }

    /** The latest state CS2 sent, an empty one before the first. Stopping the listener does not clear it. */
    public GameState currentGameState() {
        return listener.getCurrentGameState();
    }

    /**
     * How long ago CS2 sent its last game state, empty before the first one. A heartbeat that repeats
     * the state counts, so with the game up this stays below the heartbeat of the config file, 10 s in
     * the generated one.
     */
    public Optional<Duration> silence() {
        return listener.getLastGameStateTime().map(last -> Duration.between(last, clock.instant()));
    }

    /** Registers a handler that receives every CS2 event as a {@link GsiEvent}, except the provider heartbeat. */
    public void onGameEvent(Consumer<GsiEvent> handler) {
        listener.onGameEvent(event -> {
            if (!isProviderHeartbeat(event)) {
                handler.accept(eventFormatter.format(event));
            }
        });
    }

    /** CS2 stamps every payload with a new provider timestamp, so these two fire on each update and carry no news. */
    private static boolean isProviderHeartbeat(CS2GameEvent event) {
        return event instanceof ProviderUpdated || event instanceof ProviderTimestampChanged;
    }

    @Override
    public void close() {
        listener.close();
    }
}
