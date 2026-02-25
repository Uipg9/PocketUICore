package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Floating Text / Toast Notification Component
 * <p>
 * Renders temporary on-screen messages that fade in, hold, and fade out.
 * Useful for "+50 coins", "Estate payout!", "Item crafted", etc.
 * <p>
 * Toasts stack vertically from a configurable anchor and auto-expire.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // Show a toast at the top-centre of the screen:
 *     FloatingText.show("+50 Coins", FloatingText.Anchor.TOP_CENTER, 2000);
 *
 *     // Custom colours:
 *     FloatingText.show("Level Up!", FloatingText.Anchor.CENTER,
 *             ProceduralRenderer.COL_SUCCESS, 3000);
 *
 *     // In your screen's render():
 *     FloatingText.renderAll(ctx, screenWidth, screenHeight, delta);
 * }</pre>
 */
public final class FloatingText {

    /** Screen anchor point for toast positioning. */
    public enum Anchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    // ── Active toasts ────────────────────────────────────────────────────
    private static final List<Toast> toasts = new ArrayList<>();

    private FloatingText() {}

    // ── Toast data ───────────────────────────────────────────────────────
    private static final class Toast {
        final String message;
        final Anchor anchor;
        final int color;
        final long durationMs;
        final long spawnTimeMs;
        final int index;

        Toast(String message, Anchor anchor, int color, long durationMs, int index) {
            this.message     = message;
            this.anchor      = anchor;
            this.color       = color;
            this.durationMs  = durationMs;
            this.spawnTimeMs = System.currentTimeMillis();
            this.index       = index;
        }
    }

    // =====================================================================
    //  Show API
    // =====================================================================

    /**
     * Show a toast notification with default colour.
     *
     * @param message    the text to display
     * @param anchor     screen anchor position
     * @param durationMs how long the toast stays visible (incl. fade)
     */
    public static void show(String message, Anchor anchor, long durationMs) {
        show(message, anchor, ProceduralRenderer.COL_TEXT_PRIMARY, durationMs);
    }

    /**
     * Show a toast notification with a custom colour.
     *
     * @param message    the text to display
     * @param anchor     screen anchor position
     * @param color      ARGB text colour
     * @param durationMs how long the toast stays visible (incl. fade)
     */
    public static void show(String message, Anchor anchor, int color, long durationMs) {
        // Count existing toasts at this anchor for stacking
        int stackIndex = 0;
        for (Toast t : toasts) {
            if (t.anchor == anchor) stackIndex++;
        }
        toasts.add(new Toast(message, anchor, color, durationMs, stackIndex));
    }

    /**
     * Remove all active toasts immediately.
     */
    public static void clearAll() {
        toasts.clear();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    /**
     * Render all active toasts. Call at the end of your screen's render
     * method so toasts paint on top.
     *
     * @param ctx     the DrawContext
     * @param screenW screen width in GUI-scaled pixels
     * @param screenH screen height in GUI-scaled pixels
     * @param delta   frame delta time
     */
    public static void renderAll(DrawContext ctx, int screenW, int screenH, float delta) {
        if (toasts.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        long now = System.currentTimeMillis();

        Iterator<Toast> it = toasts.iterator();
        while (it.hasNext()) {
            Toast toast = it.next();
            long elapsed = now - toast.spawnTimeMs;

            if (elapsed >= toast.durationMs) {
                it.remove();
                continue;
            }

            // Fade in (first 200 ms) + fade out (last 300 ms)
            float alpha;
            long fadeInMs  = 200;
            long fadeOutMs = 300;
            if (elapsed < fadeInMs) {
                alpha = (float) elapsed / fadeInMs;
            } else if (elapsed > toast.durationMs - fadeOutMs) {
                alpha = (float) (toast.durationMs - elapsed) / fadeOutMs;
            } else {
                alpha = 1.0f;
            }
            alpha = Math.clamp(alpha, 0f, 1f);

            // Float upward over time
            float floatOffset = -(elapsed * 0.01f);

            int textW = tr.getWidth(toast.message);
            int textH = tr.fontHeight;
            int pad   = 6;

            // Compute anchor position
            int bx, by;
            switch (toast.anchor) {
                case TOP_LEFT        -> { bx = 10;                    by = 10; }
                case TOP_CENTER      -> { bx = (screenW - textW) / 2 - pad; by = 10; }
                case TOP_RIGHT       -> { bx = screenW - textW - pad * 2 - 10; by = 10; }
                case CENTER          -> { bx = (screenW - textW) / 2 - pad; by = (screenH - textH) / 2; }
                case BOTTOM_LEFT     -> { bx = 10;                    by = screenH - textH - pad * 2 - 10; }
                case BOTTOM_CENTER   -> { bx = (screenW - textW) / 2 - pad; by = screenH - textH - pad * 2 - 10; }
                case BOTTOM_RIGHT    -> { bx = screenW - textW - pad * 2 - 10; by = screenH - textH - pad * 2 - 10; }
                default              -> { bx = (screenW - textW) / 2 - pad; by = 10; }
            }

            // Stack offset
            by += toast.index * (textH + pad * 2 + 4);
            by += (int) floatOffset;

            int boxW = textW + pad * 2;
            int boxH = textH + pad * 2;

            int bgAlpha = (int) (alpha * 200);
            int bgColor = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_BG_SURFACE, bgAlpha);
            int borderAlpha = (int) (alpha * 150);
            int border  = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_BORDER, borderAlpha);
            int txtCol  = ProceduralRenderer.withAlpha(toast.color, (int) (alpha * 255));

            ProceduralRenderer.fillRoundedRect(ctx, bx, by, boxW, boxH, 4, bgColor);
            ProceduralRenderer.drawRoundedBorder(ctx, bx, by, boxW, boxH, 4, border);
            ctx.drawTextWithShadow(tr, net.minecraft.text.Text.literal(toast.message),
                    bx + pad, by + pad, txtCol);
        }
    }

    // =====================================================================
    //  Queries
    // =====================================================================

    /** @return the number of active toasts. */
    public static int getActiveCount() { return toasts.size(); }
}
