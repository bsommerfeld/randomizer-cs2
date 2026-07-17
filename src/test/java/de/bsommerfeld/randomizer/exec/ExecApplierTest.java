package de.bsommerfeld.randomizer.exec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecApplierTest {

    @TempDir
    Path tempDir;

    private ExecApplier applier(Cs2Window window, String key) {
        return new ExecApplier(new ExecConfig(() -> Optional.of(tempDir)), window, () -> key);
    }

    @Test
    void triggersWhenWindowAcceptsTheKey() throws IOException {
        AtomicReference<ExecKeys.Key> pressed = new AtomicReference<>();
        ExecApplier applier = applier(key -> {
            pressed.set(key);
            return Cs2Window.PressResult.PRESSED;
        }, "f24");

        assertEquals(ExecApplier.Result.TRIGGERED, applier.apply(Map.of("cl_crosshairsize", "3")));
        assertEquals(new ExecKeys.Key(0x87, 0x76), pressed.get(), "F24 codes are sent");
    }

    @Test
    void reportsWindowNotFoundButStillWritesTheCfg() throws IOException {
        ExecApplier applier = applier(key -> Cs2Window.PressResult.WINDOW_NOT_FOUND, "l");

        assertEquals(ExecApplier.Result.CFG_ONLY_WINDOW_NOT_FOUND,
                applier.apply(Map.of("cl_crosshairsize", "3")));
        assertTrue(Files.isRegularFile(tempDir.resolve("randomizer.cfg")));
    }

    @Test
    void reportsFocusDenied() throws IOException {
        ExecApplier applier = applier(key -> Cs2Window.PressResult.FOCUS_DENIED, "l");

        assertEquals(ExecApplier.Result.CFG_ONLY_FOCUS_DENIED,
                applier.apply(Map.of("cl_crosshairsize", "3")));
    }

    @Test
    void missingCfgFolderSkipsTheKeyPress() throws IOException {
        AtomicInteger presses = new AtomicInteger();
        ExecApplier applier = new ExecApplier(new ExecConfig(Optional::empty), key -> {
            presses.incrementAndGet();
            return Cs2Window.PressResult.PRESSED;
        }, () -> "l");

        assertEquals(ExecApplier.Result.CFG_FOLDER_MISSING, applier.apply(Map.of("cl_crosshairsize", "3")));
        assertEquals(0, presses.get(), "no key press without a written cfg");
    }

    @Test
    void invalidConfiguredKeyFallsBackToDefault() throws IOException {
        AtomicReference<ExecKeys.Key> pressed = new AtomicReference<>();
        ExecApplier applier = applier(key -> {
            pressed.set(key);
            return Cs2Window.PressResult.PRESSED;
        }, "banana");

        applier.apply(Map.of("cl_crosshairsize", "3"));

        assertEquals(new ExecKeys.Key(0x4C, 0x26), pressed.get(), "falls back to the L key");
        assertEquals("l", applier.keyName());
    }

    @Test
    void keyNameNormalizesTheConfiguredValue() {
        assertEquals("f10", applier(key -> Cs2Window.PressResult.PRESSED, " F10 ").keyName());
    }
}
