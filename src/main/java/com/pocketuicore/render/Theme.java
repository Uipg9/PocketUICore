package com.pocketuicore.render;

/**
 * Theme — Runtime-switchable colour palette for PocketUICore.
 * <p>
 * Instead of referencing {@code ProceduralRenderer.COL_*} constants directly,
 * components can query {@code Theme.current()} for their colours. This enables
 * light mode, custom themes, and runtime palette switching.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // Switch to light mode:
 *     Theme.setCurrent(Theme.LIGHT);
 *
 *     // In a component's render:
 *     int bg = Theme.current().bgPrimary();
 *
 *     // Define a custom theme:
 *     Theme custom = Theme.builder()
 *         .bgPrimary(0xFF2D2D2D)
 *         .accent(0xFF61AFEF)
 *         .build();
 *     Theme.setCurrent(custom);
 * }</pre>
 *
 * @since 1.12.0
 */
public final class Theme {

    // ── Palette fields ───────────────────────────────────────────────────
    private final int bgPrimary;
    private final int bgSurface;
    private final int bgElevated;
    private final int accent;
    private final int accentTeal;
    private final int success;
    private final int warning;
    private final int error;
    private final int textPrimary;
    private final int textMuted;
    private final int border;
    private final int hover;
    private final int overlay;
    private final int shadowBase;
    private final String name;

    // ── Built-in themes ──────────────────────────────────────────────────

    /** The default dark-mode palette matching ProceduralRenderer constants. */
    public static final Theme DARK = new Theme(
            "Dark",
            0xFF1A1A2E, 0xFF16213E, 0xFF0F3460,
            0xFFE94560, 0xFF00D2FF,
            0xFF2ECC71, 0xFFF39C12, 0xFFE74C3C,
            0xFFEAEAEA, 0xFFA0A0B0,
            0xFF2A2A4A, 0xFF1F4068, 0xC0000000, 0x00000000
    );

    /** Light-mode palette for bright environments. */
    public static final Theme LIGHT = new Theme(
            "Light",
            0xFFF0F0F0, 0xFFFFFFFF, 0xFFE8E8E8,
            0xFFD63384, 0xFF0D6EFD,
            0xFF198754, 0xFFFFC107, 0xFFDC3545,
            0xFF212529, 0xFF6C757D,
            0xFFDEE2E6, 0xFFE9ECEF, 0x40000000, 0x00000000
    );

    // ── Current theme ────────────────────────────────────────────────────
    private static Theme current = DARK;

    /** @return the currently active theme. */
    public static Theme current() { return current; }

    /** Set the active theme. All components querying Theme.current() will pick up the change. */
    public static void setCurrent(Theme theme) {
        if (theme != null) current = theme;
    }

    /** Reset to the default dark theme. */
    public static void resetToDefault() { current = DARK; }

    // ── Construction ─────────────────────────────────────────────────────

    private Theme(String name,
                  int bgPrimary, int bgSurface, int bgElevated,
                  int accent, int accentTeal,
                  int success, int warning, int error,
                  int textPrimary, int textMuted,
                  int border, int hover, int overlay, int shadowBase) {
        this.name        = name;
        this.bgPrimary   = bgPrimary;
        this.bgSurface   = bgSurface;
        this.bgElevated  = bgElevated;
        this.accent      = accent;
        this.accentTeal  = accentTeal;
        this.success     = success;
        this.warning     = warning;
        this.error       = error;
        this.textPrimary = textPrimary;
        this.textMuted   = textMuted;
        this.border      = border;
        this.hover       = hover;
        this.overlay     = overlay;
        this.shadowBase  = shadowBase;
    }

    // ── Accessors ────────────────────────────────────────────────────────

    public String name()       { return name; }
    public int bgPrimary()     { return bgPrimary; }
    public int bgSurface()     { return bgSurface; }
    public int bgElevated()    { return bgElevated; }
    public int accent()        { return accent; }
    public int accentTeal()    { return accentTeal; }
    public int success()       { return success; }
    public int warning()       { return warning; }
    public int error()         { return error; }
    public int textPrimary()   { return textPrimary; }
    public int textMuted()     { return textMuted; }
    public int border()        { return border; }
    public int hover()         { return hover; }
    public int overlay()       { return overlay; }
    public int shadowBase()    { return shadowBase; }

    // ── Builder ──────────────────────────────────────────────────────────

    /** Create a new theme builder pre-populated with the dark theme colours. */
    public static Builder builder() { return new Builder(); }

    /** Create a new theme builder pre-populated with an existing theme's colours. */
    public static Builder builder(Theme base) { return new Builder(base); }

    public static final class Builder {
        private String name = "Custom";
        private int bgPrimary, bgSurface, bgElevated;
        private int accent, accentTeal;
        private int success, warning, error;
        private int textPrimary, textMuted;
        private int border, hover, overlay, shadowBase;

        Builder() { this(DARK); }

        Builder(Theme base) {
            this.bgPrimary   = base.bgPrimary;
            this.bgSurface   = base.bgSurface;
            this.bgElevated  = base.bgElevated;
            this.accent      = base.accent;
            this.accentTeal  = base.accentTeal;
            this.success     = base.success;
            this.warning     = base.warning;
            this.error       = base.error;
            this.textPrimary = base.textPrimary;
            this.textMuted   = base.textMuted;
            this.border      = base.border;
            this.hover       = base.hover;
            this.overlay     = base.overlay;
            this.shadowBase  = base.shadowBase;
        }

        public Builder name(String n)        { this.name = n; return this; }
        public Builder bgPrimary(int c)      { this.bgPrimary = c; return this; }
        public Builder bgSurface(int c)      { this.bgSurface = c; return this; }
        public Builder bgElevated(int c)     { this.bgElevated = c; return this; }
        public Builder accent(int c)         { this.accent = c; return this; }
        public Builder accentTeal(int c)     { this.accentTeal = c; return this; }
        public Builder success(int c)        { this.success = c; return this; }
        public Builder warning(int c)        { this.warning = c; return this; }
        public Builder error(int c)          { this.error = c; return this; }
        public Builder textPrimary(int c)    { this.textPrimary = c; return this; }
        public Builder textMuted(int c)      { this.textMuted = c; return this; }
        public Builder border(int c)         { this.border = c; return this; }
        public Builder hover(int c)          { this.hover = c; return this; }
        public Builder overlay(int c)        { this.overlay = c; return this; }
        public Builder shadowBase(int c)     { this.shadowBase = c; return this; }

        public Theme build() {
            return new Theme(name, bgPrimary, bgSurface, bgElevated,
                    accent, accentTeal, success, warning, error,
                    textPrimary, textMuted, border, hover, overlay, shadowBase);
        }
    }
}
