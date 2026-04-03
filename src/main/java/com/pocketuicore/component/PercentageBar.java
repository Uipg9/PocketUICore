package com.pocketuicore.component;

import com.pocketuicore.render.RenderUtils;
import com.pocketuicore.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pocketuicore.render.RenderContext;

/**
 * Module 2 — PercentageBar
 * <p>
 * A smoothly-animated horizontal progress bar. Feed it a target progress
 * value (0.0 – 1.0) and the displayed fill will ease toward that target
 * each frame, creating a satisfying "growing" effect.
 * <p>
 * Rendering layers (bottom → top):
 * <ol>
 *   <li>Background track (rounded rect, dark)</li>
 *   <li>Fill bar (rounded rect, coloured, clipped to current %)</li>
 *   <li>Label Component (optional, centred)</li>
 * </ol>
 */
public class PercentageBar extends UIComponent {

    // ── Data ─────────────────────────────────────────────────────────────
    private float targetProgress;   // what we're heading toward (0.0–1.0)
    private float displayProgress;  // currently displayed (smoothly interpolated)
    private float easingSpeed = 6f; // higher = faster convergence

    // ── Appearance ───────────────────────────────────────────────────────
    private static final int THEME_DEFAULT = Integer.MIN_VALUE;
    private int trackColor;
    private int barColor;
    private int textColor;
    private int cornerRadius;
    private String label;           // nullable — if set, drawn centred on the bar
    private boolean showPercentage; // if true, shows "XX %" even when label is null

    // ── v1.9 — Enhanced label formatting ─────────────────────────────────

    /** Label display format. */
    public enum LabelFormat {
        /** Show "75%" */
        PERCENT,
        /** Show "75 / 100" (requires max to be set) */
        FRACTION,
        /** Use a custom formatter callback */
        CUSTOM,
        /** No label */
        NONE
    }

    private LabelFormat labelFormat = LabelFormat.PERCENT;
    /** Max value for FRACTION display. */
    private int maxValue = 100;
    /** Current value for FRACTION display. */
    private int currentValue = 0;
    /** Custom label formatter callback. */
    private LabelFormatter customFormatter;

    /** Whether to use health-style gradient colours automatically. */
    private boolean useGradientColors = false;
    /** Whether to show a pulse/glow effect at 100%. */
    private boolean pulseAtFull = false;

    /** Functional interface for custom label formatting. */
    @FunctionalInterface
    public interface LabelFormatter {
        String format(float progress, int current, int max);
    }

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create a PercentageBar with default dark-mode styling.
     *
     * @param initialProgress starting progress (0.0–1.0)
     */
    public PercentageBar(int x, int y, int width, int height, float initialProgress) {
        this(x, y, width, height, initialProgress,
             THEME_DEFAULT, THEME_DEFAULT, THEME_DEFAULT,
             height / 2,
             null, true);
    }

