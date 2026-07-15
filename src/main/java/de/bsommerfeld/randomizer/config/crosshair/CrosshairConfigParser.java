package de.bsommerfeld.randomizer.config.crosshair;

import de.bsommerfeld.randomizer.config.ConfigParser;
import de.bsommerfeld.randomizer.vdf.VdfObject;
import de.bsommerfeld.randomizer.vdf.VdfParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses {@code cs2_user_convars.vcfg}: pulls every {@code cl_crosshair*} convar out of the VDF
 * and turns it into typed {@link CrosshairSettings}.
 */
public final class CrosshairConfigParser implements ConfigParser<Crosshair> {

    @Override
    public Crosshair parse(Path file) throws IOException {
        VdfObject root = VdfParser.parse(file);
        Map<String, String> convars = new LinkedHashMap<>();
        collectCrosshairConvars(root, convars);
        return new Crosshair(file, convars, CrosshairSettings.fromConvars(convars));
    }

    /**
     * Recursively collects every crosshair-related string entry, regardless of nesting. Matches on
     * {@code "crosshair"} anywhere in the key so differently-prefixed convars are caught too, e.g.
     * {@code cl_fixedcrosshairgap}.
     */
    static void collectCrosshairConvars(VdfObject node, Map<String, String> out) {
        node.entries().forEach((key, value) -> {
            if (value instanceof VdfObject child) {
                collectCrosshairConvars(child, out);
            } else if (value instanceof String text && key.contains("crosshair")) {
                out.put(key, text);
            }
        });
    }
}
