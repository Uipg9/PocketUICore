package com.pocketuicore.render;

import com.pocketcanvascore.render.PocketCanvas;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * RenderContext backed by PocketCanvas to keep UI drawing on the shared CoreCanvas engine.
 */
public final class PocketCanvasAdapter implements RenderContext {
    private final PocketCanvas canvas;
    private final GuiGraphicsExtractor graphics;

    public PocketCanvasAdapter(PocketCanvas canvas) {
        this.canvas = canvas;
        Object nativeGraphics = canvas.context().graphics();
        if (!(nativeGraphics instanceof GuiGraphicsExtractor extractor)) {
            throw new IllegalStateException("Expected GuiGraphicsExtractor, got: " + nativeGraphics.getClass().getName());
        }
        this.graphics = extractor;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        canvas.drawRect(x1, y1, w, h, color);
    }

    @Override
    public void fillGradient(int x, int y, int x2, int y2, int colorTop, int colorBottom) {
        int w = x2 - x;
        int h = y2 - y;
        if (w <= 0 || h <= 0) return;
        canvas.drawGradientV(x, y, w, h, colorTop, colorBottom);
    }

    @Override
    public void drawTextWithShadow(Font font, Component text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
    }

    @Override
    public void drawCenteredTextWithShadow(Font font, Component text, int x, int y, int color) {
        int leftX = x - (font.width(text) / 2);
        graphics.text(font, text, leftX, y, color, true);
    }

    @Override
    public void matPush() {
        graphics.pose().pushMatrix();
    }

    @Override
    public void matPop() {
        graphics.pose().popMatrix();
    }

    @Override
    public void matTranslate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    @Override
    public void matScale(float sx, float sy) {
        graphics.pose().scale(sx, sy);
    }

    @Override
    public void drawItem(ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
    }

    @Override
    public void drawStackOverlay(Font font, ItemStack stack, int x, int y) {
        graphics.itemDecorations(font, stack, x, y);
    }

    @Override
    public void drawStackOverlay(Font font, ItemStack stack, int x, int y, String countOverride) {
        graphics.itemDecorations(font, stack, x, y, countOverride);
    }

    @Override
    public void enableScissor(int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void disableScissor() {
        graphics.disableScissor();
    }

    @Override
    public Object unwrapNative() {
        return graphics;
    }
}
