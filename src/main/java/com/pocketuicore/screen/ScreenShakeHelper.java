package com.pocketuicore.screen;

/**
 * Screen Shake Helper — Adds camera-shake effects to UI screens.
 * <p>
 * Provides a matrix-based screen-shake that can be applied to a
 * {@link net.minecraft.client.gui.DrawContext} during rendering.
 * The shake decays over time using an exponential fall-off.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     ScreenShakeHelper shake = new ScreenShakeHelper();
 *
 *     // Trigger on impact / error:
 *     shake.trigger(6.0f, 300);  // 6-pixel intensity, 300 ms
 *
 *     // In render():
 *     shake.apply(drawContext);
 *     // ... draw your UI ...
 *     shake.restore(drawContext);
 * }</pre>
 */
public final class ScreenShakeHelper {

    private float intensity;
    private long startTimeMs;
    private int durationMs;
    private boolean active = false;

    private float lastOffsetX;
    private float lastOffsetY;

    // =====================================================================
    //  Trigger
    // =====================================================================

    /**
     * Trigger a screen shake.
     *
     * @param intensity  max pixel offset (e.g. 4–10)
     * @param durationMs how long the shake lasts in milliseconds
     */
    public void trigger(float intensity, int durationMs) {
        this.intensity  = intensity;
        this.durationMs = durationMs;
        this.startTimeMs = System.currentTimeMillis();
        this.active = true;
    }

    /**
     * Convenience: light shake (3 px, 200 ms).
     */
    public void triggerLight() { trigger(3f, 200); }

    /**
     * Convenience: medium shake (6 px, 350 ms).
     */
    public void triggerMedium() { trigger(6f, 350); }

    /**
     * Convenience: heavy shake (10 px, 500 ms).
     */
    public void triggerHeavy() { trigger(10f, 500); }

    // =====================================================================
    //  Apply / Restore
    // =====================================================================

    /**
     * Push the matrix and apply the shake offset. Call at the start
     * of your render method.
     *
     * @param ctx the DrawContext
     */
    public void apply(net.minecraft.client.gui.DrawContext ctx) {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - startTimeMs;
        if (elapsed >= durationMs) {
            active = false;
            lastOffsetX = 0;
            lastOffsetY = 0;
            return;
        }

        // Exponential decay
        float progress = (float) elapsed / durationMs;
        float decay = 1f - progress;
        float mag = intensity * decay;

        // Pseudo-random offset using elapsed time for variety
        lastOffsetX = (float) (Math.sin(elapsed * 0.1) * mag);
        lastOffsetY = (float) (Math.cos(elapsed * 0.13) * mag);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(lastOffsetX, lastOffsetY);
    }

    /**
     * Pop the matrix pushed by {@link #apply}. Call at the end of
     * your render method.
     *
     * @param ctx the DrawContext
     */
    public void restore(net.minecraft.client.gui.DrawContext ctx) {
        if (!active && lastOffsetX == 0 && lastOffsetY == 0) return;
        ctx.getMatrices().popMatrix();
    }

    // =====================================================================
    //  Query
    // =====================================================================

    /** @return {@code true} if a shake is currently in progress. */
    public boolean isActive() { return active; }

    /** @return the last computed X offset (for external use). */
    public float getLastOffsetX() { return lastOffsetX; }

    /** @return the last computed Y offset (for external use). */
    public float getLastOffsetY() { return lastOffsetY; }
}
