package com.pocketuicore.controller;

/**
 * Pure (Minecraft-free) translation of gamepad state into UI actions:
 * button edges, d-pad / left-stick directional navigation with
 * delay+rate repeat, and right-stick scrolling.
 */
public final class GamepadNavigator {

    /** Dead-zone for analog sticks (0-1); values must exceed it strictly. */
    public static final float STICK_DEADZONE = 0.45f;
    /** Ticks before a held direction starts repeating. */
    public static final int NAV_REPEAT_DELAY = 8;
    /** Ticks between repeats once repeating. */
    public static final int NAV_REPEAT_RATE = 3;
    /** Scroll speed multiplier for right stick. */
    public static final float SCROLL_SPEED = 12.0f;

    private boolean prevA, prevB, prevLB, prevRB;
    private final int[] dpadTicks = new int[4];
    private int stickTicksX, stickTicksY;

    public static boolean beyondDeadZone(float value) {
        return Math.abs(value) > STICK_DEADZONE;
    }

    /** Forget press memory and repeat timers. */
    public void reset() {
        prevA = prevB = prevLB = prevRB = false;
        java.util.Arrays.fill(dpadTicks, 0);
        stickTicksX = stickTicksY = 0;
    }

    /**
     * Process one tick.
     *
     * @return {@code true} if a gamepad was connected (state was processed)
     */
    public boolean tick(GamepadBackend pad, GamepadActions out) {
        if (!pad.isConnected()) {
            reset();
            return false;
        }

        boolean a  = pad.isPressed(GamepadButton.A);
        boolean b  = pad.isPressed(GamepadButton.B);
        boolean lb = pad.isPressed(GamepadButton.LEFT_BUMPER);
        boolean rb = pad.isPressed(GamepadButton.RIGHT_BUMPER);

        if (a && !prevA) out.activate();
        if (b && !prevB) out.close();
        if (rb && !prevRB) out.focusNext();
        if (lb && !prevLB) out.focusPrevious();

        dpad(out, pad.isPressed(GamepadButton.DPAD_UP),    NavDirection.UP);
        dpad(out, pad.isPressed(GamepadButton.DPAD_DOWN),  NavDirection.DOWN);
        dpad(out, pad.isPressed(GamepadButton.DPAD_LEFT),  NavDirection.LEFT);
        dpad(out, pad.isPressed(GamepadButton.DPAD_RIGHT), NavDirection.RIGHT);

        stick(out, pad.axis(GamepadAxis.LEFT_X), pad.axis(GamepadAxis.LEFT_Y));

        float ry = pad.axis(GamepadAxis.RIGHT_Y);
        if (beyondDeadZone(ry)) out.scroll(-ry * SCROLL_SPEED);

        prevA = a; prevB = b; prevLB = lb; prevRB = rb;
        return true;
    }

    private static boolean shouldFire(int ticks) {
        return ticks == 0
                || (ticks >= NAV_REPEAT_DELAY && (ticks - NAV_REPEAT_DELAY) % NAV_REPEAT_RATE == 0);
    }

    private void dpad(GamepadActions out, boolean pressed, NavDirection dir) {
        int i = dir.ordinal();
        if (!pressed) {
            dpadTicks[i] = 0;
            return;
        }
        boolean fire = shouldFire(dpadTicks[i]);
        dpadTicks[i]++;
        if (fire) out.navigate(dir);
    }

    private void stick(GamepadActions out, float lx, float ly) {
        boolean xActive = beyondDeadZone(lx);
        boolean yActive = beyondDeadZone(ly);

        if (xActive && Math.abs(lx) >= Math.abs(ly)) {
            boolean fire = shouldFire(stickTicksX);
            stickTicksX++;
            if (fire) out.navigate(lx > 0 ? NavDirection.RIGHT : NavDirection.LEFT);
        } else {
            stickTicksX = 0;
        }

        // positive Y = down (SDL and GLFW agree)
        if (yActive && Math.abs(ly) > Math.abs(lx)) {
            boolean fire = shouldFire(stickTicksY);
            stickTicksY++;
            if (fire) out.navigate(ly > 0 ? NavDirection.DOWN : NavDirection.UP);
        } else {
            stickTicksY = 0;
        }
    }
}
