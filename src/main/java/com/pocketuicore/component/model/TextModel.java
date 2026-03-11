package com.pocketuicore.component.model;

import com.pocketuicore.data.ObservableState;

/**
 * Model for text input components.
 * <p>
 * Stores the text value, placeholder, max-length, and enabled state
 * as {@link ObservableState} fields so views can bind reactively.
 *
 * @since 1.13.0
 */
public class TextModel {

    private final ObservableState<String> text;
    private final ObservableState<String> placeholder;
    private final ObservableState<Boolean> enabled;
    private int maxLength;

    public TextModel(String initial, String placeholder, int maxLength) {
        this.text        = new ObservableState<>(initial != null ? initial : "");
        this.placeholder = new ObservableState<>(placeholder != null ? placeholder : "");
        this.enabled     = new ObservableState<>(true);
        this.maxLength   = maxLength;
    }

    public TextModel() { this("", "", 256); }

    // ── Observables ──────────────────────────────────────────────────────

    public ObservableState<String>  textState()        { return text; }
    public ObservableState<String>  placeholderState()  { return placeholder; }
    public ObservableState<Boolean> enabledState()      { return enabled; }

    // ── Text ─────────────────────────────────────────────────────────────

    public String getText() { return text.get(); }

    public void setText(String t) {
        if (t == null) t = "";
        if (t.length() > maxLength) t = t.substring(0, maxLength);
        text.set(t);
    }

    public void appendText(String s) {
        if (s == null) return;
        String current = text.get();
        if (current.length() + s.length() > maxLength) {
            s = s.substring(0, maxLength - current.length());
        }
        text.set(current + s);
    }

    /** @return current length of the text. */
    public int length() { return text.get().length(); }

    /** @return {@code true} if the text is empty. */
    public boolean isEmpty() { return text.get().isEmpty(); }

    // ── Placeholder ──────────────────────────────────────────────────────

    public String getPlaceholder() { return placeholder.get(); }
    public void   setPlaceholder(String p) { placeholder.set(p != null ? p : ""); }

    // ── Max length ───────────────────────────────────────────────────────

    public int  getMaxLength() { return maxLength; }
    public void setMaxLength(int m) {
        this.maxLength = Math.max(1, m);
        String t = text.get();
        if (t.length() > maxLength) text.set(t.substring(0, maxLength));
    }

    // ── Enabled ──────────────────────────────────────────────────────────

    public boolean isEnabled() { return enabled.get(); }
    public void    setEnabled(boolean e) { enabled.set(e); }
}
