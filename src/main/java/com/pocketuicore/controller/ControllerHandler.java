package com.pocketuicore.controller;

import com.pocketuicore.component.DarkPanel;
import com.pocketuicore.component.FocusManager;
import com.pocketuicore.component.UIComponent;
import com.pocketuicore.input.InputHelper;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;

/**
 * Controller Handler — SDL Gamepad Polling
 * <p>
 * Provides native controller support by polling connected gamepads
 * via SDL3 (see {@link SdlGamepadBackend}) every client tick. Routes directional input to
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

    // ── State ────────────────────────────────────────────────────────────
    private boolean active = false;
    private GamepadBackend backend;                 // created lazily on first enabled tick
    private final GamepadNavigator navigator = new GamepadNavigator();
    private final Actions actions = new Actions();

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
     * Internally calls {@code SDL_RumbleGamepad} when available.
     * Falls back silently if rumble is not supported by the driver.
     *
     * @param strongMagnitude intensity of the low-frequency motor (0.0–1.0)
     * @param weakMagnitude   intensity of the high-frequency motor (0.0–1.0)
     * @param durationMs      rumble duration in milliseconds
     */
    public void rumble(float strongMagnitude, float weakMagnitude, int durationMs) {
        if (!active || backend == null) return;
        backend.rumble(strongMagnitude, weakMagnitude, durationMs);
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
        if (backend == null) backend = new SdlGamepadBackend();   // never throws

        backend.update();
        active = navigator.tick(backend, actions);
        if (active) InputHelper.markController();
    }

    /** Replace the gamepad backend (tests / alternative input sources). */
    public void setBackend(GamepadBackend replacement) {
        if (backend != null && backend != replacement) backend.close();
        this.backend = replacement;
        resetState();
    }

    // =====================================================================
    //  Action sink -> FocusManager / Minecraft
    // =====================================================================

    private final class Actions implements GamepadActions {
        @Override public void activate() { FocusManager.getInstance().activateFocused(); }

        @Override public void close() {
            Minecraft client = Minecraft.getInstance();
            if (client.gui.screen() != null) client.gui.screen().onClose();
        }

        @Override public void focusNext() {
            FocusManager fm = FocusManager.getInstance();
            UIComponent before = fm.getFocused();
            fm.navigateNext();
            snapCursorIfChanged(before, fm.getFocused());
        }

        @Override public void focusPrevious() {
            FocusManager fm = FocusManager.getInstance();
            UIComponent before = fm.getFocused();
            fm.navigatePrevious();
            snapCursorIfChanged(before, fm.getFocused());
        }

        @Override public void navigate(NavDirection direction) {
            FocusManager fm = FocusManager.getInstance();
            UIComponent before = fm.getFocused();
            fm.navigateDirection(switch (direction) {
                case UP    -> FocusManager.Direction.UP;
                case DOWN  -> FocusManager.Direction.DOWN;
                case LEFT  -> FocusManager.Direction.LEFT;
                case RIGHT -> FocusManager.Direction.RIGHT;
            });
            snapCursorIfChanged(before, fm.getFocused());
        }

        @Override public void scroll(double amount) {
            // Auto-detect scroll target if not manually set
            if (scrollTarget == null) scrollTarget = findScrollablePanel();
            if (scrollTarget != null) scrollTarget.scrollBy(amount);
        }
    }

    /**
     * Walk up the parent chain from the currently focused component to
     * find the nearest scrollable {@link DarkPanel}.
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

        try {
            Minecraft client = Minecraft.getInstance();
            if (client.getWindow() == null) return;

            // Component coords are in GUI-scaled pixels; the cursor is in
            // window coordinates, so multiply by the GUI scale factor.
            double scale = client.getWindow().getGuiScale();
            double cx = (after.getX() + after.getWidth()  / 2.0) * scale;
            double cy = (after.getY() + after.getHeight() / 2.0) * scale;

            SDLMouse.SDL_WarpMouseInWindow(client.getWindow().handle(), (float) cx, (float) cy);
        } catch (Throwable ignored) {
            // cursor warp is cosmetic
        }
    }

    /** Reset all internal tracking state (press memory + repeat timers). */
    private void resetState() {
        navigator.reset();
    }
}
