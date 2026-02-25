package com.pocketuicore.component;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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
 * input routing from the {@link com.pocketuicore.controller.ControllerHandler}
 * calls {@link #navigateDirection(Direction)} and {@link #activateFocused()}.
 * A {@link FocusChangeListener} can be registered to react when the
 * focused component changes (e.g. cursor snapping).
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

    /**
     * Listener notified when the focused component changes.
     * Useful for cursor snapping, sound effects, or visual updates.
     */
    @FunctionalInterface
    public interface FocusChangeListener {
        /**
         * @param previous the previously focused component (may be {@code null})
         * @param current  the newly focused component (may be {@code null})
         */
        void onFocusChanged(UIComponent previous, UIComponent current);
    }

    // ── Singleton ────────────────────────────────────────────────────────
    private static final FocusManager INSTANCE = new FocusManager();

    public static FocusManager getInstance() { return INSTANCE; }

    private FocusManager() { }

    /** Navigation direction for spatial focus traversal. */
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    // ── State ────────────────────────────────────────────────────────────
    private final List<UIComponent> focusables = new ArrayList<>();
    private UIComponent focused;
    private final List<FocusChangeListener> listeners = new ArrayList<>();

    // ── Context stacking (for sub-screens / dialogs) ────────────────────
    private final Deque<FocusContext> contextStack = new ArrayDeque<>();

    /**
     * Snapshot of a focus context (focusable list + focused element + listeners).
     */
    private static final class FocusContext {
        final String name;
        final List<UIComponent> focusables;
        final UIComponent focused;
        final List<FocusChangeListener> listeners;

        FocusContext(String name,
                     List<UIComponent> focusables,
                     UIComponent focused,
                     List<FocusChangeListener> listeners) {
            this.name       = name;
            this.focusables = new ArrayList<>(focusables);
            this.focused    = focused;
            this.listeners  = new ArrayList<>(listeners);
        }
    }

    // =====================================================================
    //  Focus change listeners
    // =====================================================================

    /** Add a listener that fires whenever the focused component changes. */
    public void addFocusChangeListener(FocusChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Remove a previously registered listener. */
    public void removeFocusChangeListener(FocusChangeListener listener) {
        listeners.remove(listener);
    }

    /** Remove all focus change listeners. */
    public void clearFocusChangeListeners() {
        listeners.clear();
    }

    /** Notify all listeners of a focus change. */
    private void fireFocusChanged(UIComponent previous, UIComponent current) {
        if (previous == current) return;
        for (FocusChangeListener l : listeners) {
            l.onFocusChanged(previous, current);
        }
    }

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
        if (focused == component) {
            UIComponent prev = focused;
            focused = null;
            fireFocusChanged(prev, null);
        }
    }

    /** Clear all registrations, listeners, and the focused component. */
    public void clear() {
        UIComponent prev = focused;
        focusables.clear();
        focused = null;
        clearFocusChangeListeners();
        if (prev != null) fireFocusChanged(prev, null);
    }

    // =====================================================================
    //  Focus control
    // =====================================================================

    /** Set focus to a specific component (should already be registered). */
    public void focus(UIComponent component) {
        UIComponent prev = this.focused;
        this.focused = component;
        fireFocusChanged(prev, component);
    }

    /** Clear focus (nothing focused). */
    public void clearFocus() {
        UIComponent prev = this.focused;
        this.focused = null;
        fireFocusChanged(prev, null);
    }

    /** Focus the first registered visible + enabled component. */
    public void focusFirst() {
        UIComponent prev = focused;
        for (UIComponent c : focusables) {
            if (c.isVisible() && c.isEnabled()) {
                focused = c;
                fireFocusChanged(prev, c);
                return;
            }
        }
        focused = null;
        fireFocusChanged(prev, null);
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
        UIComponent prev = focused;
        int start = focused != null ? focusables.indexOf(focused) : -1;
        for (int i = 1; i <= focusables.size(); i++) {
            int idx = (start + i) % focusables.size();
            UIComponent c = focusables.get(idx);
            if (c.isVisible() && c.isEnabled()) {
                focused = c;
                fireFocusChanged(prev, c);
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
        UIComponent prev = focused;
        int start = focused != null ? focusables.indexOf(focused) : focusables.size();
        for (int i = 1; i <= focusables.size(); i++) {
            int idx = (start - i + focusables.size()) % focusables.size();
            UIComponent c = focusables.get(idx);
            if (c.isVisible() && c.isEnabled()) {
                focused = c;
                fireFocusChanged(prev, c);
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

        if (best != null) {
            UIComponent prev = focused;
            focused = best;
            fireFocusChanged(prev, best);
        }
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
    //  Context stacking
    // =====================================================================

    /**
     * Push the current focus state onto the stack and start a fresh
     * empty context. Useful when opening a dialog or sub-screen so
     * the dialog gets its own focusable set.
     *
     * @param name descriptive name for debugging (e.g. "prestige-dialog")
     */
    public void pushContext(String name) {
        contextStack.push(new FocusContext(
                name, focusables, focused, listeners));

        // Start fresh
        focusables.clear();
        listeners.clear();
        UIComponent prev = focused;
        focused = null;
        // Don't fire — old listeners are saved, fresh context has none
    }

    /**
     * Pop the most-recently-pushed context, restoring the previous
     * focusable set, focused element, and listeners.
     *
     * @return the name of the popped context, or {@code null} if the
     *         stack was empty
     */
    public String popContext() {
        if (contextStack.isEmpty()) return null;

        FocusContext ctx = contextStack.pop();

        // Restore
        focusables.clear();
        focusables.addAll(ctx.focusables);
        listeners.clear();
        listeners.addAll(ctx.listeners);
        UIComponent prev = focused;
        focused = ctx.focused;
        fireFocusChanged(prev, focused);

        return ctx.name;
    }

    /**
     * @return the number of saved contexts on the stack.
     */
    public int getContextDepth() {
        return contextStack.size();
    }

    // =====================================================================
    //  Query
    // =====================================================================

    /** @return number of registered focusable components. */
    public int getFocusableCount() { return focusables.size(); }
}
