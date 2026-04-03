package com.pocketuicore.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class GuiGraphicsAdapter implements RenderContext {
    private final GuiGraphicsExtractor ctx;

    public GuiGraphicsAdapter(GuiGraphicsExtractor ctx) { this.ctx = ctx; }

    @Override public void fill(int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y2, color);
    }
    @Override public void fillGradient(int x, int y, int x2, int y2, int top, int bot) {
        ctx.fillGradient(x, y, x2, y2, top, bot);
    }
    @Override public void drawTextWithShadow(Font font, Component text, int x, int y, int color) {
        ctx.text(font, text, x, y, color, true);
    }
    @Override public void drawCenteredTextWithShadow(Font font, Component text, int x, int y, int color) {
        // Extractor has centered text draw but no explicit shadow toggle, so emulate shadowed centered text.
        int leftX = x - (font.width(text) / 2);
        ctx.text(font, text, leftX, y, color, true);
    }
    @Override public void matPush() { ctx.pose().pushMatrix(); }
    @Override public void matPop() { ctx.pose().popMatrix(); }
    @Override public void matTranslate(float x, float y) { ctx.pose().translate(x, y); }
    @Override public void matScale(float sx, float sy) { ctx.pose().scale(sx, sy); }
    @Override public void drawItem(ItemStack stack, int x, int y) { ctx.item(stack, x, y); }
    @Override public void drawStackOverlay(Font font, ItemStack stack, int x, int y) {
        ctx.itemDecorations(font, stack, x, y);
    }
    @Override public void drawStackOverlay(Font font, ItemStack stack, int x, int y, String countOverride) {
        ctx.itemDecorations(font, stack, x, y, countOverride);
    }
    @Override public void enableScissor(int x1, int y1, int x2, int y2) { ctx.enableScissor(x1, y1, x2, y2); }
    @Override public void disableScissor() { ctx.disableScissor(); }

    @Override public Object unwrapNative() { return ctx; }
    public GuiGraphicsExtractor unwrap() { return ctx; }
}
