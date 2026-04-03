package com.pocketuicore.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class DrawContextAdapter implements RenderContext {
    private final DrawContext ctx;

    public DrawContextAdapter(DrawContext ctx) { this.ctx = ctx; }

    @Override public void fill(int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y2, color);
    }
    @Override public void fillGradient(int x, int y, int x2, int y2, int top, int bot) {
        ctx.fillGradient(x, y, x2, y2, top, bot);
    }
    @Override public void drawTextWithShadow(TextRenderer tr, Text text, int x, int y, int color) {
        ctx.drawTextWithShadow(tr, text, x, y, color);
    }
    @Override public void drawCenteredTextWithShadow(TextRenderer tr, Text text, int x, int y, int color) {
        ctx.drawCenteredTextWithShadow(tr, text, x, y, color);
    }
    // Matrix ops — delegate to DrawContext's Matrix3x2fStack (JOML)
    @Override public void matPush() { ctx.getMatrices().pushMatrix(); }
    @Override public void matPop() { ctx.getMatrices().popMatrix(); }
    @Override public void matTranslate(float x, float y) { ctx.getMatrices().translate(x, y); }
    @Override public void matScale(float sx, float sy) { ctx.getMatrices().scale(sx, sy); }
    @Override public void drawItem(ItemStack stack, int x, int y) { ctx.drawItem(stack, x, y); }
    @Override public void drawStackOverlay(TextRenderer tr, ItemStack stack, int x, int y) {
        ctx.drawStackOverlay(tr, stack, x, y);
    }
    @Override public void drawStackOverlay(TextRenderer tr, ItemStack stack, int x, int y, String countOverride) {
        ctx.drawStackOverlay(tr, stack, x, y, countOverride);
    }
    @Override public void enableScissor(int x1, int y1, int x2, int y2) { ctx.enableScissor(x1, y1, x2, y2); }
    @Override public void disableScissor() { ctx.disableScissor(); }

    @Override public Object unwrapNative() { return ctx; }
    /** Unwrap for any call site that still needs the raw DrawContext. */
    public DrawContext unwrap() { return ctx; }
}
