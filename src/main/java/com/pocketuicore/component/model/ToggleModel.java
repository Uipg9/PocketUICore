package com.pocketuicore.component.model;

import com.pocketuicore.data.ObservableState;

/**
 * Model for toggle / checkbox components.
 * <p>
 * Wraps a boolean state as an {@link ObservableState} so views can
 * bind reactively.
 *
 * @since 1.13.0
 */
public class ToggleModel {

    private final ObservableState<Boolean> toggled;
    private final ObservableState<Boolean> enabled;
    private final ObservableState<String>  label;

    public ToggleModel(String label, boolean initial) {
        this.label   = new ObservableState<>(label);
        this.toggled = new ObservableState<>(initial);
        this.enabled = new ObservableState<>(true);
    }

    public ToggleModel(boolean initial) {
        this("", initial);
    }

    /**
     * No-arg constructor — creates a ToggleModel with empty label, initially off.
     *
     * @since 1.14.0
     */
    public ToggleModel() {
        this("", false);
    }

    // ── Observables ──────────────────────────────────────────────────────

    public ObservableState<Boolean> toggledState() { return toggled; }
    public ObservableState<Boolean> enabledState() { return enabled; }
    public ObservableState<String>  labelState()   { return label; }

    // ── Convenience ──────────────────────────────────────────────────────

    public boolean isToggled() { return toggled.get(); }
    public void    setToggled(boolean v) { toggled.set(v); }
    public void    toggle() { toggled.set(!toggled.get()); }

    public boolean isEnabled() { return enabled.get(); }
    public void    setEnabled(boolean e) { enabled.set(e); }

    public String getLabel() { return label.get(); }
    public void   setLabel(String l) { label.set(l); }
}
