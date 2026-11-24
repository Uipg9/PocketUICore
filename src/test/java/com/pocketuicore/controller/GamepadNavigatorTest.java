package com.pocketuicore.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GamepadNavigatorTest {

    /** Records every action as a string. */
    static final class Recorder implements GamepadActions {
        final List<String> log = new ArrayList<>();
        double scrolled;
        @Override public void activate() { log.add("activate"); }
        @Override public void close() { log.add("close"); }
        @Override public void focusNext() { log.add("next"); }
        @Override public void focusPrevious() { log.add("prev"); }
        @Override public void navigate(NavDirection d) { log.add("nav:" + d); }
        @Override public void scroll(double amount) { scrolled += amount; log.add("scroll"); }
    }

    FakeGamepadBackend pad;
    Recorder rec;
    GamepadNavigator nav;

    @BeforeEach
    void setUp() {
        pad = new FakeGamepadBackend();
        rec = new Recorder();
        nav = new GamepadNavigator();
    }

    private void tick() { nav.tick(pad, rec); }

    // -- button mapping -----------------------------------------------------

    @Test
    void aActivatesOnceOnPressEdge() {
        pad.press(GamepadButton.A);
        tick(); tick(); tick();
        assertEquals(List.of("activate"), rec.log);
        pad.release(GamepadButton.A);
        tick();
        pad.press(GamepadButton.A);
        tick();
        assertEquals(List.of("activate", "activate"), rec.log);
    }

    @Test
    void bClosesOnceOnPressEdge() {
        pad.press(GamepadButton.B);
        tick(); tick();
        assertEquals(List.of("close"), rec.log);
    }

    @Test
    void bumpersCycleFocus() {
        pad.press(GamepadButton.RIGHT_BUMPER);
        tick();
        pad.release(GamepadButton.RIGHT_BUMPER).press(GamepadButton.LEFT_BUMPER);
        tick();
        assertEquals(List.of("next", "prev"), rec.log);
    }

    @Test
    void eachDpadButtonMapsToItsDirection() {
        pad.press(GamepadButton.DPAD_UP); tick(); pad.release(GamepadButton.DPAD_UP);
        pad.press(GamepadButton.DPAD_DOWN); tick(); pad.release(GamepadButton.DPAD_DOWN);
        pad.press(GamepadButton.DPAD_LEFT); tick(); pad.release(GamepadButton.DPAD_LEFT);
        pad.press(GamepadButton.DPAD_RIGHT); tick(); pad.release(GamepadButton.DPAD_RIGHT);
        assertEquals(List.of("nav:UP", "nav:DOWN", "nav:LEFT", "nav:RIGHT"), rec.log);
    }

    // -- dead zone ----------------------------------------------------------

    @Test
    void stickInsideDeadZoneDoesNothing() {
        pad.stick(GamepadAxis.LEFT_X, GamepadNavigator.STICK_DEADZONE - 0.01f);
        pad.stick(GamepadAxis.LEFT_Y, -(GamepadNavigator.STICK_DEADZONE - 0.01f));
        pad.stick(GamepadAxis.RIGHT_Y, 0.2f);
        for (int i = 0; i < 30; i++) tick();
        assertTrue(rec.log.isEmpty());
        assertEquals(0.0, rec.scrolled);
    }

    @Test
    void stickPastDeadZoneNavigates() {
        pad.stick(GamepadAxis.LEFT_X, 0.9f);
        tick();
        assertEquals(List.of("nav:RIGHT"), rec.log);
    }

    @Test
    void deadZoneHelperIsStrict() {
        assertFalse(GamepadNavigator.beyondDeadZone(GamepadNavigator.STICK_DEADZONE));
        assertFalse(GamepadNavigator.beyondDeadZone(-GamepadNavigator.STICK_DEADZONE));
        assertTrue(GamepadNavigator.beyondDeadZone(GamepadNavigator.STICK_DEADZONE + 0.001f));
        assertTrue(GamepadNavigator.beyondDeadZone(-1f));
    }

    // -- stick direction ----------------------------------------------------

    @Test
    void stickDirectionsFollowSignAndPositiveYIsDown() {
        pad.stick(GamepadAxis.LEFT_X, -0.9f); tick();
        pad.stick(GamepadAxis.LEFT_X, 0f); tick();
        pad.stick(GamepadAxis.LEFT_Y, 0.9f); tick();
        pad.stick(GamepadAxis.LEFT_Y, 0f); tick();
        pad.stick(GamepadAxis.LEFT_Y, -0.9f); tick();
        assertEquals(List.of("nav:LEFT", "nav:DOWN", "nav:UP"), rec.log);
    }

    @Test
    void dominantAxisWinsOnDiagonal() {
        pad.stick(GamepadAxis.LEFT_X, 0.6f).stick(GamepadAxis.LEFT_Y, 0.9f);
        tick();
        assertEquals(List.of("nav:DOWN"), rec.log);
    }

    // -- repeat timing ------------------------------------------------------

    @Test
    void heldDpadRepeatsAfterDelayThenAtRate() {
        pad.press(GamepadButton.DPAD_DOWN);
        int total = GamepadNavigator.NAV_REPEAT_DELAY + GamepadNavigator.NAV_REPEAT_RATE * 2 + 1;
        for (int i = 0; i < total; i++) tick();
        // fires at tick index 0, DELAY, DELAY+RATE, DELAY+2*RATE
        assertEquals(4, rec.log.size());
    }

    @Test
    void heldDpadDoesNotRepeatBeforeDelay() {
        pad.press(GamepadButton.DPAD_LEFT);
        for (int i = 0; i < GamepadNavigator.NAV_REPEAT_DELAY; i++) tick();
        assertEquals(1, rec.log.size());
        tick();
        assertEquals(2, rec.log.size());
    }

    @Test
    void releasingResetsRepeatTimer() {
        pad.press(GamepadButton.DPAD_UP);
        tick(); tick();
        pad.release(GamepadButton.DPAD_UP);
        tick();
        pad.press(GamepadButton.DPAD_UP);
        tick();
        assertEquals(2, rec.log.size()); // immediate fire on each fresh press
    }

    @Test
    void heldStickRepeatsLikeDpad() {
        pad.stick(GamepadAxis.LEFT_Y, 1f);
        for (int i = 0; i <= GamepadNavigator.NAV_REPEAT_DELAY; i++) tick();
        assertEquals(List.of("nav:DOWN", "nav:DOWN"), rec.log);
    }

    // -- scroll -------------------------------------------------------------

    @Test
    void rightStickScrollsOpposingStickDirectionScaled() {
        pad.stick(GamepadAxis.RIGHT_Y, 1f);
        tick();
        assertEquals(-GamepadNavigator.SCROLL_SPEED, rec.scrolled, 1e-4);
    }

    // -- connection ---------------------------------------------------------

    @Test
    void disconnectedBackendProducesNoActionsAndTickReportsFalse() {
        pad.connected = false;
        pad.down.add(GamepadButton.A);
        assertFalse(nav.tick(pad, rec));
        assertTrue(rec.log.isEmpty());
    }

    @Test
    void connectedTickReportsTrue() {
        assertTrue(nav.tick(pad, rec));
    }

    @Test
    void resetClearsPressMemory() {
        pad.press(GamepadButton.A);
        tick();
        nav.reset();
        tick();
        assertEquals(2, rec.log.size());
    }
}
