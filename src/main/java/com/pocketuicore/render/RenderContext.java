package com.pocketuicore.render;

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/**
 * Version-neutral rendering context. Wraps DrawContext (1.21.11)
 * or GuiGraphics (26.1) behind a common interface so ProceduralRenderer
 * and UIComponent need no version overlays.
 */
public interface RenderContext {
    void fill(int x1, int y1, int x2, int y2, int color);
    void fillGradient(int x, int y, int x2, int y2, int colorTop, int colorBottom);
    void drawTextWithShadow(Font font, Component text, int x, int y, int color);
    void drawCenteredTextWithShadow(Font font, Component text, int x, int y, int color);
    // Matrix ops — version-neutral replacements for getMatrices().push/translate/scale/pop
    void matPush();
    void matPop();
    void matTranslate(float x, float y);
    void matScale(float sx, float sy);
    void drawItem(ItemStack stack, int x, int y);
    void drawStackOverlay(Font font, ItemStack stack, int x, int y);
    void drawStackOverlay(Font font, ItemStack stack, int x, int y, String countOverride);
    void enableScissor(int x1, int y1, int x2, int y2);
    void disableScissor();
    /**
     * Returns the underlying native context object (DrawContext for 1.21.11,
     * GuiGraphics for 26.1). Used as a bridge for PocketCanvasCore until
     * it is migrated to RenderContext.
     */
    default Object unwrapNative() { return null; }
}
