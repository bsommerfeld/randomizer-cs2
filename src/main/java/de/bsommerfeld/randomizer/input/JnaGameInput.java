package de.bsommerfeld.randomizer.input;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import de.bsommerfeld.randomizer.game.ProcessGameDetector;
import de.bsommerfeld.randomizer.input.Keys.Key;

/**
 * Sends input through {@code SendInput}, a plain OS input event with no access to the game process.
 * Keyboard keys go out as scancodes ({@code KEYEVENTF_SCANCODE}), because CS2 reads raw input and
 * ignores events that only carry a virtual key.
 *
 * <p>Never moves the focus. {@code SendInput} lands in whatever window is in front, so callers ask
 * {@link #isCs2Foreground()} first.
 */
public final class JnaGameInput implements GameInput {

    private static final int MOUSEEVENTF_XDOWN = 0x0080;
    private static final int MOUSEEVENTF_XUP = 0x0100;

    private record Foreground(WinDef.HWND window, boolean cs2) {
    }

    /** Asked from the runner's thread and the watching one. */
    private volatile Foreground lastForeground;

    @Override
    public boolean press(Key key) {
        return send(key, false);
    }

    @Override
    public void release(Key key) {
        send(key, true);
    }

    /**
     * The process lookup is remembered per window. The raw input watcher asks on every key event, and
     * {@code ProcessHandle.info()} opens the process and resolves its user each time.
     *
     * <p>ponytail: a window handle that Windows gives to another process while it stays the foreground
     * handle keeps the old answer. Compare the process id too if that ever shows up.
     */
    @Override
    public boolean isCs2Foreground() {
        try {
            WinDef.HWND window = User32.INSTANCE.GetForegroundWindow();
            if (window == null) {
                return false;
            }
            Foreground known = lastForeground;
            if (known == null || !known.window().equals(window)) {
                known = new Foreground(window, belongsToCs2(window));
                lastForeground = known;
            }
            return known.cs2();
        } catch (RuntimeException | UnsatisfiedLinkError notWindows) {
            return false;
        }
    }

    /** Goes by the process behind the window. A title match would also hit a browser tab named like the game. */
    private static boolean belongsToCs2(WinDef.HWND window) {
        IntByReference processId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, processId);
        return ProcessHandle.of(processId.getValue())
                .flatMap(process -> process.info().command())
                .filter(ProcessGameDetector::isCs2Executable)
                .isPresent();
    }

    /** False when Windows did not insert the event, SendInput then reports 0 events sent. */
    private static boolean send(Key key, boolean release) {
        WinUser.INPUT input = key.mouse() ? mouseInput(key.code(), release) : keyboardInput(key.code(), release);
        return User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size())
                .intValue() == 1;
    }

    private static WinUser.INPUT keyboardInput(int scanCode, boolean release) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(0); // ignored with KEYEVENTF_SCANCODE
        input.input.ki.wScan = new WinDef.WORD(scanCode);
        input.input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_SCANCODE
                | (release ? WinUser.KEYBDINPUT.KEYEVENTF_KEYUP : 0));
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        return input;
    }

    private static WinUser.INPUT mouseInput(int button, boolean release) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType("mi");
        input.input.mi.dx = new WinDef.LONG(0);
        input.input.mi.dy = new WinDef.LONG(0);
        // MOUSE4 and MOUSE5 share one flag pair and say which of the two in mouseData (XBUTTON1 = 1, XBUTTON2 = 2).
        input.input.mi.mouseData = new WinDef.DWORD(button > 3 ? button - 3 : 0);
        input.input.mi.dwFlags = new WinDef.DWORD(mouseFlag(button, release));
        input.input.mi.time = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        return input;
    }

    /** MOUSEEVENTF_*: left 0x2/0x4, right 0x8/0x10, middle 0x20/0x40, each button's up flag is twice its down flag. */
    private static int mouseFlag(int button, boolean release) {
        if (button > 3) {
            return release ? MOUSEEVENTF_XUP : MOUSEEVENTF_XDOWN;
        }
        int down = 0x0002 << (2 * (button - 1));
        return release ? down << 1 : down;
    }
}
