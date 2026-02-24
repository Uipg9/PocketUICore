package com.pocketuicore.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Focus Manager — Controller &amp; Keyboard Navigation
 * <p>
 * Manages directional focus traversal across {@link UIComponent} trees,
 * enabling D-pad / thumbstick / Tab-key navigation for controller and
 * keyboard users.
 * <p>
 * Components register themselves as focusable. The FocusManager tracks
 * the currently focused component and provides spatial navigation
 * (nearest neighbour in the requested direction) as well as linear
 * next/previous cycling.
 * <p>
 * <b>Controller integration:</b> This manager provides the focus graph;
 * input routing from a controller mod such as Controlify should call
 * {@link #navigateDirection(Direction)} and {@link #activateFocused()}.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     FocusManager fm = FocusManager.getInstance();
 *     fm.register(myButton);
 *     fm.register(myOtherButton);
 *     fm.focusFirst();
 *
 *     // On D-pad input:
 *     fm.navigateDirection(FocusManager.Direction.DOWN);
 *     // On A / Enter:
 *     fm.activateFocused();
 * }</pre>
 */
public final class FocusManager {

    // ── Singleton ────────────────────────────────────────────────────────
    private static final FocusManager INSTANCE = new FocusManager();

    public static FocusManager getInstance() { return INSTANCE; }

    private FocusManager() { }

    /** Navigation direction for spatial focus traversal. */
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    // ── State ────────────────────────────────────────────────────────────
    private final List<UIComponent> focusables = new ArrayList<>();
    private UIComponent focused;

    // =====================================================================
    //  Registration
    // =====================================================================

    /** Register a component as focusable. Duplicates are ignored. */
    public void register(UIComponent component) {
        if (component != null && !focusables.contains(component)) {
            focusables.add(component);
        }
    }

    /** Unregister a component. If it was focused, focus is cleared. */
    public void unregister(UIComponent component) {
        focusables.remove(component);
        if (focused == component) focused = null;
    }

    /** Clear all registrations and the focused component. */
    public void clear() {
        focusables.clear();
        focused = null;
    }

    // =====================================================================
    //  Focus control
    // =====================================================================

    /** Set focus to a specific component (should already be registered). */
    public void focus(UIComponent component) {
        this.focused = component;
    }

    /** Clear focus (nothing focused). */
    public void clearFocus() {
        this.focused = null;
    }

    /** Focus the first registered visible + enabled component. */
    public void focusFirst() {
        for (UIComponent c : focusables) {
            if (c.isVisible() && c.isEnabled()) {
                focused = c;
                return;
            }
        }
        focused = null;
    }

    /** @return the currently focused component, or {@code null}. */
    public UIComponent getFocused() { return focused; }

    /** @return {@code true} if the given component is currently focused. */
    public boolean isFocused(UIComponent component) {
        return focused != null && focused == component;
    }

    /** @return {@code true} if any component is currently focused. */
    public boolean hasFocus() { return focused != null; }

    // =====================================================================
    //  Linear navigation (Tab / Shift+Tab / bumpers)
    // =====================================================================

    /**
     * Move focus to the next visible + enabled component in
     * registration order, wrapping around at the end.
     */
    public void navigateNext() {
        if (focusables.isEmpty()) return;
        int start = focused != null ? focusables.indexOf(focused) : -1;
        for (int i = 1; i <= focusables.size(); i++) {
            int idx = (start + i) % focusables.size();
            UIComponent c = focusables.get(idx);
            if (c.isVisible() && c.isEnabled()) {
                focused = c;
                return;
            }
        }
    }

    /**
     * Move focus to the previous visible + enabled component in
     * registration order, wrapping around at the beginning.
     */
    public void navigatePrevious() {
        if (focusables.isEmpty()) return;
        int start = focused != null ? focusables.indexOf(focused) : focusables.size();
        for (int i = 1; i <= focusables.size(); i++) {
            int idx = (start - i + focusables.size()) % focusables.size();
            UIComponent c = focusables.get(idx);
            if (c.isVisible() && c.isEnabled()) {
                focused = c;
                return;
            }
        }
    }

    // =====================================================================
    //  Spatial / directional navigation (D-pad / thumbstick)
    // =====================================================================

    /**
     * Move focus to the nearest visible + enabled component in the given
     * direction from the currently focused component.
     * <p>
     * Uses centre-point distance with a directional half-plane filter.
     * If no component is focused, falls back to {@link #focusFirst()}.
     */
    public void navigateDirection(Direction dir) {
        if (focused == null) {
            focusFirst();
            return;
        }

        int cx = focused.getX() + focused.getWidth() / 2;
        int cy = focused.getY() + focused.getHeight() / 2;

        UIComponent best = null;
        double bestDist = Double.MAX_VALUE;

        for (UIComponent c : focusables) {
            if (c == focused || !c.isVisible() || !c.isEnabled()) continue;

            int tx = c.getX() + c.getWidth() / 2;
            int ty = c.getY() + c.getHeight() / 2;
            int dx = tx - cx;
            int dy = ty - cy;

            // Direction filter — target must be in the correct half-plane
            boolean valid = switch (dir) {
                case UP    -> dy < 0 && Math.abs(dy) >= Math.abs(dx);
                case DOWN  -> dy > 0 && Math.abs(dy) >= Math.abs(dx);
                case LEFT  -> dx < 0 && Math.abs(dx) >= Math.abs(dy);
                case RIGHT -> dx > 0 && Math.abs(dx) >= Math.abs(dy);
            };
            if (!valid) continue;

            double dist = Math.sqrt((double) dx * dx + (double) dy * dy);
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }

        if (best != null) focused = best;
    }

    // =====================================================================
    //  Activation
    // =====================================================================

    /**
     * Simulate a click on the focused component (controller A / Enter).
     * Dispatches {@link UIComponent#mouseClicked} then
     * {@link UIComponent#mouseReleased} at the component's centre.
     *
     * @return {@code true} if the event was dispatched
     */
    public boolean activateFocused() {
        if (focused == null || !focused.isVisible() || !focused.isEnabled()) return false;
        double cx = focused.getX() + focused.getWidth() / 2.0;
        double cy = focused.getY() + focused.getHeight() / 2.0;
        focused.mouseClicked(cx, cy, 0);
        focused.mouseReleased(cx, cy, 0);
        return true;
    }

    // =====================================================================
    //  Query
    // =====================================================================

    /** @return number of registered focusable components. */
    public int getFocusableCount() { return focusables.size(); }
}