    /**
     * Full constructor.
     */
    public PercentageBar(int x, int y, int width, int height,
                         float initialProgress,
                         int trackColor, int barColor, int textColor,
                         int cornerRadius, String label, boolean showPercentage) {
        super(x, y, width, height);
        this.targetProgress  = clamp01(initialProgress);
        this.displayProgress = this.targetProgress;
        this.trackColor      = trackColor;
        this.barColor        = barColor;
        this.textColor       = textColor;
        this.cornerRadius    = cornerRadius;
        this.label           = label;
        this.showPercentage  = showPercentage;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(RenderContext ctx, int mouseX, int mouseY, float delta) {
        // ── Ease display toward target ───────────────────────────────────
        float diff = targetProgress - displayProgress;
        if (Math.abs(diff) < 0.001f) {
            displayProgress = targetProgress;
        } else {
            displayProgress += diff * Math.min(easingSpeed * delta, 1f);
        }

        // ── Auto gradient colours ────────────────────────────────────────
        if (useGradientColors) {
            applyHealthColors();
        }

        // ── Track (background) ───────────────────────────────────────────
        int resolvedTrack = trackColor == THEME_DEFAULT ? Theme.current().bgPrimary()   : trackColor;
        int resolvedBar   = barColor   == THEME_DEFAULT ? Theme.current().accentTeal()  : barColor;
        int resolvedText  = textColor  == THEME_DEFAULT ? Theme.current().textPrimary() : textColor;
        RenderUtils.fillRoundedRect(ctx, x, y, width, height, cornerRadius, resolvedTrack);

        // ── Fill bar ─────────────────────────────────────────────────────
        int fillW = Math.round(width * displayProgress);
        if (fillW > 0) {
            // Clamp corner radius so the tiny initial bar doesn't look broken
            int barRadius = Math.min(cornerRadius, fillW / 2);
            // Use scissor to ensure the bar doesn't bleed past the track bounds
            ctx.enableScissor(x, y, x + width, y + height);

            int renderColor = resolvedBar;
            // Gradient fill: red → yellow → green based on progress
            if (useGradientColors) {
                renderColor = getGradientColor(displayProgress);
            }

            RenderUtils.fillRoundedRect(ctx, x, y, fillW, height, barRadius, renderColor);
            ctx.disableScissor();
        }

        // ── Pulse / glow at 100% ────────────────────────────────────────
        if (pulseAtFull && displayProgress >= 0.999f) {
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int glowAlpha = (int) (pulse * 80);
            int glowColor = RenderUtils.withAlpha(0xFFFFFFFF, glowAlpha);
            RenderUtils.fillRoundedRect(ctx, x, y, width, height, cornerRadius, glowColor);
            // Bright border pulse
            int borderAlpha = (int) (pulse * 160);
            int borderColor = RenderUtils.withAlpha(RenderUtils.COL_SUCCESS, borderAlpha);
            RenderUtils.drawRoundedBorder(ctx, x, y, width, height, cornerRadius, borderColor);
        }

        // ── Label / percentage Component ──────────────────────────────────────
        String displayText = resolveDisplayText();

        if (displayText != null) {
            Minecraft client = Minecraft.getInstance();
            Font tr = client.font;
            int textY = y + (height - tr.lineHeight) / 2;
            RenderUtils.drawCenteredText(ctx, tr, displayText,
                    x + width / 2, textY, resolvedText);
        }
    }

    /**
     * Resolve the label Component based on the current format settings.
     */
    private String resolveDisplayText() {
        // Explicit label always takes priority
        if (label != null) return label;

        return switch (labelFormat) {
            case PERCENT -> Math.round(displayProgress * 100) + "%";
            case FRACTION -> currentValue + " / " + maxValue;
            case CUSTOM -> customFormatter != null
                    ? customFormatter.format(displayProgress, currentValue, maxValue)
                    : null;
            case NONE -> null;
        };
    }

    /**
     * Get a gradient color based on progress (red → yellow → green).
     */
    private int getGradientColor(float progress) {
        if (progress < 0.5f) {
            // Red → Yellow (0% → 50%)
            return RenderUtils.lerpColor(
                    RenderUtils.COL_ERROR,
                    RenderUtils.COL_WARNING,
                    progress * 2f);
        } else {
            // Yellow → Green (50% → 100%)
            return RenderUtils.lerpColor(
                    RenderUtils.COL_WARNING,
                    RenderUtils.COL_SUCCESS,
                    (progress - 0.5f) * 2f);
        }
    }

    // =====================================================================
    //  API
    // =====================================================================

    /**
     * Set the target progress. The bar will smoothly animate toward it.
     *
     * @param progress 0.0 (empty) to 1.0 (full)
     */
    public PercentageBar setProgress(float progress) {
        this.targetProgress = clamp01(progress);
        return this;
    }

    /**
     * Snap directly to a value (no easing). Useful for initial load.
     */
    public void snapTo(float progress) {
        this.targetProgress  = clamp01(progress);
        this.displayProgress = this.targetProgress;
    }

    public float getTargetProgress()  { return targetProgress; }
    public float getDisplayProgress() { return displayProgress; }

    public PercentageBar setBarColor(int c)        { this.barColor = c; return this; }
    public int  getBarColor()              { return barColor; }
    public PercentageBar setTrackColor(int c)      { this.trackColor = c; return this; }
    public int  getTrackColor()            { return trackColor; }
    public PercentageBar setTextColor(int c)       { this.textColor = c; return this; }
    public int  getTextColor()             { return textColor; }
    public PercentageBar setCornerRadius(int r)    { this.cornerRadius = r; return this; }
    public int  getCornerRadius()          { return cornerRadius; }
    /** @deprecated Use {@link #setText(String)} instead. */
    @Deprecated
    public PercentageBar setLabel(String l)        { this.label = l; return this; }
    /** Primary label setter — consistent with TextLabel. @since 1.12.0 */
    public PercentageBar setText(String Component)      { this.label = Component; return this; }
    public String getLabel()               { return label; }
    public PercentageBar setShowPercentage(boolean b) { this.showPercentage = b; return this; }
    public boolean isShowPercentage()      { return showPercentage; }
    public PercentageBar setEasingSpeed(float s)   { this.easingSpeed = s; return this; }
    public float getEasingSpeed()          { return easingSpeed; }

    // ── v1.9 — Enhanced label & gradient accessors ───────────────────────

    /**
     * Set the label display format.
     *
     * @param format the label format mode
     */
    public PercentageBar setLabelFormat(LabelFormat format) {
        this.labelFormat = format;
        // Backwards compat: if switching to PERCENT, enable showPercentage
        this.showPercentage = (format == LabelFormat.PERCENT);
        return this;
    }

    public LabelFormat getLabelFormat() { return labelFormat; }

    /**
     * Set the max value for {@link LabelFormat#FRACTION} display.
     */
    public PercentageBar setMaxValue(int max) { this.maxValue = max; return this; }
    public int getMaxValue() { return maxValue; }

    /**
     * Set the current value for {@link LabelFormat#FRACTION} display.
     * Also updates the target progress to {@code current / max}.
     */
    public PercentageBar setCurrentValue(int current) {
        this.currentValue = current;
        return this;
    }

    /**
     * Set current and max values together, updating progress automatically.
     *
     * @param current current value
     * @param max     maximum value
     */
    public PercentageBar setValues(int current, int max) {
        this.currentValue = current;
        this.maxValue = max;
        setProgress(max > 0 ? (float) current / max : 0f);
        return this;
    }

    public int getCurrentValue() { return currentValue; }

    /**
     * Set a custom label formatter for {@link LabelFormat#CUSTOM} mode.
     */
    public PercentageBar setCustomFormatter(LabelFormatter formatter) {
        this.customFormatter = formatter;
        this.labelFormat = LabelFormat.CUSTOM;
        return this;
    }

    /**
     * Enable automatic gradient colours (red → yellow → green) based
     * on the current fill level.
     */
    public PercentageBar setUseGradientColors(boolean use) { this.useGradientColors = use; return this; }
    public boolean isUseGradientColors() { return useGradientColors; }

    /**
     * Enable a pulsing glow effect when the bar reaches 100%.
     */
    public PercentageBar setPulseAtFull(boolean pulse) { this.pulseAtFull = pulse; return this; }
    public boolean isPulseAtFull() { return pulseAtFull; }

    // =====================================================================
    //  Utility
    // =====================================================================

    /** Colour the bar to reflect "health"-style thresholds. */
    public void applyHealthColors() {
        if (displayProgress > 0.5f)  barColor = RenderUtils.COL_SUCCESS;
        else if (displayProgress > 0.25f) barColor = RenderUtils.COL_WARNING;
        else barColor = RenderUtils.COL_ERROR;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
