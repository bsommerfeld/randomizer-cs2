package de.bsommerfeld.model.action.mapper;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * The KeyMapper class provides a mapping between key strings and their corresponding key codes. It supports common
 * keyboard keys, function keys, modifier keys, navigation keys, control keys, and mouse buttons and scroll wheel
 * events.
 */
public class KeyMapper {

    private static final int MWHEELDOWN_KEYCODE = -1;
    private static final int MWHEELUP_KEYCODE = 1;

    private final Map<String, Integer> stringToKeyCodeMap = new HashMap<>();

    {
        // Letters
        addMapping("A", KeyEvent.VK_A);
        addMapping("B", KeyEvent.VK_B);
        addMapping("C", KeyEvent.VK_C);
        addMapping("D", KeyEvent.VK_D);
        addMapping("E", KeyEvent.VK_E);
        addMapping("F", KeyEvent.VK_F);
        addMapping("G", KeyEvent.VK_G);
        addMapping("H", KeyEvent.VK_H);
        addMapping("I", KeyEvent.VK_I);
        addMapping("J", KeyEvent.VK_J);
        addMapping("K", KeyEvent.VK_K);
        addMapping("L", KeyEvent.VK_L);
        addMapping("M", KeyEvent.VK_M);
        addMapping("N", KeyEvent.VK_N);
        addMapping("O", KeyEvent.VK_O);
        addMapping("P", KeyEvent.VK_P);
        addMapping("Q", KeyEvent.VK_Q);
        addMapping("R", KeyEvent.VK_R);
        addMapping("S", KeyEvent.VK_S);
        addMapping("T", KeyEvent.VK_T);
        addMapping("U", KeyEvent.VK_U);
        addMapping("V", KeyEvent.VK_V);
        addMapping("W", KeyEvent.VK_W);
        addMapping("X", KeyEvent.VK_X);
        addMapping("Y", KeyEvent.VK_Y);
        addMapping("Z", KeyEvent.VK_Z);

        // Numbers
        addMapping("0", KeyEvent.VK_0);
        addMapping("1", KeyEvent.VK_1);
        addMapping("2", KeyEvent.VK_2);
        addMapping("3", KeyEvent.VK_3);
        addMapping("4", KeyEvent.VK_4);
        addMapping("5", KeyEvent.VK_5);
        addMapping("6", KeyEvent.VK_6);
        addMapping("7", KeyEvent.VK_7);
        addMapping("8", KeyEvent.VK_8);
        addMapping("9", KeyEvent.VK_9);

        // Function keys
        addMapping("F1", KeyEvent.VK_F1);
        addMapping("F2", KeyEvent.VK_F2);
        addMapping("F3", KeyEvent.VK_F3);
        addMapping("F4", KeyEvent.VK_F4);
        addMapping("F5", KeyEvent.VK_F5);
        addMapping("F6", KeyEvent.VK_F6);
        addMapping("F7", KeyEvent.VK_F7);
        addMapping("F8", KeyEvent.VK_F8);
        addMapping("F9", KeyEvent.VK_F9);
        addMapping("F10", KeyEvent.VK_F10);
        addMapping("F11", KeyEvent.VK_F11);
        addMapping("F12", KeyEvent.VK_F12);

        // Modifier keys
        addMapping("CTRL", KeyEvent.VK_CONTROL);
        addMapping("SHIFT", KeyEvent.VK_SHIFT);
        addMapping("ALT", KeyEvent.VK_ALT);
        addMapping("ALTGR", KeyEvent.VK_ALT_GRAPH);

        // Navigation keys
        addMapping("UP", KeyEvent.VK_UP);
        addMapping("DOWN", KeyEvent.VK_DOWN);
        addMapping("LEFT", KeyEvent.VK_LEFT);
        addMapping("RIGHT", KeyEvent.VK_RIGHT);
        addMapping("PAGE_UP", KeyEvent.VK_PAGE_UP);
        addMapping("PAGE_DOWN", KeyEvent.VK_PAGE_DOWN);
        addMapping("HOME", KeyEvent.VK_HOME);
        addMapping("END", KeyEvent.VK_END);
        addMapping("INSERT", KeyEvent.VK_INSERT);
        addMapping("DELETE", KeyEvent.VK_DELETE);

        // Control keys
        addMapping("ENTER", KeyEvent.VK_ENTER);
        addMapping("ESCAPE", KeyEvent.VK_ESCAPE);
        addMapping("TAB", KeyEvent.VK_TAB);
        addMapping("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        addMapping("SPACE", KeyEvent.VK_SPACE);
        addMapping("CAPSLOCK", KeyEvent.VK_CAPS_LOCK);
        addMapping("PRINTSCREEN", KeyEvent.VK_PRINTSCREEN);
        addMapping("SCROLL_LOCK", KeyEvent.VK_SCROLL_LOCK);
        addMapping("PAUSE", KeyEvent.VK_PAUSE);

        // Mouse keys
        addMapping("MOUSE1", InputEvent.getMaskForButton(1));
        addMapping("MOUSE2", InputEvent.getMaskForButton(2));
        addMapping("MOUSE3", InputEvent.getMaskForButton(3));
        addMapping("MOUSE4", InputEvent.getMaskForButton(4));
        addMapping("MOUSE5", InputEvent.getMaskForButton(5));

        // Mouse scroll wheel
        addMapping("MWHEELDOWN", MWHEELDOWN_KEYCODE);
        addMapping("MWHEELUP", MWHEELUP_KEYCODE);
    }

    /**
     * Retrieves the key code for the given key string.
     *
     * @param key the key string for which the key code is to be retrieved.
     *
     * @return the key code corresponding to the key string, or -1 if the key string is not found in the mapping.
     */
    public int getKeyCodeForKey(String key) {
        return stringToKeyCodeMap.getOrDefault(key.toUpperCase(), -1);
    }

    private void addMapping(String key, int keyCode) {
        stringToKeyCodeMap.put(key.toUpperCase(), keyCode);
    }
}
