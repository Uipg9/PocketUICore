package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

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
 *   <li>Label text (optional, centred)</li>
 * </ol>
 */
public class PercentageBar extends UIComponent {

    // ── Data ─────────────────────────────────────────────────────────────
    private float targetProgress;   // what we're heading toward (0.0–1.0)
    private float displayProgress;  // currently displayed (smoothly interpolated)
    private float easingSpeed = 6f; // higher = faster convergence

    // ── Appearance ───────────────────────────────────────────────────────
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
             ProceduralRenderer.COL_BG_PRIMARY,
             ProceduralRenderer.COL_ACCENT_TEAL,
             ProceduralRenderer.COL_TEXT_PRIMARY,
             height / 2, // fully rounded ends by default
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
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
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
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, trackColor);

        // ── Fill bar ─────────────────────────────────────────────────────
        int fillW = Math.round(width * displayProgress);
        if (fillW > 0) {
            // Clamp corner radius so the tiny initial bar doesn't look broken
            int barRadius = Math.min(cornerRadius, fillW / 2);
            // Use scissor to ensure the bar doesn't bleed past the track bounds
            ctx.enableScissor(x, y, x + width, y + height);

            int renderColor = barColor;
            // Gradient fill: red → yellow → green based on progress
            if (useGradientColors) {
                renderColor = getGradientColor(displayProgress);
            }

            ProceduralRenderer.fillRoundedRect(ctx, x, y, fillW, height, barRadius, renderColor);
            ctx.disableScissor();
        }

        // ── Pulse / glow at 100% ────────────────────────────────────────
        if (pulseAtFull && displayProgress >= 0.999f) {
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int glowAlpha = (int) (pulse * 80);
            int glowColor = ProceduralRenderer.withAlpha(0xFFFFFFFF, glowAlpha);
            ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, glowColor);
            // Bright border pulse
            int borderAlpha = (int) (pulse * 160);
            int borderColor = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_SUCCESS, borderAlpha);
            ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, cornerRadius, borderColor);
        }

        // ── Label / percentage text ──────────────────────────────────────
        String displayText = resolveDisplayText();

        if (displayText != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer tr = client.textRenderer;
            int textY = y + (height - tr.fontHeight) / 2;
            ProceduralRenderer.drawCenteredText(ctx, tr, displayText,
                    x + width / 2, textY, textColor);
        }
    }

    /**
     * Resolve the label text based on the current format settings.
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
            return ProceduralRenderer.lerpColor(
                    ProceduralRenderer.COL_ERROR,
                    ProceduralRenderer.COL_WARNING,
                    progress * 2f);
        } else {
            // Yellow → Green (50% → 100%)
            return ProceduralRenderer.lerpColor(
                    ProceduralRenderer.COL_WARNING,
                    ProceduralRenderer.COL_SUCCESS,
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
    public void setProgress(float progress) {
        this.targetProgress = clamp01(progress);
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

    public void setBarColor(int c)        { this.barColor = c; }
    public int  getBarColor()              { return barColor; }
    public void setTrackColor(int c)      { this.trackColor = c; }
    public int  getTrackColor()            { return trackColor; }
    public void setTextColor(int c)       { this.textColor = c; }
    public int  getTextColor()             { return textColor; }
    public void setCornerRadius(int r)    { this.cornerRadius = r; }
    public int  getCornerRadius()          { return cornerRadius; }
    public void setLabel(String l)        { this.label = l; }
    public String getLabel()               { return label; }
    public void setShowPercentage(boolean b) { this.showPercentage = b; }
    public boolean isShowPercentage()      { return showPercentage; }
    public void setEasingSpeed(float s)   { this.easingSpeed = s; }
    public float getEasingSpeed()          { return easingSpeed; }

    // ── v1.9 — Enhanced label & gradient accessors ───────────────────────

    /**
     * Set the label display format.
     *
     * @param format the label format mode
     */
    public void setLabelFormat(LabelFormat format) {
        this.labelFormat = format;
        // Backwards compat: if switching to PERCENT, enable showPercentage
        this.showPercentage = (format == LabelFormat.PERCENT);
    }

    public LabelFormat getLabelFormat() { return labelFormat; }

    /**
     * Set the max value for {@link LabelFormat#FRACTION} display.
     */
    public void setMaxValue(int max) { this.maxValue = max; }
    public int getMaxValue() { return maxValue; }

    /**
     * Set the current value for {@link LabelFormat#FRACTION} display.
     * Also updates the target progress to {@code current / max}.
     */
    public void setCurrentValue(int current) {
        this.currentValue = current;
    }

    /**
     * Set current and max values together, updating progress automatically.
     *
     * @param current current value
     * @param max     maximum value
     */
    public void setValues(int current, int max) {
        this.currentValue = current;
        this.maxValue = max;
        setProgress(max > 0 ? (float) current / max : 0f);
    }

    public int getCurrentValue() { return currentValue; }

    /**
     * Set a custom label formatter for {@link LabelFormat#CUSTOM} mode.
     */
    public void setCustomFormatter(LabelFormatter formatter) {
        this.customFormatter = formatter;
        this.labelFormat = LabelFormat.CUSTOM;
    }

    /**
     * Enable automatic gradient colours (red → yellow → green) based
     * on the current fill level.
     */
    public void setUseGradientColors(boolean use) { this.useGradientColors = use; }
    public boolean isUseGradientColors() { return useGradientColors; }

    /**
     * Enable a pulsing glow effect when the bar reaches 100%.
     */
    public void setPulseAtFull(boolean pulse) { this.pulseAtFull = pulse; }
    public boolean isPulseAtFull() { return pulseAtFull; }

    // =====================================================================
    //  Utility
    // =====================================================================

    /** Colour the bar to reflect "health"-style thresholds. */
    public void applyHealthColors() {
        if (displayProgress > 0.5f)  barColor = ProceduralRenderer.COL_SUCCESS;
        else if (displayProgress > 0.25f) barColor = ProceduralRenderer.COL_WARNING;
        else barColor = ProceduralRenderer.COL_ERROR;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
