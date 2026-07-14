package de.bsommerfeld.randomizer.gsi;

import com.cs2gsi.GameState;
import com.cs2gsi.GameStateListener;
import com.cs2gsi.events.CS2GameEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    /** A single CS2 event prepared for display: one-line summary plus multi-line details. */
    public record GsiEvent(String summary, String details) {
    }

    /** Registers a handler that receives every CS2 event as a {@link GsiEvent}. */
    public void onGameEvent(Consumer<GsiEvent> handler) {
        listener.onGameEvent(event -> handler.accept(toGsiEvent(event)));
    }

    private static GsiEvent toGsiEvent(CS2GameEvent event) {
        String time = LocalTime.now().format(TIME);
        String name = event.getClass().getSimpleName();
        String summary = time + "  " + abbreviate(name + summarizeFields(event));
        String details = "Zeit:  " + time + System.lineSeparator()
                + "Event: " + name + System.lineSeparator()
                + System.lineSeparator()
                + formatFields(event);
        return new GsiEvent(summary, details);
    }

    private static String abbreviate(String value) {
        return value.length() <= 160 ? value : value.substring(0, 157) + "…";
    }

    /** Compact "key=value" pairs of all scalar event fields for the summary line. */
    private static String summarizeFields(CS2GameEvent event) {
        StringBuilder sb = new StringBuilder();
        for (Field field : publicInstanceFields(event)) {
            Object value = readField(field, event);
            String text = String.valueOf(value);
            // Node values render as JSON objects — too long for the summary line
            if (!text.startsWith("{") && !text.startsWith("[")) {
                sb.append(' ').append(field.getName()).append('=').append(text);
            }
        }
        return sb.toString();
    }

    /** Lists every public event field; Node values (raw JSON) are pretty-printed. */
    private static String formatFields(CS2GameEvent event) {
        List<Field> fields = publicInstanceFields(event);
        if (fields.isEmpty()) {
            return "(keine weiteren Daten)";
        }
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            Object value = readField(field, event);
            sb.append(field.getName()).append(':').append(System.lineSeparator())
                    .append(formatValue(value).indent(2))
                    .append(System.lineSeparator());
        }
        return sb.toString().stripTrailing();
    }

    private static List<Field> publicInstanceFields(CS2GameEvent event) {
        List<Field> fields = new ArrayList<>();
        for (Field field : event.getClass().getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static Object readField(Field field, CS2GameEvent event) {
        try {
            return field.get(event);
        } catch (ReflectiveOperationException e) {
            return "<nicht lesbar>";
        }
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value);
        if (text.startsWith("{")) {
            // Raw JSON (e.g. from Node.parsedData) — pretty-print it when possible
            try {
                return GSON.toJson(JsonParser.parseString(text));
            } catch (RuntimeException ignored) {
            }
        }
        if (text.contains("[")) {
            // Node values render as "[SteamID: ..., State: [...]]" — indent the nesting
            return indentBrackets(text);
        }
        return text;
    }

    private static String indentBrackets(String raw) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '[' -> {
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == ']') {
                        sb.append("[]");
                        i++;
                    } else {
                        indent++;
                        sb.append('[').append(System.lineSeparator()).append("  ".repeat(indent));
                    }
                }
                case ']' -> {
                    indent = Math.max(0, indent - 1);
                    sb.append(System.lineSeparator()).append("  ".repeat(indent)).append(']');
                }
                case ',' -> {
                    sb.append(',').append(System.lineSeparator()).append("  ".repeat(indent));
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == ' ') {
                        i++;
                    }
                }
                default -> sb.append(c);
            }
        }
        return sb.toString();
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
