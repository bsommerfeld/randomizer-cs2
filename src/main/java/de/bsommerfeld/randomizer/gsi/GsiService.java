package de.bsommerfeld.randomizer.gsi;

import com.cs2gsi.GameState;
import com.cs2gsi.GameStateListener;
import de.bsommerfeld.randomizer.gsi.event.EventFormatter;
import de.bsommerfeld.randomizer.gsi.json.Json;
import de.bsommerfeld.randomizer.gsi.state.GameStateChangeTracker;

import java.util.function.Consumer;

/**
 * Facade over the CS2 GSI library (<a href="https://github.com/bustolio/CS2-GSI">bustolio/CS2-GSI</a>):
 * owns the local HTTP listener lifecycle and exposes the incoming game states and events to the
 * application. Serializing, diffing and event formatting live in dedicated collaborators
 * ({@link Json}, {@link GameStateChangeTracker}, {@link EventFormatter}); this class only wires
 * them to the listener.
 *
 * <p>Note: callbacks run on the library's listener thread, not on the JavaFX Application Thread.
 */
public final class GsiService implements AutoCloseable {

    public static final int DEFAULT_PORT = 4000;

    /** Results in the file gamestate_integration_randomizer.cfg in the CS2 cfg folder. */
    private static final String GSI_CONFIG_NAME = "randomizer";

    private final GameStateListener listener;
    private final EventFormatter eventFormatter;

    public GsiService() {
        this(DEFAULT_PORT);
    }

    public GsiService(int port) {
        this(new GameStateListener(port), new EventFormatter());
    }

    /** Full control over the collaborators - mainly for tests and custom event rendering. */
    public GsiService(GameStateListener listener, EventFormatter eventFormatter) {
        this.listener = listener;
        this.eventFormatter = eventFormatter;
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
     * Writes gamestate_integration_randomizer.cfg into the CS2 cfg folder (the library locates CS2
     * on its own); false if that fails. CS2 has to be restarted once afterwards so it picks up the
     * file.
     */
    public boolean generateConfigFile() {
        return listener.generateGSIConfigFile(GSI_CONFIG_NAME);
    }

    /** Registers a handler that receives the pretty JSON of every game-state update. */
    public void onGameStateJson(Consumer<String> handler) {
        listener.onNewGameState(state -> handler.accept(Json.prettify(state.toString())));
    }

    /**
     * Registers a handler that receives, as pretty JSON, only the values that changed since the
     * previous game state. The handler is not called for the first state (nothing to compare
     * against) nor when nothing changed; see {@link GameStateChangeTracker} for the exact semantics.
     */
    public void onGameStateChanges(Consumer<String> handler) {
        GameStateChangeTracker tracker = new GameStateChangeTracker();
        listener.onNewGameState(state -> {
            String changes = tracker.track(state.toString());
            if (changes != null) {
                handler.accept(changes);
            }
        });
    }

    /**
     * Registers a handler that receives the structured {@link GameState} of every update - the
     * parsed node tree ({@code player}, {@code map}, {@code round}, {@code allPlayers}, …) rather
     * than a JSON string. Intended for views that read fields directly, e.g. the live overview.
     */
    public void onGameState(Consumer<GameState> handler) {
        listener.onNewGameState(handler::accept);
    }

    /** Registers a handler that receives every CS2 event as a {@link GsiEvent}. */
    public void onGameEvent(Consumer<GsiEvent> handler) {
        listener.onGameEvent(event -> handler.accept(eventFormatter.format(event)));
    }

    @Override
    public void close() {
        listener.close();
    }
}
