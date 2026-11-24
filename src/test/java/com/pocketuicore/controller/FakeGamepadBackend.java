package com.pocketuicore.controller;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** In-memory backend for tests. */
final class FakeGamepadBackend implements GamepadBackend {
    boolean connected = true;
    final Set<GamepadButton> down = EnumSet.noneOf(GamepadButton.class);
    final Map<GamepadAxis, Float> axes = new EnumMap<>(GamepadAxis.class);
    int updates;
    float lastStrong, lastWeak;
    int lastRumbleMs = -1;

    @Override public void update() { updates++; }
    @Override public boolean isConnected() { return connected; }
    @Override public boolean isPressed(GamepadButton b) { return connected && down.contains(b); }
    @Override public float axis(GamepadAxis a) { return connected ? axes.getOrDefault(a, 0f) : 0f; }
    @Override public void rumble(float strong, float weak, int durationMs) {
        lastStrong = strong; lastWeak = weak; lastRumbleMs = durationMs;
    }
    @Override public void close() { connected = false; }

    FakeGamepadBackend press(GamepadButton b) { down.add(b); return this; }
    FakeGamepadBackend release(GamepadButton b) { down.remove(b); return this; }
    FakeGamepadBackend stick(GamepadAxis a, float v) { axes.put(a, v); return this; }
}
