package com.pocketuicore.component.model;

import com.pocketuicore.data.ObservableState;

/**
 * Model for slider/progress-bar components.
 * <p>
 * Stores a normalised value (0.0–1.0), min/max labels, and enabled state.
 * Bind to a slider view for automatic two-way reactive updates.
 *
 * @since 1.13.0
 */
public class SliderModel {

    private final ObservableState<Float> value;
    private final ObservableState<Boolean> enabled;
    private float min;
    private float max;

    public SliderModel(float min, float max, float initial) {
        this.min     = min;
        this.max     = max;
        this.value   = new ObservableState<>(clamp(initial));
        this.enabled = new ObservableState<>(true);
    }

    // ── Observables ──────────────────────────────────────────────────────

    public ObservableState<Float>   valueState()   { return value; }
    public ObservableState<Boolean> enabledState() { return enabled; }

    // ── Value ────────────────────────────────────────────────────────────

    /** @return current value in [min, max]. */
    public float getValue() { return value.get(); }

    /** Set value, clamped to [min, max]. */
    public void setValue(float v) { value.set(clamp(v)); }

    /** @return normalised position in [0, 1]. */
    public float getNormalized() {
        return (max == min) ? 0f : (value.get() - min) / (max - min);
    }

    /** Set value from a normalised [0, 1] position. */
    public void setNormalized(float t) {
        setValue(min + t * (max - min));
    }

    // ── Range ────────────────────────────────────────────────────────────

    public float getMin() { return min; }
    public float getMax() { return max; }
    public void  setRange(float min, float max) {
        this.min = min;
        this.max = max;
        value.set(clamp(value.get()));
    }

    // ── Enabled ──────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled.get(); }
    public void    setEnabled(boolean e) { enabled.set(e); }

    private float clamp(float v) {
        return Math.max(min, Math.min(max, v));
    }
}
