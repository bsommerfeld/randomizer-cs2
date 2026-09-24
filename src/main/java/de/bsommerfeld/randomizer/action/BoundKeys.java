package de.bsommerfeld.randomizer.action;

import de.bsommerfeld.randomizer.input.Keys;
import de.bsommerfeld.randomizer.vdf.VdfObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The keybind files read backwards: which keys CS2 has on a command. The files map key to command,
 * the randomizer knows the command and needs the key.
 *
 * <p>Only a bind of exactly the command counts. A key bound to several commands at once
 * ({@code "+jump; +duck"}) is no key for either of them.
 */
public final class BoundKeys {

    private final Map<String, List<String>> keysByCommand;

    private BoundKeys(Map<String, List<String>> keysByCommand) {
        this.keysByCommand = keysByCommand;
    }

    /**
     * Reads the binds the way CS2 does, config after config. A later bind of the same key replaces
     * the earlier one. That is also how {@code "<unbound>"} in the custom file takes a default away.
     */
    public static BoundKeys of(List<VdfObject> configsInLoadOrder) {
        Map<String, String> commandByKey = new LinkedHashMap<>();
        for (VdfObject config : configsInLoadOrder) {
            bindingsOf(config).ifPresent(bindings -> bindings.entries().forEach((key, command) -> {
                if (command instanceof String text) {
                    commandByKey.put(key.toUpperCase(Locale.ROOT), text.toLowerCase(Locale.ROOT));
                }
            }));
        }
        Map<String, List<String>> keysByCommand = new LinkedHashMap<>();
        commandByKey.forEach((key, command) ->
                keysByCommand.computeIfAbsent(command, unused -> new ArrayList<>()).add(key));
        return new BoundKeys(keysByCommand);
    }

    /** CS2 writes cs2_user_keys.vcfg with or without the outer "config" node. */
    private static Optional<VdfObject> bindingsOf(VdfObject config) {
        return config.getObject("config").orElse(config).getObject("bindings");
    }

    /** Every key on {@code command}, upper-cased, in the order the files list them. */
    public List<String> keysFor(String command) {
        return keysByCommand.getOrDefault(command.toLowerCase(Locale.ROOT), List.of());
    }

    /** The key the randomizer presses for {@code command}, the first one {@link Keys} can send. */
    public Optional<String> pressableKeyFor(String command) {
        return keysFor(command).stream().filter(key -> Keys.key(key).isPresent()).findFirst();
    }
}
