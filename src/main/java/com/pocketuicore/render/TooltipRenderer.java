package com.pocketuicore.render;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Tooltip Render Queue — ensures tooltips are always drawn on top
 * of all other UI by deferring their rendering until after all components
 * have finished drawing.
 * <p>
 * Instead of rendering tooltips inline (which causes z-order issues when
 * a component's tooltip overlaps a neighbouring panel), components call
 * {@link #queue(Runnable)} and the screen calls {@link #flush(DrawContext)}
 * at the very end of its render cycle.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // Inside a component's render():
 *     if (hovered) {
 *         TooltipRenderer.queue(() -> {
 *             UIComponent.renderTooltip(ctx, myTooltip, mouseX, mouseY, screenW, screenH);
 *         });
 *     }
 *
 *     // At the end of your screen's render():
 *     TooltipRenderer.flush(ctx);
 * }</pre>
 *
 * @since 1.13.0
 */
public final class TooltipRenderer {

    private TooltipRenderer() {}

    private static final List<Runnable> pending = new ArrayList<>();

    /**
     * Enqueue a tooltip render action. The action will be executed during
     * the next {@link #flush} call, after all normal UI rendering.
     *
     * @param renderAction a {@link Runnable} that performs the tooltip draw
     */
    public static void queue(Runnable renderAction) {
        if (renderAction != null) pending.add(renderAction);
    }

    /**
     * Execute and clear all queued tooltip render actions.  Call this at
     * the <b>very end</b> of your screen / HUD render method.
     *
     * @param ctx the current DrawContext (passed for context; the queued
     *            actions should capture their own DrawContext reference)
     */
    public static void flush(DrawContext ctx) {
        if (pending.isEmpty()) return;
        for (Runnable action : pending) {
            action.run();
        }
        pending.clear();
    }

    /** @return the number of tooltips currently queued. */
    public static int pendingCount() { return pending.size(); }

    /** Discard all queued tooltips without rendering them. */
    public static void clear() { pending.clear(); }
}
