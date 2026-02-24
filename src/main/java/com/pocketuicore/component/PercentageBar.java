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

        // ── Track (background) ───────────────────────────────────────────
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, trackColor);

        // ── Fill bar ─────────────────────────────────────────────────────
        int fillW = Math.round(width * displayProgress);
        if (fillW > 0) {
            // Clamp corner radius so the tiny initial bar doesn't look broken
            int barRadius = Math.min(cornerRadius, fillW / 2);
            // Use scissor to ensure the bar doesn't bleed past the track bounds
            ctx.enableScissor(x, y, x + width, y + height);
            ProceduralRenderer.fillRoundedRect(ctx, x, y, fillW, height, barRadius, barColor);
            ctx.disableScissor();
        }

        // ── Label / percentage text ──────────────────────────────────────
        String displayText = null;
        if (label != null) {
            displayText = label;
        } else if (showPercentage) {
            displayText = Math.round(displayProgress * 100) + "%";
        }

        if (displayText != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer tr = client.textRenderer;
            int textY = y + (height - tr.fontHeight) / 2;
            ProceduralRenderer.drawCenteredText(ctx, tr, displayText,
                    x + width / 2, textY, textColor);
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
    public void setTrackColor(int c)      { this.trackColor = c; }
    public void setTextColor(int c)       { this.textColor = c; }
    public void setCornerRadius(int r)    { this.cornerRadius = r; }
    public void setLabel(String l)        { this.label = l; }
    public void setShowPercentage(boolean b) { this.showPercentage = b; }
    public void setEasingSpeed(float s)   { this.easingSpeed = s; }

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
