package com.pocketuicore.component.model;

import com.pocketuicore.data.ObservableState;

/**
 * Model for button components — separates click/enable/label state from rendering.
 * <p>
 * Bind a {@code ButtonModel} to a button view and the view will react to
 * state changes automatically via {@link ObservableState}.
 *
 * @since 1.13.0
 */
public class ButtonModel {

    private final ObservableState<String> label;
    private final ObservableState<Boolean> enabled;
    private final ObservableState<Boolean> pressed;
    private Runnable action;

    public ButtonModel(String label) {
        this.label   = new ObservableState<>(label);
        this.enabled = new ObservableState<>(true);
        this.pressed = new ObservableState<>(false);
    }

    // ── Observables ──────────────────────────────────────────────────────

    public ObservableState<String>  labelState()   { return label; }
    public ObservableState<Boolean> enabledState() { return enabled; }
    public ObservableState<Boolean> pressedState() { return pressed; }

    // ── Convenience getters / setters ────────────────────────────────────

    public String  getLabel()   { return label.get(); }
    public void    setLabel(String l) { label.set(l); }

    public boolean isEnabled()  { return enabled.get(); }
    public void    setEnabled(boolean e) { enabled.set(e); }

    public boolean isPressed()  { return pressed.get(); }

    public Runnable getAction() { return action; }
    public void     setAction(Runnable action) { this.action = action; }

    /** Simulate a press — sets pressed, fires action, then releases. */
    public void press() {
        if (!isEnabled()) return;
        pressed.set(true);
        if (action != null) action.run();
        pressed.set(false);
    }
}
