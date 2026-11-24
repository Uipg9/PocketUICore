package com.pocketuicore.controller;

/**
 * Source of gamepad state. Implementations must never throw from any method;
 * a missing device or native library simply reports "not connected".
 */
public interface GamepadBackend {
    /** Refresh device state; call once per client tick before querying. */
    void update();

    /** @return {@code true} if a usable gamepad is currently attached. */
    boolean isConnected();

    /** @return {@code true} if the button is held. Always false when disconnected. */
    boolean isPressed(GamepadButton button);

    /** @return axis value in [-1, 1]; 0 when disconnected. Positive Y is down. */
    float axis(GamepadAxis axis);

    /**
     * Start a rumble effect; no-op when unsupported.
     *
     * @param strong     low-frequency motor intensity (0-1)
     * @param weak       high-frequency motor intensity (0-1)
     * @param durationMs duration in milliseconds
     */
    void rumble(float strong, float weak, int durationMs);

    /** Release any native resources. Safe to call repeatedly. */
    void close();
}
