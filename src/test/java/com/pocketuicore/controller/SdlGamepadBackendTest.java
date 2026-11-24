package com.pocketuicore.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SdlGamepadBackendTest {

    @Test
    void normalizeAxisMapsFullRangeToUnitInterval() {
        assertEquals(0f, SdlGamepadBackend.normalizeAxis((short) 0), 1e-6);
        assertEquals(1f, SdlGamepadBackend.normalizeAxis(Short.MAX_VALUE), 1e-6);
        assertEquals(-1f, SdlGamepadBackend.normalizeAxis(Short.MIN_VALUE), 1e-6);
        assertEquals(0.5f, SdlGamepadBackend.normalizeAxis((short) 16384), 1e-3);
    }

    @Test
    void rumbleIntensityConvertsToUnsignedShortRange() {
        assertEquals(0, SdlGamepadBackend.toRumbleMagnitude(0f) & 0xFFFF);
        assertEquals(0xFFFF, SdlGamepadBackend.toRumbleMagnitude(1f) & 0xFFFF);
        assertEquals(0, SdlGamepadBackend.toRumbleMagnitude(-3f) & 0xFFFF);
        assertEquals(0xFFFF, SdlGamepadBackend.toRumbleMagnitude(7f) & 0xFFFF);
    }

    @Test
    void creatingBackendNeverThrowsAndFailsSoftWithoutNatives() {
        // On a headless unit-test JVM there is no SDL native library / controller.
        SdlGamepadBackend backend = assertDoesNotThrow(SdlGamepadBackend::new);
        assertDoesNotThrow(backend::update);
        assertDoesNotThrow(backend::isConnected);
        assertDoesNotThrow(() -> backend.isPressed(GamepadButton.A));
        assertDoesNotThrow(() -> backend.axis(GamepadAxis.LEFT_X));
        assertDoesNotThrow(() -> backend.rumble(1f, 1f, 100));
        assertDoesNotThrow(backend::close);
    }
}
