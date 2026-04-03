package com.pocketuicore.render;

import net.minecraft.client.gui.Font;
import com.pocketuicore.render.RenderContext;

/**
 * Unified rendering utility that delegates to ProceduralRenderer.
 * This class serves as a wrapper/compatibility layer for rendering operations.
 */
public class RenderUtils {

    // ─────────────────────────────────────────────────────────────────
    // Color Constants (exported from ProceduralRenderer)
    // ─────────────────────────────────────────────────────────────────

    public static final int COL_BG_PRIMARY = ProceduralRenderer.COL_BG_PRIMARY;
    public static final int COL_BG_SURFACE = ProceduralRenderer.COL_BG_SURFACE;
    public static final int COL_BG_ELEVATED = ProceduralRenderer.COL_BG_ELEVATED;
    public static final int COL_ACCENT = ProceduralRenderer.COL_ACCENT;
    public static final int COL_ACCENT_TEAL = ProceduralRenderer.COL_ACCENT_TEAL;
    public static final int COL_SUCCESS = ProceduralRenderer.COL_SUCCESS;
    public static final int COL_WARNING = ProceduralRenderer.COL_WARNING;
    public static final int COL_ERROR = ProceduralRenderer.COL_ERROR;
    public static final int COL_TEXT_PRIMARY = ProceduralRenderer.COL_TEXT_PRIMARY;
    public static final int COL_TEXT_MUTED = ProceduralRenderer.COL_TEXT_MUTED;
    public static final int COL_BORDER = ProceduralRenderer.COL_BORDER;
    public static final int COL_HOVER = ProceduralRenderer.COL_HOVER;
    public static final int COL_OVERLAY = ProceduralRenderer.COL_OVERLAY;
    public static final int COL_SHADOW_BASE = ProceduralRenderer.COL_SHADOW_BASE;

    // ─────────────────────────────────────────────────────────────────
    // Rendering Methods (delegating to ProceduralRenderer)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fill a rounded rectangle.
     */
    public static void fillRoundedRect(RenderContext ctx, int x, int y, int width, int height, int radius, int color) {
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, radius, color);
    }

    /**
     * Draw a rounded rectangle border.
     */
    public static void drawRoundedBorder(RenderContext ctx, int x, int y, int width, int height, int radius, int color) {
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, radius, color);
    }

    /**
     * Draw centered Component.
     */
    public static void drawCenteredText(RenderContext ctx, Font Font, String Component, int x, int y, int color) {
        ProceduralRenderer.drawCenteredText(ctx, Font, Component, x, y, color);
    }

    /**
     * Draw left-aligned Component.
     */
    public static void drawText(RenderContext ctx, Font Font, String Component, int x, int y, int color) {
        ProceduralRenderer.drawText(ctx, Font, Component, x, y, color);
    }

    /**
     * Linearly interpolate between two colors.
     */
    public static int lerpColor(int color1, int color2, float t) {
        return ProceduralRenderer.lerpColor(color1, color2, t);
    }

    /**
     * Set the alpha channel of a color.
     */
    public static int withAlpha(int color, int alpha) {
        return ProceduralRenderer.withAlpha(color, alpha);
    }

    /**
     * Darken a color by the given factor.
     */
    public static int darken(int argb, float factor) {
        return ProceduralRenderer.darken(argb, factor);
    }

    /**
     * Lighten a color by the given factor.
     */
    public static int lighten(int argb, float factor) {
        return ProceduralRenderer.lighten(argb, factor);
    }

    /**
     * Saturate a color by the given factor.
     */
    public static int saturate(int argb, float factor) {
        return ProceduralRenderer.saturate(argb, factor);
    }

    /**
     * Convert hex string to color.
     */
    public static int hex(String hex) {
        return ProceduralRenderer.hex(hex);
    }

    // Prevent instantiation
    private RenderUtils() {
    }
}

