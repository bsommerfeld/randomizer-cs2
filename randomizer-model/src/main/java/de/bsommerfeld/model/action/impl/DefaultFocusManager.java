package de.bsommerfeld.model.action.impl;

import com.google.inject.Singleton;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import de.bsommerfeld.model.action.spi.FocusManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the FocusManager interface.
 * This class checks if the Counter-Strike 2 window is currently in focus.
 */
@Slf4j
@Singleton
public class DefaultFocusManager implements FocusManager {

    /**
     * Checks if the window currently in focus belongs to the "Counter-Strike 2" game.
     *
     * @return true if the window in focus is "Counter-Strike 2", false otherwise
     */
    @Override
    public boolean isApplicationWindowInFocus() {
        try {
            User32 user32 = User32.INSTANCE;
            HWND hwnd = user32.GetForegroundWindow();

            if (hwnd == null) {
                return false;
            }

            char[] windowText = new char[512];
            user32.GetWindowText(hwnd, windowText, 512);
            String wText = Native.toString(windowText);

            return wText.contains("Counter-Strike 2");
        } catch (UnsatisfiedLinkError e) {
            log.error("JNA is not properly set up", e);
            return false;
        } catch (Exception e) {
            log.error("Error while checking for CS2 focus", e);
            return false;
        }
    }
}