package com.pocketuicore.controller;

import com.pocketuicore.component.DarkPanel;
import com.pocketuicore.component.FocusManager;
import com.pocketuicore.component.UIComponent;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Controller Handler — GLFW Gamepad Polling
 * <p>
 * Provides native controller support by polling connected gamepads
 * via GLFW every client tick. Routes directional input to
 * {@link FocusManager} for focus navigation, the A/Cross button to
 * component activation, and the right thumbstick to panel scrolling.
 * <p>
 * This handler works out-of-the-box with <b>no</b> external mods.
 * When Controlify is present, it coexists via
 * {@link PocketControlifyCompat} which disables vmouse on PocketUI
 * screens to prevent double-handling.
 * <p>
 * <b>Input mapping (Xbox / PlayStation):</b>
 * <ul>
 *   <li>D-Pad / Left Stick → {@link FocusManager#navigateDirection}</li>
 *   <li>A / Cross → {@link FocusManager#activateFocused()}</li>
 *   <li>Right Stick Y → Scroll active {@link DarkPanel}</li>
 *   <li>LB/RB / L1/R1 → Tab-cycle focus (next / previous)</li>
 * </ul>
 * <p>
 * <b>Virtual cursor snap:</b> When directional input changes the
 * focused component, the OS cursor is warped to the component centre
 * so hover effects and tooltips activate naturally.
 */
public final class ControllerHandler {

    // ── Singleton ────────────────────────────────────────────────────────
    private static final ControllerHandler INSTANCE = new ControllerHandler();
    public static ControllerHandler getInstance() { return INSTANCE; }
    private ControllerHandler() { }

    // ── GLFW gamepad constants ───────────────────────────────────────────
    // Buttons
    private static final int BTN_A      = GLFW.GLFW_GAMEPAD_BUTTON_A;           // 0 — Xbox A / PS Cross
    private static final int BTN_B      = GLFW.GLFW_GAMEPAD_BUTTON_B;           // 1 — Xbox B / PS Circle
    private static final int BTN_LB     = GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER; // 4
    private static final int BTN_RB     = GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;// 5
    private static final int BTN_DPAD_U = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP;     // 11
    private static final int BTN_DPAD_D = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN;   // 13
    private static final int BTN_DPAD_L = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT;   // 14
    private static final int BTN_DPAD_R = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;  // 12

    // Axes
    private static final int AXIS_LX = GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;   // 0
    private static final int AXIS_LY = GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;   // 1
    private static final int AXIS_RY = GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y;  // 3

    // ── Tuning ───────────────────────────────────────────────────────────
    /** Dead-zone for analog sticks (0 ─ 1). */
    private static final float STICK_DEADZONE = 0.45f;
    /** Ticks between repeated d-pad / stick navigation events. */
    private static final int NAV_REPEAT_DELAY  = 8;  // ~400 ms initial
    private static final int NAV_REPEAT_RATE   = 3;  // ~150 ms repeat
    /** Scroll speed multiplier for right stick. */
    private static final float SCROLL_SPEED = 12.0f;

    // ── State ────────────────────────────────────────────────────────────
    private boolean active = false;
    private int connectedJoystick = -1;

    // Button press tracking (previous frame)
    private boolean prevA;
    private boolean prevB;
    private boolean prevLB;
    private boolean prevRB;

    // Navigation repeat timers
    private int navTicksUp;
    private int navTicksDown;
    private int navTicksLeft;
    private int navTicksRight;
    private int stickNavTicksX;
    private int stickNavTicksY;

    // Scroll target — set by the active screen
    private DarkPanel scrollTarget;

    // ── Enabled flag — disabled when no PocketUI screen is open ──────────
    private boolean enabled = false;

    // =====================================================================
    //  Public API
    // =====================================================================

    /** Enable controller polling (called when a PocketUI screen opens). */
    public void enable() {
        this.enabled = true;
        resetState();
    }

    /** Disable controller polling (called when a PocketUI screen closes). */
    public void disable() {
        this.enabled = false;
        this.scrollTarget = null;
        resetState();
    }

    /** Set the scrollable panel that the right stick should scroll. */
    public void setScrollTarget(DarkPanel panel) {
        this.scrollTarget = panel;
    }

    /** @return {@code true} if a gamepad is connected and active. */
    public boolean isActive() { return active; }

    /**
     * Trigger a haptic rumble on the connected gamepad (if supported).
     * <p>
     * Internally calls {@code glfwSetJoystickRumble} when available.
     * Falls back silently if rumble is not supported by the driver.
     *
     * @param strongMagnitude intensity of the low-frequency motor (0.0–1.0)
     * @param weakMagnitude   intensity of the high-frequency motor (0.0–1.0)
     * @param durationMs      rumble duration in milliseconds
     */
    public void rumble(float strongMagnitude, float weakMagnitude, int durationMs) {
        if (!active || connectedJoystick < 0) return;
        // GLFW 3.4+ added glfwSetJoystickRumble; older versions lack it.
        // We use reflection so the code compiles against any LWJGL version.
        try {
            java.lang.reflect.Method m = org.lwjgl.glfw.GLFW.class.getMethod(
                    "glfwSetJoystickRumble", int.class, float.class, float.class, int.class);
            m.invoke(null, connectedJoystick, strongMagnitude, weakMagnitude, durationMs);
        } catch (Throwable ignored) {
            // Not supported on this GLFW/LWJGL version — silently ignore.
        }
    }

    /**
     * Convenience: short 100 ms tap rumble.
     */
    public void rumbleTap() {
        rumble(0.3f, 0.2f, 100);
    }

    /**
     * Convenience: medium 200 ms rumble for confirmations.
     */
    public void rumbleConfirm() {
        rumble(0.5f, 0.3f, 200);
    }

    /**
     * Convenience: strong 400 ms rumble for errors/impacts.
     */
    public void rumbleError() {
        rumble(0.8f, 0.6f, 400);
    }

    /**
     * Simplified rumble with a single intensity value.
     * Maps the intensity equally to both the strong and weak motors.
     *
     * @param intensity  motor intensity (0.0–1.0)
     * @param durationMs rumble duration in milliseconds
     * @since 1.10.0
     */
    public void rumble(float intensity, int durationMs) {
        rumble(intensity, intensity * 0.7f, durationMs);
    }

    // =====================================================================
    //  Tick — called every END_CLIENT_TICK
    // =====================================================================

    /**
     * Poll the first connected gamepad and dispatch navigation, activation,
     * and scroll events to the PocketUI focus/component system.
     */
    public void tick() {
        if (!enabled) {
            active = false;
            return;
        }

        // ── Find the first connected gamepad ─────────────────────────────
        int joy = findGamepad();
        if (joy < 0) {
            active = false;
            return;
        }
        connectedJoystick = joy;

        // ── Read gamepad state ───────────────────────────────────────────
        try (GLFWGamepadState state = GLFWGamepadState.calloc()) {
            if (!GLFW.glfwGetGamepadState(joy, state)) {
                active = false;
                return;
            }
            active = true;

            ByteBuffer buttons = state.buttons();
            FloatBuffer axes   = state.axes();

            // ── Read inputs ──────────────────────────────────────────────
            boolean btnA      = buttons.get(BTN_A)      == GLFW.GLFW_PRESS;
            boolean btnB      = buttons.get(BTN_B)      == GLFW.GLFW_PRESS;
            boolean btnLB     = buttons.get(BTN_LB)     == GLFW.GLFW_PRESS;
            boolean btnRB     = buttons.get(BTN_RB)     == GLFW.GLFW_PRESS;
            boolean dpadUp    = buttons.get(BTN_DPAD_U) == GLFW.GLFW_PRESS;
            boolean dpadDown  = buttons.get(BTN_DPAD_D) == GLFW.GLFW_PRESS;
            boolean dpadLeft  = buttons.get(BTN_DPAD_L) == GLFW.GLFW_PRESS;
            boolean dpadRight = buttons.get(BTN_DPAD_R) == GLFW.GLFW_PRESS;

            float leftX  = axes.get(AXIS_LX);
            float leftY  = axes.get(AXIS_LY);
            float rightY = axes.get(AXIS_RY);

            FocusManager fm = FocusManager.getInstance();

            // ── A / Cross — activate focused ─────────────────────────────
            if (btnA && !prevA) {
                fm.activateFocused();
            }

            // ── B / Circle — close screen ────────────────────────────────
            if (btnB && !prevB) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen != null) {
                    client.currentScreen.close();
                }
            }

            // ── LB / RB — tab-cycle focus ────────────────────────────────
            if (btnRB && !prevRB) {
                UIComponent before = fm.getFocused();
                fm.navigateNext();
                snapCursorIfChanged(before, fm.getFocused());
            }
            if (btnLB && !prevLB) {
                UIComponent before = fm.getFocused();
                fm.navigatePrevious();
                snapCursorIfChanged(before, fm.getFocused());
            }

            // ── D-pad — directional navigation ──────────────────────────
            handleDpad(fm, dpadUp,    FocusManager.Direction.UP);
            handleDpad(fm, dpadDown,  FocusManager.Direction.DOWN);
            handleDpad(fm, dpadLeft,  FocusManager.Direction.LEFT);
            handleDpad(fm, dpadRight, FocusManager.Direction.RIGHT);

            // ── Left stick — directional navigation (with deadzone) ─────
            handleStickNavigation(fm, leftX, leftY);

            // ── Right stick Y — scroll ───────────────────────────────────
            handleScroll(rightY);

            // ── Store previous frame ─────────────────────────────────────
            prevA  = btnA;
            prevB  = btnB;
            prevLB = btnLB;
            prevRB = btnRB;
        }
    }

    // =====================================================================
    //  D-pad navigation with repeat
    // =====================================================================

    private void handleDpad(FocusManager fm, boolean pressed, FocusManager.Direction dir) {
        // Select the correct tick counter reference
        int ticks;
        switch (dir) {
            case UP    -> ticks = navTicksUp;
            case DOWN  -> ticks = navTicksDown;
            case LEFT  -> ticks = navTicksLeft;
            case RIGHT -> ticks = navTicksRight;
            default    -> ticks = 0;
        }

        if (pressed) {
            boolean navigate = false;
            if (ticks == 0) {
                // First press
                navigate = true;
            } else if (ticks >= NAV_REPEAT_DELAY &&
                       (ticks - NAV_REPEAT_DELAY) % NAV_REPEAT_RATE == 0) {
                // Repeat after delay
                navigate = true;
            }
            ticks++;

            if (navigate) {
                UIComponent before = fm.getFocused();
                fm.navigateDirection(dir);
                snapCursorIfChanged(before, fm.getFocused());
            }
        } else {
            ticks = 0;
        }

        // Write back tick counter
        switch (dir) {
            case UP    -> navTicksUp    = ticks;
            case DOWN  -> navTicksDown  = ticks;
            case LEFT  -> navTicksLeft  = ticks;
            case RIGHT -> navTicksRight = ticks;
        }
    }

    // =====================================================================
    //  Left stick directional navigation
    // =====================================================================

    private void handleStickNavigation(FocusManager fm, float lx, float ly) {
        // Determine dominant axis
        boolean xActive = Math.abs(lx) > STICK_DEADZONE;
        boolean yActive = Math.abs(ly) > STICK_DEADZONE;

        // X-axis navigation
        if (xActive && Math.abs(lx) >= Math.abs(ly)) {
            FocusManager.Direction dir = lx > 0
                    ? FocusManager.Direction.RIGHT
                    : FocusManager.Direction.LEFT;

            if (stickNavTicksX == 0) {
                UIComponent before = fm.getFocused();
                fm.navigateDirection(dir);
                snapCursorIfChanged(before, fm.getFocused());
            } else if (stickNavTicksX >= NAV_REPEAT_DELAY &&
                       (stickNavTicksX - NAV_REPEAT_DELAY) % NAV_REPEAT_RATE == 0) {
                UIComponent before = fm.getFocused();
                fm.navigateDirection(dir);
                snapCursorIfChanged(before, fm.getFocused());
            }
            stickNavTicksX++;
        } else {
            stickNavTicksX = 0;
        }

        // Y-axis navigation (GLFW: positive Y = down)
        if (yActive && Math.abs(ly) > Math.abs(lx)) {
            FocusManager.Direction dir = ly > 0
                    ? FocusManager.Direction.DOWN
                    : FocusManager.Direction.UP;

            if (stickNavTicksY == 0) {
                UIComponent before = fm.getFocused();
                fm.navigateDirection(dir);
                snapCursorIfChanged(before, fm.getFocused());
            } else if (stickNavTicksY >= NAV_REPEAT_DELAY &&
                       (stickNavTicksY - NAV_REPEAT_DELAY) % NAV_REPEAT_RATE == 0) {
                UIComponent before = fm.getFocused();
                fm.navigateDirection(dir);
                snapCursorIfChanged(before, fm.getFocused());
            }
            stickNavTicksY++;
        } else {
            stickNavTicksY = 0;
        }
    }

    // =====================================================================
    //  Right stick scrolling
    // =====================================================================

    private void handleScroll(float ry) {
        // Auto-detect scroll target if not manually set
        if (scrollTarget == null) {
            scrollTarget = findScrollablePanel();
        }
        if (scrollTarget == null) return;
        if (Math.abs(ry) <= STICK_DEADZONE) return;

        // Map stick deflection to scroll amount (positive ry = scroll down)
        double amount = -ry * SCROLL_SPEED;
        scrollTarget.scrollBy(amount);
    }

    /**
     * Walk up the parent chain from the currently focused component to
     * find the nearest scrollable {@link DarkPanel}.  This allows the
     * right-stick scroll to work automatically without a manual
     * {@link #setScrollTarget(DarkPanel)} call.
     *
     * @return the nearest scrollable DarkPanel, or {@code null}
     */
    private DarkPanel findScrollablePanel() {
        UIComponent focused = FocusManager.getInstance().getFocused();
        if (focused == null) return null;

        UIComponent current = focused;
        while (current != null) {
            if (current instanceof DarkPanel dp && dp.isScrollable()) {
                return dp;
            }
            current = current.getParent();
        }
        return null;
    }

    // =====================================================================
    //  Virtual cursor snapping
    // =====================================================================

    /**
     * If focus changed, warp the OS cursor to the centre of the newly
     * focused component. This activates hover effects and tooltips
     * naturally without manual mouse movement.
     */
    private void snapCursorIfChanged(UIComponent before, UIComponent after) {
        if (after == null || after == before) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return;

        // Component coords are in GUI-scaled pixels; GLFW cursor is in
        // window pixels, so we must multiply by the GUI scale factor.
        double scale = client.getWindow().getScaleFactor();
        double cx = (after.getX() + after.getWidth()  / 2.0) * scale;
        double cy = (after.getY() + after.getHeight() / 2.0) * scale;

        GLFW.glfwSetCursorPos(client.getWindow().getHandle(), cx, cy);
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    /** Find the first connected GLFW joystick that reports as a gamepad. */
    private int findGamepad() {
        for (int j = GLFW.GLFW_JOYSTICK_1; j <= GLFW.GLFW_JOYSTICK_LAST; j++) {
            if (GLFW.glfwJoystickPresent(j) && GLFW.glfwJoystickIsGamepad(j)) {
                return j;
            }
        }
        return -1;
    }

    /** Reset all internal tracking state (press memory + repeat timers). */
    private void resetState() {
        prevA = prevB = prevLB = prevRB = false;
        navTicksUp = navTicksDown = navTicksLeft = navTicksRight = 0;
        stickNavTicksX = stickNavTicksY = 0;
    }
}
