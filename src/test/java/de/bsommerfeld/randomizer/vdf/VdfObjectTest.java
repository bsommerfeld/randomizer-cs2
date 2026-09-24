package de.bsommerfeld.randomizer.vdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VdfObjectTest {

    private static final VdfObject DEFAULTS = VdfParser.parse("""
            "config"
            {
                "bindings"
                {
                    "w" "+forward"
                    "c" "+radialradio"
                    "`" "toggleconsole"
                }
                "analogbindings"
                {
                    "MOUSE_X" "yaw"
                }
            }
            """);

    private static final VdfObject CUSTOM = VdfParser.parse("""
            "config"
            {
                "bindings"
                {
                    "c" "player_ping"
                    "`" "<unbound>"
                    "F9" "echo hi"
                }
            }
            """);

    @Test
    void overrideWinsForTheSameKeyAndEverythingElseSurvives() {
        VdfObject config = DEFAULTS.mergedWith(CUSTOM).getObject("config").orElseThrow();
        VdfObject bindings = config.getObject("bindings").orElseThrow();

        assertEquals("+forward", bindings.getString("w").orElseThrow(), "default-only key is kept");
        assertEquals("player_ping", bindings.getString("c").orElseThrow(), "custom replaces the default");
        assertEquals("<unbound>", bindings.getString("`").orElseThrow(), "an unbind is an override too");
        assertEquals("echo hi", bindings.getString("F9").orElseThrow(), "custom-only key is added");
        assertEquals("yaw", config.getObject("analogbindings").orElseThrow().getString("MOUSE_X").orElseThrow(),
                "a block only the defaults have is kept");
    }

    @Test
    void keepsTheDefaultOrderAndAppendsNewKeys() {
        VdfObject bindings = DEFAULTS.mergedWith(CUSTOM)
                .getObject("config").orElseThrow().getObject("bindings").orElseThrow();

        assertEquals(List.of("w", "c", "`", "F9"), List.copyOf(bindings.entries().keySet()));
    }

    @Test
    void leavesBothInputsUntouched() {
        DEFAULTS.mergedWith(CUSTOM);

        VdfObject defaultBindings = DEFAULTS.getObject("config").orElseThrow().getObject("bindings").orElseThrow();
        VdfObject customBindings = CUSTOM.getObject("config").orElseThrow().getObject("bindings").orElseThrow();
        assertEquals("+forward", defaultBindings.getString("w").orElseThrow());
        assertEquals("+radialradio", defaultBindings.getString("c").orElseThrow());
        assertEquals(3, customBindings.entries().size());
    }
}
