package de.bsommerfeld.randomizer.input;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import de.bsommerfeld.randomizer.input.Keys.Key;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Reads along what the user does on their real keyboard and mouse, through Windows Raw Input with
 * {@code RIDEV_INPUTSINK}. Windows hands the app a copy of every input event, also while CS2 has the
 * focus. The copy is passive: nothing is blocked, nothing sits in the path between the device and
 * the game, and it needs no admin rights.
 *
 * <p>Raw input has no setting for "only while that other window is in front", so the copies also
 * arrive while the user types elsewhere. {@link PhysicalKeys} drops those.
 *
 * <p>Raw input tells real from synthesized: an event from a device carries that device's handle,
 * one made by {@code SendInput} carries none. So the randomizer's own key presses never count as
 * the user's.
 *
 * <p>Raw input needs a window to deliver to. This class owns a message-only window and the thread
 * that pumps its messages.
 */
public final class RawInputWatcher implements UserKeys {

    /** The two user32 functions jna-platform does not map. */
    private interface RawInputApi extends StdCallLibrary {
        RawInputApi INSTANCE = Native.load("user32", RawInputApi.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean RegisterRawInputDevices(RawInputDevice[] devices, int count, int size);

        int GetRawInputData(Pointer rawInput, int command, Pointer data, IntByReference size, int headerSize);
    }

    @Structure.FieldOrder({"usUsagePage", "usUsage", "dwFlags", "hwndTarget"})
    public static class RawInputDevice extends Structure {
        public short usUsagePage;
        public short usUsage;
        public int dwFlags;
        public WinDef.HWND hwndTarget;
    }

    private static final String WINDOW_CLASS = "RandomizerCs2RawInput";

    /**
     * HWND_MESSAGE, the parent that makes a window message-only. JNA has the constant too, but builds
     * it from the int -3, which comes out as 0xFFFFFFFD on 64 bit and makes CreateWindowEx fail with 1400.
     */
    private static final WinDef.HWND MESSAGE_ONLY = new WinDef.HWND(Pointer.createConstant(-3L));

    private static final int WM_QUIT = 0x0012;
    private static final int WM_INPUT = 0x00FF;
    private static final int RID_INPUT = 0x10000003;
    private static final int RIDEV_INPUTSINK = 0x00000100;
    private static final int RIM_TYPEMOUSE = 0;
    private static final int RIM_TYPEKEYBOARD = 1;
    private static final short USAGE_PAGE_GENERIC_DESKTOP = 0x01;
    private static final short USAGE_MOUSE = 0x02;
    private static final short USAGE_KEYBOARD = 0x06;

    /** RAWINPUTHEADER: dwType, dwSize, hDevice, wParam. The device handle sits after the two DWORDs. */
    private static final int HEADER_SIZE = 8 + 2 * Native.POINTER_SIZE;
    private static final int DEVICE_OFFSET = 8;
    /** RAWMOUSE: usFlags, two bytes of padding, then usButtonFlags. RAWKEYBOARD: MakeCode, then Flags. */
    private static final int MOUSE_BUTTON_FLAGS_OFFSET = HEADER_SIZE + 4;
    private static final int KEYBOARD_MAKE_CODE_OFFSET = HEADER_SIZE;
    private static final int KEYBOARD_FLAGS_OFFSET = HEADER_SIZE + 2;

    private final PhysicalKeys keys;
    /** Kept in a field because the window calls it from native code. A collected callback would crash the JVM. */
    private final WinUser.WindowProc windowProc = User32.INSTANCE::DefWindowProc;

    /** The thread whose message loop is meant to run. A loop that finds another value here ends itself. */
    private volatile Thread thread;
    private volatile int nativeThreadId;

    /** {@code cs2Foreground} is asked on every key and button event, on the watching thread, so it has to be cheap. */
    public RawInputWatcher(BooleanSupplier cs2Foreground) {
        this.keys = new PhysicalKeys(cs2Foreground);
    }

    @Override
    public boolean holds(Key key) {
        return keys.holds(key);
    }

    @Override
    public void onRelease(Consumer<Key> listener) {
        keys.onRelease(listener);
    }

    /** A platform thread on purpose, because it sits in a native {@code GetMessage} for its whole life. */
    @Override
    public synchronized void start() {
        if (thread == null) {
            keys.clear();
            nativeThreadId = 0;
            thread = Thread.ofPlatform().daemon().name("raw-input-watcher").unstarted(this::pumpMessages);
            thread.start(); // only after the assignment, the loop compares itself with the field
        }
    }

