package com.pocketuicore.input;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Key, mouse-button and modifier constants for PocketUICore (Minecraft 26.3+).
 * <p>
 * Minecraft 26.x replaced GLFW with SDL3, so the old {@code GLFW_KEY_*} values
 * no longer exist. All constants here alias {@link InputConstants} (compile-time
 * constants, so this class can be loaded without a running client). The values are
 * <b>SDL scancodes</b>, and mouse buttons are 1-based
 * ({@link #MOUSE_LEFT} == 1, not 0 as in GLFW).
 * <p>
 * Note: unlike GLFW (ASCII order), SDL scancodes place KEY_0 <i>after</i> KEY_9.
 * Use {@link #digitIndex(int)} / {@link #isDigit(int)} instead of range checks.
 */
public final class PocketKeys {
    private PocketKeys() { }

    // -- Keys (GLFW_KEY_x -> InputConstants.KEY_y) -------------------------
    public static final int KEY_UP = InputConstants.KEY_UP;
    public static final int KEY_DOWN = InputConstants.KEY_DOWN;
    public static final int KEY_LEFT = InputConstants.KEY_LEFT;
    public static final int KEY_RIGHT = InputConstants.KEY_RIGHT;
    public static final int KEY_ENTER = InputConstants.KEY_RETURN;
    public static final int KEY_KP_ENTER = InputConstants.KEY_NUMPADENTER;
    public static final int KEY_TAB = InputConstants.KEY_TAB;
    public static final int KEY_ESCAPE = InputConstants.KEY_ESCAPE;
    public static final int KEY_SPACE = InputConstants.KEY_SPACE;
    public static final int KEY_HOME = InputConstants.KEY_HOME;
    public static final int KEY_END = InputConstants.KEY_END;
    public static final int KEY_DELETE = InputConstants.KEY_DELETE;
    public static final int KEY_BACKSPACE = InputConstants.KEY_BACKSPACE;
    public static final int KEY_PAGE_UP = InputConstants.KEY_PAGEUP;
    public static final int KEY_PAGE_DOWN = InputConstants.KEY_PAGEDOWN;
    public static final int KEY_LEFT_SHIFT = InputConstants.KEY_LSHIFT;
    public static final int KEY_RIGHT_SHIFT = InputConstants.KEY_RSHIFT;
    public static final int KEY_LEFT_CONTROL = InputConstants.KEY_LCONTROL;
    public static final int KEY_RIGHT_CONTROL = InputConstants.KEY_RCONTROL;
    public static final int KEY_LEFT_ALT = InputConstants.KEY_LALT;
    public static final int KEY_RIGHT_ALT = InputConstants.KEY_RALT;
    public static final int KEY_A = InputConstants.KEY_A;
    public static final int KEY_C = InputConstants.KEY_C;
    public static final int KEY_E = InputConstants.KEY_E;
    public static final int KEY_H = InputConstants.KEY_H;
    public static final int KEY_P = InputConstants.KEY_P;
    public static final int KEY_V = InputConstants.KEY_V;
    public static final int KEY_X = InputConstants.KEY_X;
    public static final int KEY_Z = InputConstants.KEY_Z;
    public static final int KEY_0 = InputConstants.KEY_0;
    public static final int KEY_1 = InputConstants.KEY_1;
    public static final int KEY_2 = InputConstants.KEY_2;
    public static final int KEY_3 = InputConstants.KEY_3;
    public static final int KEY_4 = InputConstants.KEY_4;
    public static final int KEY_5 = InputConstants.KEY_5;
    public static final int KEY_6 = InputConstants.KEY_6;
    public static final int KEY_7 = InputConstants.KEY_7;
    public static final int KEY_8 = InputConstants.KEY_8;
    public static final int KEY_9 = InputConstants.KEY_9;
    public static final int KEY_F1 = InputConstants.KEY_F1;
    public static final int KEY_F2 = InputConstants.KEY_F2;
    public static final int KEY_F3 = InputConstants.KEY_F3;
    public static final int KEY_F4 = InputConstants.KEY_F4;
    public static final int KEY_F5 = InputConstants.KEY_F5;
    public static final int KEY_F6 = InputConstants.KEY_F6;
    public static final int KEY_F7 = InputConstants.KEY_F7;
    public static final int KEY_F8 = InputConstants.KEY_F8;
    public static final int KEY_F9 = InputConstants.KEY_F9;
    public static final int KEY_F10 = InputConstants.KEY_F10;
    public static final int KEY_F11 = InputConstants.KEY_F11;
    public static final int KEY_F12 = InputConstants.KEY_F12;

    // -- Mouse buttons ------------------------------------------------------
    public static final int MOUSE_LEFT   = InputConstants.MOUSE_BUTTON_LEFT;
    public static final int MOUSE_RIGHT  = InputConstants.MOUSE_BUTTON_RIGHT;
    public static final int MOUSE_MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE;

    // -- Modifier masks (SDL keymod; each covers both left and right keys) --
    public static final int MOD_SHIFT   = InputConstants.MOD_SHIFT;
    public static final int MOD_CONTROL = InputConstants.MOD_CONTROL;
    public static final int MOD_ALT     = InputConstants.MOD_ALT;

    // -- Helpers ------------------------------------------------------------
    public static boolean hasShift(int modifiers)   { return (modifiers & MOD_SHIFT) != 0; }
    public static boolean hasControl(int modifiers) { return (modifiers & MOD_CONTROL) != 0; }
    public static boolean hasAlt(int modifiers)     { return (modifiers & MOD_ALT) != 0; }

    /** @return {@code true} for main Enter or keypad Enter. */
    public static boolean isEnter(int key) { return key == KEY_ENTER || key == KEY_KP_ENTER; }

    /** @return {@code true} if the mouse button is the primary (left) button. */
    public static boolean isLeftClick(int button) { return button == MOUSE_LEFT; }

    /** @return 0-8 for KEY_1..KEY_9, otherwise -1 (KEY_0 is not a tab hotkey). */
    public static int digitIndex(int key) {
        return (key >= KEY_1 && key <= KEY_9) ? key - KEY_1 : -1;
    }

    /** @return {@code true} for KEY_0..KEY_9 (handles SDL's 1..9,0 ordering). */
    public static boolean isDigit(int key) { return key == KEY_0 || digitIndex(key) >= 0; }

    /** @return 'A'-'Z' / '0'-'9' for letter and digit keys, otherwise {@code (char) 0}. */
    public static char alphanumericChar(int key) {
        if (key >= KEY_A && key <= KEY_A + 25) return (char) ('A' + (key - KEY_A));
        if (key == KEY_0) return '0';
        int d = digitIndex(key);
        return d >= 0 ? (char) ('1' + d) : (char) 0;
    }
}
