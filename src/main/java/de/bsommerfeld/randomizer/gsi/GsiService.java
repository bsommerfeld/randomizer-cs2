package de.bsommerfeld.randomizer.gsi;

import com.cs2gsi.GameState;
import com.cs2gsi.GameStateListener;
import com.cs2gsi.events.CS2GameEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Wraps the CS2 GSI library (<a href="https://github.com/bustolio/CS2-GSI">bustolio/CS2-GSI</a>):
 * starts the local HTTP listener that CS2 sends its game-state updates to and forwards
 * every new GameState as pretty JSON.
 *
 * <p>Note: callbacks run on the library's listener thread, not on the
 * JavaFX Application Thread.
 */
public final class GsiService implements AutoCloseable {

    public static final int DEFAULT_PORT = 4000;

    /** Results in the file gamestate_integration_randomizer.cfg in the CS2 cfg folder. */
    private static final String GSI_CONFIG_NAME = "randomizer";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GameStateListener listener;

    public GsiService() {
        this(DEFAULT_PORT);
    }

    public GsiService(int port) {
        this.listener = new GameStateListener(port);
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
     * Writes gamestate_integration_randomizer.cfg into the CS2 cfg folder (the library
     * locates CS2 on its own); false if that fails. CS2 has to be restarted once
     * afterwards so it picks up the file.
     */
    public boolean generateConfigFile() {
        return listener.generateGSIConfigFile(GSI_CONFIG_NAME);
    }

    /** Registers a handler that receives the pretty JSON of every game-state update. */
    public void onGameStateJson(Consumer<String> handler) {
        listener.onNewGameState(state -> handler.accept(toPrettyJson(state)));
    }

    /** Registers a handler that receives every CS2 event as a formatted log line. */
    public void onGameEventText(Consumer<String> handler) {
        listener.onGameEvent(event -> handler.accept(formatEvent(event)));
    }

    private static String formatEvent(CS2GameEvent event) {
        String details = event.toString();
        // Records yield "PlayerGotKill[...]"; otherwise show just the class name
        String description = details.contains("[") ? details : event.getClass().getSimpleName();
        return LocalTime.now().format(TIME) + "  " + description;
    }

    private static String toPrettyJson(GameState state) {
        String raw = state.toString();
        try {
            return GSON.toJson(JsonParser.parseString(raw));
        } catch (RuntimeException e) {
            return raw;
        }
    }

    @Override
    public void close() {
        listener.close();
    }
}