    /** Ends the message loop, which also takes the window and with it the raw input registration away. */
    @Override
    public synchronized void stop() {
        if (thread != null && nativeThreadId != 0) {
            User32.INSTANCE.PostThreadMessage(nativeThreadId, WM_QUIT, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
        }
        thread = null;
        keys.clear();
    }

    private void pumpMessages() {
        try {
            WinDef.HMODULE module = Kernel32.INSTANCE.GetModuleHandle(null);
            WinDef.HWND window = createMessageWindow(module);
            try {
                registerKeyboardAndMouse(window);
                nativeThreadId = Kernel32.INSTANCE.GetCurrentThreadId();
                Memory buffer = new Memory(64); // a mouse or keyboard RAWINPUT is 48 bytes at most
                WinUser.MSG message = new WinUser.MSG();
                // The first check covers a stop() that came before this thread had an id to post WM_QUIT to.
                while (thread == Thread.currentThread() && User32.INSTANCE.GetMessage(message, null, 0, 0) > 0) {
                    if (message.message == WM_INPUT) {
                        read(message.lParam, buffer);
                    }
                    User32.INSTANCE.DispatchMessage(message); // DefWindowProc frees the raw input
                }
            } finally {
                User32.INSTANCE.DestroyWindow(window);
                User32.INSTANCE.UnregisterClass(WINDOW_CLASS, module);
            }
        } catch (RuntimeException | UnsatisfiedLinkError failed) {
            // Without the watcher the randomizer releases every key it pressed, even one the user holds.
            // Nothing else would show that, so it goes to stderr.
            System.err.println("Raw input watcher is not running: " + failed);
        }
    }

    private WinDef.HWND createMessageWindow(WinDef.HMODULE module) {
        WinUser.WNDCLASSEX windowClass = new WinUser.WNDCLASSEX();
        windowClass.hInstance = module;
        windowClass.lpszClassName = WINDOW_CLASS;
        windowClass.lpfnWndProc = windowProc;
        User32.INSTANCE.RegisterClassEx(windowClass);
        WinDef.HWND window = User32.INSTANCE.CreateWindowEx(0, WINDOW_CLASS, WINDOW_CLASS, 0, 0, 0, 0, 0,
                MESSAGE_ONLY, null, module, null);
        if (window == null) {
            throw new IllegalStateException("message window not created, error " + Native.getLastError());
        }
        return window;
    }

    private static void registerKeyboardAndMouse(WinDef.HWND window) {
        RawInputDevice[] devices = (RawInputDevice[]) new RawInputDevice().toArray(2);
        short[] usages = {USAGE_MOUSE, USAGE_KEYBOARD};
        for (int i = 0; i < devices.length; i++) {
            devices[i].usUsagePage = USAGE_PAGE_GENERIC_DESKTOP;
            devices[i].usUsage = usages[i];
            devices[i].dwFlags = RIDEV_INPUTSINK;
            devices[i].hwndTarget = window;
            devices[i].write();
        }
        if (!RawInputApi.INSTANCE.RegisterRawInputDevices(devices, devices.length, devices[0].size())) {
            throw new IllegalStateException("raw input not registered, error " + Native.getLastError());
        }
    }

    /** Copies one raw input event into {@code buffer} and hands it to {@link #keys} if a real device sent it. */
    private void read(WinDef.LPARAM rawInputHandle, Memory buffer) {
        IntByReference size = new IntByReference((int) buffer.size());
        int copied = RawInputApi.INSTANCE.GetRawInputData(
                new Pointer(rawInputHandle.longValue()), RID_INPUT, buffer, size, HEADER_SIZE);
        if (copied <= 0) {
            return;
        }
        boolean synthesized = (Native.POINTER_SIZE == 8
                ? buffer.getLong(DEVICE_OFFSET)
                : buffer.getInt(DEVICE_OFFSET)) == 0;
        if (synthesized) {
            return;
        }
        int type = buffer.getInt(0);
        if (type == RIM_TYPEMOUSE) {
            keys.mouse(buffer.getShort(MOUSE_BUTTON_FLAGS_OFFSET) & 0xFFFF);
        } else if (type == RIM_TYPEKEYBOARD) {
            keys.keyboard(buffer.getShort(KEYBOARD_MAKE_CODE_OFFSET) & 0xFFFF,
                    buffer.getShort(KEYBOARD_FLAGS_OFFSET) & 0xFFFF);
        }
    }
}
