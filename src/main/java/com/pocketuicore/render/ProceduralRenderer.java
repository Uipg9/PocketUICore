package com.pocketuicore.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Module 1 — Procedural Render Engine
 * <p>
 * All drawing is done through {@link DrawContext} with zero external textures.
 * Every shape is constructed from filled rectangles, scanline arcs, and
 * gradient fills so the entire UI is resolution-independent dark-mode art.
 * <p>
 * <b>Performance note:</b> Rounded-rectangle corners use a scanline approach
 * (one {@code fill()} per pixel row per corner). For a typical radius of 6 px
 * that is 24 extra fill calls — negligible compared to a single chunk rebuild.
 * <p>
 * All colours are standard ARGB (0xAARRGGBB).
 *
 * <h2>Dark-mode palette constants</h2>
 * Use the {@code COL_*} fields for a consistent look across all Pocket screens.
 */
public final class ProceduralRenderer {

    // ── Dark-mode palette ────────────────────────────────────────────────
    /** Very dark blue-purple background.  #1A1A2E */
    public static final int COL_BG_PRIMARY    = 0xFF1A1A2E;
    /** Deep navy surface / panel fill.    #16213E */
    public static final int COL_BG_SURFACE    = 0xFF16213E;
    /** Medium navy for elevated surfaces. #0F3460 */
    public static final int COL_BG_ELEVATED   = 0xFF0F3460;
    /** Vibrant accent (red-pink).         #E94560 */
    public static final int COL_ACCENT        = 0xFFE94560;
    /** Teal accent for positive actions.  #00D2FF */
    public static final int COL_ACCENT_TEAL   = 0xFF00D2FF;
    /** Success green.                     #2ECC71 */
    public static final int COL_SUCCESS       = 0xFF2ECC71;
    /** Warning amber.                     #F39C12 */
    public static final int COL_WARNING       = 0xFFF39C12;
    /** Error red.                         #E74C3C */
    public static final int COL_ERROR         = 0xFFE74C3C;
    /** Primary text colour.               #EAEAEA */
    public static final int COL_TEXT_PRIMARY  = 0xFFEAEAEA;
    /** Secondary / muted text colour.     #A0A0B0 */
    public static final int COL_TEXT_MUTED    = 0xFFA0A0B0;
    /** Subtle border / divider.           #2A2A4A */
    public static final int COL_BORDER        = 0xFF2A2A4A;
    /** Hover highlight tint.              #1F4068 */
    public static final int COL_HOVER         = 0xFF1F4068;
    /** Semi-transparent overlay.          75 % black */
    public static final int COL_OVERLAY       = 0xC0000000;
    /** Shadow base colour (pure black, alpha varies). */
    public static final int COL_SHADOW_BASE   = 0x00000000;

    private ProceduralRenderer() { /* static utility */ }

    // =====================================================================
    //  Scanline geometry cache (eliminates per-frame Math.sqrt calls)
    // =====================================================================

    /**
     * Maps corner radius → array of arc widths (dx) per scanline row.
     * Computed once per unique radius, reused every subsequent frame.
     */
    private static final Map<Integer, int[]> SCANLINE_CACHE = new HashMap<>();

