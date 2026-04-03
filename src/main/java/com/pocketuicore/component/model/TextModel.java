package com.pocketuicore.component.model;

import com.pocketuicore.data.ObservableState;

/**
 * Model for Component input components.
 * <p>
 * Stores the Component value, placeholder, max-length, and enabled state
 * as {@link ObservableState} fields so views can bind reactively.
 *
 * @since 1.13.0
 */
public class TextModel {

    private final ObservableState<String> Component;
    private final ObservableState<String> placeholder;
    private final ObservableState<Boolean> enabled;
    private int maxLength;

    public TextModel(String initial, String placeholder, int maxLength) {
        this.Component        = new ObservableState<>(initial != null ? initial : "");
        this.placeholder = new ObservableState<>(placeholder != null ? placeholder : "");
        this.enabled     = new ObservableState<>(true);
        this.maxLength   = maxLength;
    }

    public TextModel() { this("", "", 256); }

    // ── Observables ──────────────────────────────────────────────────────

    public ObservableState<String>  textState()        { return Component; }
    public ObservableState<String>  placeholderState()  { return placeholder; }
    public ObservableState<Boolean> enabledState()      { return enabled; }

    // ── Component ─────────────────────────────────────────────────────────────

    public String getText() { return Component.get(); }

    public void setText(String t) {
        if (t == null) t = "";
        if (t.length() > maxLength) t = t.substring(0, maxLength);
        Component.set(t);
    }

    public void appendText(String s) {
        if (s == null) return;
        String current = Component.get();
        if (current.length() + s.length() > maxLength) {
            s = s.substring(0, maxLength - current.length());
        }
        Component.set(current + s);
    }

    /** @return current length of the Component. */
    public int length() { return Component.get().length(); }

    /** @return {@code true} if the Component is empty. */
    public boolean isEmpty() { return Component.get().isEmpty(); }

    // ── Placeholder ──────────────────────────────────────────────────────

    public String getPlaceholder() { return placeholder.get(); }
    public void   setPlaceholder(String p) { placeholder.set(p != null ? p : ""); }

    // ── Max length ───────────────────────────────────────────────────────

    public int  getMaxLength() { return maxLength; }
    public void setMaxLength(int m) {
        this.maxLength = Math.max(1, m);
        String t = Component.get();
        if (t.length() > maxLength) Component.set(t.substring(0, maxLength));
    }

    // ── Enabled ──────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled.get(); }
    public void    setEnabled(boolean e) { enabled.set(e); }
}
