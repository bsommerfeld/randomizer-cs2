package de.bsommerfeld.randomizer.exec;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * JNA-backed {@link Cs2Window}: finds the CS2 window by title, brings it to the foreground and
 * synthesizes the key press via {@code SendInput} - a plain OS input event, no access to the game
 * process. The press is sent as a hardware scancode ({@code KEYEVENTF_SCANCODE}): CS2 reads input
 * via scancodes/raw input and ignores events that only carry a virtual key. Returns
 * {@link PressResult#WINDOW_NOT_FOUND} on non-Windows platforms or any native failure.
 */
public final class JnaCs2Window implements Cs2Window {

    private static final String WINDOW_TITLE = "Counter-Strike 2";
    /** Give Windows a moment to complete the foreground switch before sending input. */
    private static final long FOCUS_WAIT_MILLIS = 250;
    /** Hold the key briefly so the game's input polling reliably sees the press. */
    private static final long KEY_HOLD_MILLIS = 30;

    @Override
    public PressResult pressKey(ExecKeys.Key key) {
        try {
            WinDef.HWND window = User32.INSTANCE.FindWindow(null, WINDOW_TITLE);
            if (window == null) {
                return PressResult.WINDOW_NOT_FOUND;
            }
            User32.INSTANCE.SetForegroundWindow(window);
            Thread.sleep(FOCUS_WAIT_MILLIS);
            if (!window.equals(User32.INSTANCE.GetForegroundWindow())) {
                return PressResult.FOCUS_DENIED;
            }
            sendScanCode(key.scanCode(), false);
            Thread.sleep(KEY_HOLD_MILLIS);
            sendScanCode(key.scanCode(), true);
            return PressResult.PRESSED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PressResult.FOCUS_DENIED;
        } catch (RuntimeException | UnsatisfiedLinkError e) {
            return PressResult.WINDOW_NOT_FOUND;
        }
    }

    private static void sendScanCode(int scanCode, boolean release) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(0); // ignored with KEYEVENTF_SCANCODE
        input.input.ki.wScan = new WinDef.WORD(scanCode);
        input.input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_SCANCODE
                | (release ? WinUser.KEYBDINPUT.KEYEVENTF_KEYUP : 0));
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        User32.INSTANCE.SendInput(new WinDef.DWORD(1),
                (WinUser.INPUT[]) input.toArray(1), input.size());
    }
}
