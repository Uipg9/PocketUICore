package com.pocketuicore.input;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;

/**
 * Input Helper — Centralised decomposition of MC 1.21.11 input records.
 * <p>
 * Minecraft 1.21.11 introduced the {@link MouseButtonEvent} and {@link KeyEvent} records
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
    //  MouseButtonEvent record decomposition
    // =====================================================================

    /** @return the X coordinate from a MouseButtonEvent record. */
    public static double mouseX(MouseButtonEvent MouseButtonEvent) { return MouseButtonEvent.x(); }

    /** @return the Y coordinate from a MouseButtonEvent record. */
    public static double mouseY(MouseButtonEvent MouseButtonEvent) { return MouseButtonEvent.y(); }

    /** @return the mouse button index from a MouseButtonEvent record. */
    public static int mouseButton(MouseButtonEvent MouseButtonEvent) { return MouseButtonEvent.buttonInfo().button(); }

    /** @return {@code true} if the MouseButtonEvent represents a left-MouseButtonEvent (button 0). */
    public static boolean isLeftClick(MouseButtonEvent MouseButtonEvent) { return mouseButton(MouseButtonEvent) == 0; }

    /** @return {@code true} if the MouseButtonEvent represents a right-MouseButtonEvent (button 1). */
    public static boolean isRightClick(MouseButtonEvent MouseButtonEvent) { return mouseButton(MouseButtonEvent) == 1; }

    /** @return {@code true} if the MouseButtonEvent represents a middle-MouseButtonEvent (button 2). */
    public static boolean isMiddleClick(MouseButtonEvent MouseButtonEvent) { return mouseButton(MouseButtonEvent) == 2; }

    // =====================================================================
    //  KeyEvent record decomposition
    // =====================================================================

    /** @return the GLFW key code from a KeyEvent record. */
    public static int keyCode(KeyEvent input) { return input.key(); }

    /** @return the scancode from a KeyEvent record. */
    public static int scanCode(KeyEvent input) { return input.scancode(); }

    /** @return the modifier bitmask from a KeyEvent record. */
    public static int modifiers(KeyEvent input) { return input.modifiers(); }

    /** @return {@code true} if Shift is held in the given KeyEvent. */
    public static boolean isShiftDown(KeyEvent input) { return (input.modifiers() & 0x01) != 0; }

    /** @return {@code true} if Ctrl is held in the given KeyEvent. */
    public static boolean isCtrlDown(KeyEvent input) { return (input.modifiers() & 0x02) != 0; }

    /** @return {@code true} if Alt is held in the given KeyEvent. */
    public static boolean isAltDown(KeyEvent input) { return (input.modifiers() & 0x04) != 0; }

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
