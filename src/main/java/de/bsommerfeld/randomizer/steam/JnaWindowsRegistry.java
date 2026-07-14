package de.bsommerfeld.randomizer.steam;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.util.Optional;

/** Registry access via JNA. Missing keys or access errors yield an empty Optional. */
public final class JnaWindowsRegistry implements WindowsRegistry {

    @Override
    public Optional<String> readString(Hive hive, String keyPath, String valueName) {
        if (!Platform.isWindows()) {
            return Optional.empty();
        }
        WinReg.HKEY root = hive == Hive.CURRENT_USER ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE;
        try {
            if (!Advapi32Util.registryValueExists(root, keyPath, valueName)) {
                return Optional.empty();
            }
            return Optional.ofNullable(Advapi32Util.registryGetStringValue(root, keyPath, valueName));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
