package com.pocketuicore.screen;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Screen Tint Manager — Animated full-screen colour overlays with
 * smooth transitions, pulse/flash effects, and layer stacking.
 * <p>
 * Integrates with the existing rendering pipeline — call
 * {@link #render(DrawContext, int, int)} at the start of your screen's
 * render method (before UI components) to tint the background.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     ScreenTintManager tint = new ScreenTintManager();
 *
 *     // Smooth fade to red danger tint:
 *     tint.fadeTo(0x40FF0000, 500);
 *
 *     // Flash effect (quick pulse then fade out):
 *     tint.flash(0x60FFFFFF, 200);
 *
 *     // Pulsing tint (oscillates alpha):
 *     tint.pulse("danger", 0x40FF0000, 800, 0.2f, 0.6f);
 *
 *     // In render():
 *     tint.render(ctx, screenWidth, screenHeight);
 * }</pre>
 */
public final class ScreenTintManager {

    // ── Active tint layers ───────────────────────────────────────────────
    private final List<TintLayer> layers = new ArrayList<>();

    // ── Main tint (fade target) ──────────────────────────────────────────
    private int currentColor = 0;  // current rendered tint ARGB
    private int targetColor  = 0;  // target tint ARGB
    private long fadeStartMs;
    private long fadeDurationMs;
    private int fadeFromColor;
    private boolean fading = false;

    // =====================================================================
    //  Tint layer (for stacking multiple effects)
    // =====================================================================

    private static final class TintLayer {
        final String id;
        final int baseColor;
        final long startMs;
        final long durationMs; // 0 = infinite
        final float minAlpha;
        final float maxAlpha;
        final boolean isPulse;
        final long pulseRateMs; // period of one full oscillation
        boolean expired = false;

        // Flash (one-shot)
        TintLayer(int baseColor, long durationMs) {
            this.id          = null;
            this.baseColor   = baseColor;
            this.startMs     = System.currentTimeMillis();
            this.durationMs  = durationMs;
            this.minAlpha    = 0f;
            this.maxAlpha    = ((baseColor >> 24) & 0xFF) / 255f;
            this.isPulse     = false;
            this.pulseRateMs = 0;
        }

        // Pulse (continuous)
        TintLayer(String id, int baseColor, long pulseRateMs,
                  float minAlpha, float maxAlpha) {
            this.id          = id;
            this.baseColor   = baseColor;
            this.startMs     = System.currentTimeMillis();
            this.durationMs  = 0; // infinite
            this.minAlpha    = minAlpha;
            this.maxAlpha    = maxAlpha;
            this.isPulse     = true;
            this.pulseRateMs = pulseRateMs;
        }
    }

    // =====================================================================
    //  Fade API
    // =====================================================================

    /**
     * Smoothly fade to a target tint colour.
     *
     * @param color      target ARGB colour (alpha determines max opacity)
     * @param durationMs fade duration in milliseconds
     */
    public void fadeTo(int color, long durationMs) {
        this.fadeFromColor  = this.currentColor;
        this.targetColor    = color;
        this.fadeStartMs    = System.currentTimeMillis();
        this.fadeDurationMs = Math.max(1, durationMs);
        this.fading = true;
    }

    /**
     * Immediately set the tint colour (no fade).
     */
    public void setTint(int color) {
        this.currentColor = color;
        this.targetColor  = color;
        this.fading = false;
    }

    /**
     * Fade the tint out to transparent.
     *
     * @param durationMs fade-out duration in milliseconds
     */
    public void fadeOut(long durationMs) {
        fadeTo(0, durationMs);
    }

    /**
     * Clear the tint immediately.
     */
    public void clearTint() {
        this.currentColor = 0;
        this.targetColor  = 0;
        this.fading = false;
    }

    // =====================================================================
    //  Flash API (one-shot effects)
    // =====================================================================

    /**
     * Flash the screen with a colour that fades out.
     *
     * @param color      flash colour (alpha = initial opacity)
     * @param durationMs how long the flash lasts
     */
    public void flash(int color, long durationMs) {
        layers.add(new TintLayer(color, durationMs));
    }

