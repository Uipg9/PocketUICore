package com.pocketuicore.input;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;

/**
 * Input Helper — Centralised decomposition of MC 1.21.11 input records.
 * <p>
 * Minecraft 1.21.11 introduced the {@link Click} and {@link KeyInput} records
 * which bundle mouse/keyboard data. This utility provides clean extraction
 * methods so downstream code does not couple to the internal record layout.
 * <p>
 * Also provides an {@link InputMode} enum and detection for whether the
 * user last interacted via keyboard, mouse, or controller.
 *
 * @since 1.13.0
 */
public final class InputHelper {

    private InputHelper() { /* static utility */ }

    /** The type of input device the user last interacted with. */
    public enum InputMode {
        KEYBOARD_MOUSE,
        CONTROLLER
    }

    private static InputMode currentMode = InputMode.KEYBOARD_MOUSE;

    // =====================================================================
    //  Click record decomposition
    // =====================================================================

    /** @return the X coordinate from a Click record. */
    public static double mouseX(Click click) { return click.x(); }

    /** @return the Y coordinate from a Click record. */
    public static double mouseY(Click click) { return click.y(); }

    /** @return the mouse button index from a Click record. */
    public static int mouseButton(Click click) { return click.buttonInfo().button(); }

    /** @return {@code true} if the Click represents a left-click (button 0). */
    public static boolean isLeftClick(Click click) { return mouseButton(click) == 0; }

    /** @return {@code true} if the Click represents a right-click (button 1). */
    public static boolean isRightClick(Click click) { return mouseButton(click) == 1; }

    /** @return {@code true} if the Click represents a middle-click (button 2). */
    public static boolean isMiddleClick(Click click) { return mouseButton(click) == 2; }

    // =====================================================================
    //  KeyInput record decomposition
    // =====================================================================

    /** @return the GLFW key code from a KeyInput record. */
    public static int keyCode(KeyInput input) { return input.key(); }

    /** @return the scancode from a KeyInput record. */
    public static int scanCode(KeyInput input) { return input.scancode(); }

    /** @return the modifier bitmask from a KeyInput record. */
    public static int modifiers(KeyInput input) { return input.modifiers(); }

    /** @return {@code true} if Shift is held in the given KeyInput. */
    public static boolean isShiftDown(KeyInput input) { return (input.modifiers() & 0x01) != 0; }

    /** @return {@code true} if Ctrl is held in the given KeyInput. */
    public static boolean isCtrlDown(KeyInput input) { return (input.modifiers() & 0x02) != 0; }

    /** @return {@code true} if Alt is held in the given KeyInput. */
    public static boolean isAltDown(KeyInput input) { return (input.modifiers() & 0x04) != 0; }

    // =====================================================================
    //  Input mode tracking
    // =====================================================================

    /**
     * Notify InputHelper that a keyboard/mouse event was received.
     * Call this from your screen's key/mouse handlers.
     */
    public static void markKeyboardMouse() {
        currentMode = InputMode.KEYBOARD_MOUSE;
    }

    /**
     * Notify InputHelper that a controller event was received.
     * Called automatically by ControllerHandler.
     */
    public static void markController() {
        currentMode = InputMode.CONTROLLER;
    }

    /** @return the current detected input mode. */
    public static InputMode getCurrentMode() { return currentMode; }

    /** @return {@code true} if the user is currently using a controller. */
    public static boolean isControllerActive() { return currentMode == InputMode.CONTROLLER; }

    /** @return {@code true} if the user is currently using keyboard/mouse. */
    public static boolean isKeyboardMouseActive() { return currentMode == InputMode.KEYBOARD_MOUSE; }
}