    private static int[] getScanlineDx(int radius) {
        return SCANLINE_CACHE.computeIfAbsent(radius, r -> {
            int[] dx = new int[r];
            for (int i = 0; i < r; i++) {
                int dy = r - i;
                dx[i] = (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy));
            }
            return dx;
        });
    }

    /**
     * Clear the scanline geometry cache. Call after closing a screen
     * with many unique radii to reclaim memory (rarely needed).
     */
    public static void clearScanlineCache() { SCANLINE_CACHE.clear(); }

    // =====================================================================
    //  Colour helpers
    // =====================================================================


    /**
     * Parse a hex colour string into an ARGB int.
     * Accepts {@code "#RRGGBB"}, {@code "#AARRGGBB"}, or the same without '#'.
     */
    public static int hex(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        long v = Long.parseUnsignedLong(hex, 16);
        if (hex.length() <= 6) v |= 0xFF000000L; // add full alpha
        return (int) v;
    }

    /** Return {@code color} with its alpha channel replaced by {@code alpha} (0–255). */
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * Linearly interpolate between two ARGB colours per-channel.
     *
     * @param t blend factor 0.0 → {@code from}, 1.0 → {@code to}
     */
    public static int lerpColor(int from, int to, float t) {
        t = Math.clamp(t, 0f, 1f);
        int aF = (from >> 24) & 0xFF, rF = (from >> 16) & 0xFF, gF = (from >> 8) & 0xFF, bF = from & 0xFF;
        int aT = (to   >> 24) & 0xFF, rT = (to   >> 16) & 0xFF, gT = (to   >> 8) & 0xFF, bT = to   & 0xFF;
        int a = (int) (aF + (aT - aF) * t);
        int r = (int) (rF + (rT - rF) * t);
        int g = (int) (gF + (gT - gF) * t);
        int b = (int) (bF + (bT - bF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // =====================================================================
    //  Basic shapes
    // =====================================================================

    /**
     * Solid-colour filled rectangle.
     *
     * @param x     left edge
     * @param y     top edge
     * @param w     width  (pixels)
     * @param h     height (pixels)
     * @param color ARGB colour
     */
    public static void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, color);
    }

    /**
     * 1-pixel border rectangle (outline only).
     */
    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);               // top
        ctx.fill(x, y + h - 1, x + w, y + h, color);       // bottom
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);       // left
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color); // right
    }

    // =====================================================================
    //  Rounded rectangle  (scanline arc approach — single-draw-batch safe)
    // =====================================================================

    /**
     * Filled rectangle with rounded corners.
     * <p>
     * Implementation: three body rectangles plus four quarter-circle
     * scanline fans. Total extra fills = {@code 4 × radius}.
     *
     * @param radius corner radius in pixels (clamped to half of min dimension)
     */
    public static void fillRoundedRect(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w, h) / 2);
        if (radius <= 0) {
            fillRect(ctx, x, y, w, h, color);
            return;
        }

        // ── Body rectangles (no overlap) ─────────────────────────────────
        // Horizontal centre strip (full height)
        ctx.fill(x + radius, y, x + w - radius, y + h, color);
        // Left column (between corners)
        ctx.fill(x, y + radius, x + radius, y + h - radius, color);
        // Right column (between corners)
        ctx.fill(x + w - radius, y + radius, x + w, y + h - radius, color);

        // ── Corner scanlines (cached — zero sqrt per frame) ─────────────
        int[] dxArr = getScanlineDx(radius);
        for (int i = 0; i < radius; i++) {
            int arcDx = dxArr[i];
            // Top-left
            ctx.fill(x + radius - arcDx, y + i, x + radius, y + i + 1, color);
            // Top-right
            ctx.fill(x + w - radius, y + i, x + w - radius + arcDx, y + i + 1, color);
            // Bottom-left
            ctx.fill(x + radius - arcDx, y + h - 1 - i, x + radius, y + h - i, color);
            // Bottom-right
            ctx.fill(x + w - radius, y + h - 1 - i, x + w - radius + arcDx, y + h - i, color);
        }
    }

    /**
     * Rounded rectangle with a 1-pixel border drawn on top.
     */
    public static void fillRoundedRectWithBorder(DrawContext ctx, int x, int y, int w, int h,
                                                  int radius, int fillColor, int borderColor) {
        fillRoundedRect(ctx, x, y, w, h, radius, fillColor);
        drawRoundedBorder(ctx, x, y, w, h, radius, borderColor);
    }

    /**
     * 1-pixel rounded border (no fill).
     */
    public static void drawRoundedBorder(DrawContext ctx, int x, int y, int w, int h,
                                          int radius, int color) {
        if (w <= 0 || h <= 0) return;
        radius = Math.min(radius, Math.min(w, h) / 2);
        if (radius <= 0) {
            drawBorder(ctx, x, y, w, h, color);
            return;
        }

        // Straight edges (between corners)
        ctx.fill(x + radius, y, x + w - radius, y + 1, color);                 // top
        ctx.fill(x + radius, y + h - 1, x + w - radius, y + h, color);         // bottom
        ctx.fill(x, y + radius, x + 1, y + h - radius, color);                 // left
        ctx.fill(x + w - 1, y + radius, x + w, y + h - radius, color);         // right

        // Corner arcs (cached — zero sqrt per frame)
        int[] dxArr = getScanlineDx(radius);
        for (int i = 0; i < radius; i++) {
            int arcDx = dxArr[i];
            int prevArcDx = (i > 0) ? dxArr[i - 1] : 0;

            // Single-pixel-wide arc segment
            // Top-left
            ctx.fill(x + radius - arcDx, y + i, x + radius - prevArcDx, y + i + 1, color);
            // Top-right
            ctx.fill(x + w - radius + prevArcDx, y + i, x + w - radius + arcDx, y + i + 1, color);
            // Bottom-left
            ctx.fill(x + radius - arcDx, y + h - 1 - i, x + radius - prevArcDx, y + h - i, color);
            // Bottom-right
            ctx.fill(x + w - radius + prevArcDx, y + h - 1 - i, x + w - radius + arcDx, y + h - i, color);
        }
    }

    // =====================================================================
    //  Gradients
    // =====================================================================

    /**
     * Vertical gradient rectangle (top colour → bottom colour).
     * Uses {@link DrawContext#fillGradient} internally.
     */
    public static void fillGradientV(DrawContext ctx, int x, int y, int w, int h,
                                      int topColor, int bottomColor) {
        if (w <= 0 || h <= 0) return;
        ctx.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
    }

    /**
     * Horizontal gradient rectangle (left colour → right colour).
     * Implemented as vertical 1-px-wide columns with interpolated colour.
     * For very wide gradients (>300 px) consider batching externally.
     */
    public static void fillGradientH(DrawContext ctx, int x, int y, int w, int h,
                                      int leftColor, int rightColor) {
        if (w <= 0 || h <= 0) return;
        for (int col = 0; col < w; col++) {
            float t = (float) col / Math.max(w - 1, 1);
            int c = lerpColor(leftColor, rightColor, t);
            ctx.fill(x + col, y, x + col + 1, y + h, c);
        }
    }

    // =====================================================================
    //  Drop shadow
    // =====================================================================

    /**
     * Soft drop shadow behind a rounded rectangle.
     * <p>
     * Draws {@code layers} concentric rounded rects with decreasing alpha.
     * The innermost layer aligns exactly with the target rectangle.
     *
     * @param x       target rect left
     * @param y       target rect top
     * @param w       target rect width
     * @param h       target rect height
     * @param radius  corner radius of the target rect
     * @param layers  number of shadow layers (4–8 is a good range)
     * @param maxAlpha peak alpha at the innermost layer (0–255)
     */
    public static void drawDropShadow(DrawContext ctx, int x, int y, int w, int h,
                                       int radius, int layers, int maxAlpha) {
        for (int i = layers; i >= 1; i--) {
            // Quadratic fall-off gives a softer penumbra than linear
            float ratio = (float) i / layers;
            int alpha = (int) (maxAlpha * ratio * ratio);
            if (alpha <= 0) continue;
            int shadowColor = (alpha << 24); // black + computed alpha
            fillRoundedRect(ctx,
                    x - i, y - i,
                    w + i * 2, h + i * 2,
                    radius + i,
                    shadowColor);
        }
    }

    /**
     * Convenience overload — 6 layers at 40 % peak alpha. Looks good on
     * most dark surfaces.
     */
    public static void drawDropShadow(DrawContext ctx, int x, int y, int w, int h, int radius) {
        drawDropShadow(ctx, x, y, w, h, radius, 6, 102); // 102 ≈ 40 % of 255
    }

    // =====================================================================
    //  Text rendering
    // =====================================================================

    /** Left-aligned text with shadow, standard scale. */
    public static void drawText(DrawContext ctx, TextRenderer tr, String text,
                                int x, int y, int color) {
        ctx.drawTextWithShadow(tr, Text.literal(text), x, y, color);
    }

    /** Centred text with shadow, standard scale. */
    public static void drawCenteredText(DrawContext ctx, TextRenderer tr, String text,
                                        int centerX, int y, int color) {
        ctx.drawCenteredTextWithShadow(tr, Text.literal(text), centerX, y, color);
    }

    /**
     * Left-aligned text drawn at an arbitrary scale.
     * Uses matrix push / scale / pop so neighbouring draws are unaffected.
     */
    public static void drawScaledText(DrawContext ctx, TextRenderer tr, String text,
                                      int x, int y, int color, float scale) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(tr, Text.literal(text), 0, 0, color);
        ctx.getMatrices().popMatrix();
    }

    /**
     * Centred text at an arbitrary scale.
     * The text is measured at scale 1 and then the matrix is offset so
     * the scaled text remains centred on {@code centerX}.
     */
    public static void drawScaledCenteredText(DrawContext ctx, TextRenderer tr, String text,
                                              int centerX, int y, int color, float scale) {
        int textWidth = tr.getWidth(text);
        float scaledHalf = (textWidth * scale) / 2f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(centerX - scaledHalf, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(tr, Text.literal(text), 0, 0, color);
        ctx.getMatrices().popMatrix();
    }

    /**
     * Draw a {@link Text} object (supports formatting, translatable keys, etc.)
     * at a custom scale and position.
     */
    public static void drawScaledText(DrawContext ctx, TextRenderer tr, Text text,
                                      int x, int y, int color, float scale) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(tr, text, 0, 0, color);
        ctx.getMatrices().popMatrix();
    }

    // =====================================================================
    //  Utility: screen-relative helpers
    // =====================================================================

    /**
     * Draw a full-screen dark overlay. Useful as a background behind modals
     * or the main Pocket menu.
     */
    public static void drawFullScreenOverlay(DrawContext ctx, int screenW, int screenH, int color) {
        ctx.fill(0, 0, screenW, screenH, color);
    }

    /**
     * Draw a horizontal divider line.
     */
    public static void drawDivider(DrawContext ctx, int x, int y, int width, int color) {
        ctx.fill(x, y, x + width, y + 1, color);
    }

    // =====================================================================
    //  Geometry caching — bake once, replay free
    // =====================================================================

    /**
     * A pre-baked shape: a flat list of fill-rect operations stored in a
     * compact {@code int[]} array. Call {@link #render} every frame instead
     * of recomputing rounded-rect scanlines, shadows, or gradients.
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     *     // Bake once (e.g. in init or on resize):
     *     CachedShape bg = ProceduralRenderer.bakeRoundedRect(
     *             10, 10, 200, 100, 6, COL_BG_SURFACE);
     *
     *     // Render every frame (zero math):
     *     bg.render(drawContext);
     * }</pre>
     */
    public static final class CachedShape {
        private final int[] data;   // flat: [x1, y1, x2, y2, color, ...]
        private final int fillCount;

        CachedShape(int[] data, int fillCount) {
            this.data      = data;
            this.fillCount = fillCount;
        }

        /** Render at the baked position. */
        public void render(DrawContext ctx) {
            for (int i = 0, len = fillCount * 5; i < len; i += 5) {
                ctx.fill(data[i], data[i + 1], data[i + 2], data[i + 3], data[i + 4]);
            }
        }

        /** Render translated by an offset (useful for scrolling / animation). */
        public void render(DrawContext ctx, int offsetX, int offsetY) {
            for (int i = 0, len = fillCount * 5; i < len; i += 5) {
                ctx.fill(data[i] + offsetX, data[i + 1] + offsetY,
                         data[i + 2] + offsetX, data[i + 3] + offsetY,
                         data[i + 4]);
            }
        }

        /** Number of fill-rect operations in this shape. */
        public int getFillCount() { return fillCount; }

        /** True if this shape contains no fill operations. */
        public boolean isEmpty() { return fillCount == 0; }
    }

    // ── Shape builder (internal) ─────────────────────────────────────────

    private static final class ShapeBuilder {
        private int[] buf = new int[120]; // initial room for 24 fills
        private int count = 0;

        void fill(int x1, int y1, int x2, int y2, int color) {
            int idx = count * 5;
            if (idx + 5 > buf.length) {
                int[] grown = new int[buf.length * 2];
                System.arraycopy(buf, 0, grown, 0, buf.length);
                buf = grown;
            }
            buf[idx]     = x1;
            buf[idx + 1] = y1;
            buf[idx + 2] = x2;
            buf[idx + 3] = y2;
            buf[idx + 4] = color;
            count++;
        }

        /** Merge another CachedShape's fills into this builder. */
        void merge(CachedShape other) {
            for (int i = 0, len = other.fillCount * 5; i < len; i += 5) {
                fill(other.data[i], other.data[i + 1], other.data[i + 2],
                     other.data[i + 3], other.data[i + 4]);
            }
        }

        CachedShape build() {
            int[] trimmed = new int[count * 5];
            System.arraycopy(buf, 0, trimmed, 0, count * 5);
            return new CachedShape(trimmed, count);
        }
    }

    // ── Bake methods ─────────────────────────────────────────────────────

    /**
     * Bake a filled rounded rectangle into a {@link CachedShape}.
     * The expensive scanline-arc math runs <b>once</b>; subsequent render
     * calls are pure fill replay.
     */
    public static CachedShape bakeRoundedRect(int x, int y, int w, int h,
                                               int radius, int color) {
        if (w <= 0 || h <= 0) return new CachedShape(new int[0], 0);
        radius = Math.min(radius, Math.min(w, h) / 2);
        ShapeBuilder sb = new ShapeBuilder();
        if (radius <= 0) {
            sb.fill(x, y, x + w, y + h, color);
            return sb.build();
        }
        sb.fill(x + radius, y, x + w - radius, y + h, color);
        sb.fill(x, y + radius, x + radius, y + h - radius, color);
        sb.fill(x + w - radius, y + radius, x + w, y + h - radius, color);
        int[] dx = getScanlineDx(radius);
        for (int i = 0; i < radius; i++) {
            sb.fill(x + radius - dx[i], y + i, x + radius, y + i + 1, color);
            sb.fill(x + w - radius, y + i, x + w - radius + dx[i], y + i + 1, color);
            sb.fill(x + radius - dx[i], y + h - 1 - i, x + radius, y + h - i, color);
            sb.fill(x + w - radius, y + h - 1 - i, x + w - radius + dx[i], y + h - i, color);
        }
        return sb.build();
    }

    /**
     * Bake a 1-pixel rounded border into a {@link CachedShape}.
     */
    public static CachedShape bakeRoundedBorder(int x, int y, int w, int h,
                                                 int radius, int color) {
        if (w <= 0 || h <= 0) return new CachedShape(new int[0], 0);
        radius = Math.min(radius, Math.min(w, h) / 2);
        ShapeBuilder sb = new ShapeBuilder();
        if (radius <= 0) {
            sb.fill(x, y, x + w, y + 1, color);
            sb.fill(x, y + h - 1, x + w, y + h, color);
            sb.fill(x, y + 1, x + 1, y + h - 1, color);
            sb.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
            return sb.build();
        }
        sb.fill(x + radius, y, x + w - radius, y + 1, color);
        sb.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        sb.fill(x, y + radius, x + 1, y + h - radius, color);
        sb.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
        int[] dx = getScanlineDx(radius);
        for (int i = 0; i < radius; i++) {
            int arcDx = dx[i];
            int prevArcDx = (i > 0) ? dx[i - 1] : 0;
            sb.fill(x + radius - arcDx, y + i, x + radius - prevArcDx, y + i + 1, color);
            sb.fill(x + w - radius + prevArcDx, y + i, x + w - radius + arcDx, y + i + 1, color);
            sb.fill(x + radius - arcDx, y + h - 1 - i, x + radius - prevArcDx, y + h - i, color);
            sb.fill(x + w - radius + prevArcDx, y + h - 1 - i, x + w - radius + arcDx, y + h - i, color);
        }
        return sb.build();
    }

    /**
     * Bake a drop shadow into a {@link CachedShape}.
     */
    public static CachedShape bakeDropShadow(int x, int y, int w, int h,
                                              int radius, int layers, int maxAlpha) {
        ShapeBuilder sb = new ShapeBuilder();
        for (int l = layers; l >= 1; l--) {
            float ratio = (float) l / layers;
            int alpha = (int) (maxAlpha * ratio * ratio);
            if (alpha <= 0) continue;
            int col = alpha << 24;
            int sx = x - l, sy = y - l, sw = w + l * 2, sh = h + l * 2;
            int sr = Math.min(radius + l, Math.min(sw, sh) / 2);
            if (sr <= 0) {
                sb.fill(sx, sy, sx + sw, sy + sh, col);
                continue;
            }
            sb.fill(sx + sr, sy, sx + sw - sr, sy + sh, col);
            sb.fill(sx, sy + sr, sx + sr, sy + sh - sr, col);
            sb.fill(sx + sw - sr, sy + sr, sx + sw, sy + sh - sr, col);
            int[] dx = getScanlineDx(sr);
            for (int i = 0; i < sr; i++) {
                sb.fill(sx + sr - dx[i], sy + i, sx + sr, sy + i + 1, col);
                sb.fill(sx + sw - sr, sy + i, sx + sw - sr + dx[i], sy + i + 1, col);
                sb.fill(sx + sr - dx[i], sy + sh - 1 - i, sx + sr, sy + sh - i, col);
                sb.fill(sx + sw - sr, sy + sh - 1 - i, sx + sw - sr + dx[i], sy + sh - i, col);
            }
        }
        return sb.build();
    }

    /**
     * Bake a complete dark panel (shadow + background + optional border)
     * into one {@link CachedShape} for the absolute lowest per-frame cost.
     * <p>
     * Ideal for static panels that never move or resize.
     *
     * @param x           panel left
     * @param y           panel top
     * @param w           panel width
     * @param h           panel height
     * @param radius      corner radius
     * @param bgColor     background colour
     * @param borderColor border colour (ignored if {@code withBorder} is false)
     * @param withShadow  include 6-layer drop shadow
     * @param withBorder  include 1-pixel rounded border
     */
    public static CachedShape bakePanel(int x, int y, int w, int h,
                                         int radius, int bgColor, int borderColor,
                                         boolean withShadow, boolean withBorder) {
        ShapeBuilder sb = new ShapeBuilder();
        if (withShadow) sb.merge(bakeDropShadow(x, y, w, h, radius, 6, 102));
        sb.merge(bakeRoundedRect(x, y, w, h, radius, bgColor));
        if (withBorder) sb.merge(bakeRoundedBorder(x, y, w, h, radius, borderColor));
        return sb.build();
    }
}