    /**
     * White flash (common for impacts / confirms).
     */
    public void flashWhite(long durationMs) {
        flash(0x60FFFFFF, durationMs);
    }

    /**
     * Red flash (common for damage / errors).
     */
    public void flashRed(long durationMs) {
        flash(0x40FF0000, durationMs);
    }

    // =====================================================================
    //  Pulse API (continuous oscillation)
    // =====================================================================

    /**
     * Start a pulsing tint overlay that oscillates between min and
     * max alpha. Continues until {@link #stopPulse(String)} is called.
     *
     * @param id          unique identifier for this pulse
     * @param baseColor   the tint colour (RGB channels used, alpha ignored)
     * @param pulseRateMs period of one full oscillation in milliseconds
     * @param minAlpha    minimum alpha (0.0–1.0)
     * @param maxAlpha    maximum alpha (0.0–1.0)
     */
    public void pulse(String id, int baseColor, long pulseRateMs,
                      float minAlpha, float maxAlpha) {
        // Remove existing pulse with same id
        layers.removeIf(l -> id.equals(l.id));
        layers.add(new TintLayer(id, baseColor, pulseRateMs, minAlpha, maxAlpha));
    }

    /**
     * Stop a pulsing tint by its id.
     */
    public void stopPulse(String id) {
        layers.removeIf(l -> id.equals(l.id));
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    /**
     * Render all tint layers. Call at the start of your render method
     * (before UI components) for background tinting.
     *
     * @param ctx     the DrawContext
     * @param screenW screen width in GUI-scaled pixels
     * @param screenH screen height in GUI-scaled pixels
     */
    public void render(DrawContext ctx, int screenW, int screenH) {
        long now = System.currentTimeMillis();

        // ── Main fade tint ───────────────────────────────────────────────
        if (fading) {
            long elapsed = now - fadeStartMs;
            if (elapsed >= fadeDurationMs) {
                currentColor = targetColor;
                fading = false;
            } else {
                float t = (float) elapsed / fadeDurationMs;
                currentColor = ProceduralRenderer.lerpColor(fadeFromColor, targetColor, t);
            }
        }

        if ((currentColor & 0xFF000000) != 0) {
            ctx.fill(0, 0, screenW, screenH, currentColor);
        }

        // ── Additional layers ────────────────────────────────────────────
        Iterator<TintLayer> it = layers.iterator();
        while (it.hasNext()) {
            TintLayer layer = it.next();
            long elapsed = now - layer.startMs;

            int renderColor;

            if (layer.isPulse) {
                // Sine-wave oscillation between minAlpha and maxAlpha
                float t = (float) (Math.sin(2.0 * Math.PI * elapsed / layer.pulseRateMs) * 0.5 + 0.5);
                float alpha = layer.minAlpha + (layer.maxAlpha - layer.minAlpha) * t;
                renderColor = ProceduralRenderer.setAlpha(layer.baseColor, alpha);
            } else {
                // Flash: start at full alpha, fade to zero
                if (elapsed >= layer.durationMs) {
                    it.remove();
                    continue;
                }
                float progress = (float) elapsed / layer.durationMs;
                float alpha = layer.maxAlpha * (1f - progress);
                renderColor = ProceduralRenderer.setAlpha(layer.baseColor, alpha);
            }

            if ((renderColor & 0xFF000000) != 0) {
                ctx.fill(0, 0, screenW, screenH, renderColor);
            }
        }
    }

    // =====================================================================
    //  Queries
    // =====================================================================

    /** @return {@code true} if any tint is active (fading, pulsing, or flashing). */
    public boolean isActive() {
        return fading || (currentColor & 0xFF000000) != 0 || !layers.isEmpty();
    }

    /** @return the current rendered tint colour. */
    public int getCurrentColor() { return currentColor; }

    /** @return the number of active tint layers. */
    public int getLayerCount() { return layers.size(); }

    /**
     * Remove all layers and reset the tint.
     */
    public void clear() {
        layers.clear();
        clearTint();
    }
}
