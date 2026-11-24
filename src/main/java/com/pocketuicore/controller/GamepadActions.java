package com.pocketuicore.controller;

/** Sink for high-level actions derived from gamepad input. */
public interface GamepadActions {
    void activate();
    void close();
    void focusNext();
    void focusPrevious();
    void navigate(NavDirection direction);
    /** @param amount scroll delta in pixels (positive = scroll down). */
    void scroll(double amount);
}
