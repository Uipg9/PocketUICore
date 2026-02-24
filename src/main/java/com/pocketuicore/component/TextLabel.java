package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Text Label Component
 * <p>
 * A simple text-rendering component with left / centre / right
 * alignment, optional scaling, and shadow rendering.
 * <p>
 * Pairs naturally with {@link com.pocketuicore.data.ObservableState}
 * for reactive text updates:
 * <pre>{@code
 *     ObservableState<String> name = new ObservableState<>("Steve");
 *     TextLabel label = new TextLabel(10, 10, 100, 12, "Steve");
 *     name.addListener(label::setText);
 * }</pre>
 */
public class TextLabel extends UIComponent {

    /** Text alignment within the label bounds. */
    public enum Align { LEFT, CENTER, RIGHT }

    // ── State ────────────────────────────────────────────────────────────
    private String text;
    private int color;
    private Align align;
    private float scale;

    // =====================================================================
    //  Construction
    // =====================================================================

    public TextLabel(int x, int y, int width, int height, String text) {
        this(x, y, width, height, text,
             ProceduralRenderer.COL_TEXT_PRIMARY, Align.LEFT, 1.0f);
    }

    public TextLabel(int x, int y, int width, int height, String text, int color) {
        this(x, y, width, height, text, color, Align.LEFT, 1.0f);
    }

    public TextLabel(int x, int y, int width, int height, String text,
                     int color, Align align, float scale) {
        super(x, y, width, height);
        this.text  = text != null ? text : "";
        this.color = color;
        this.align = align;
        this.scale = scale;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (text.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int textY = y + (height - Math.round(tr.fontHeight * scale)) / 2;

        if (scale != 1.0f) {
            switch (align) {
                case LEFT   -> ProceduralRenderer.drawScaledText(
                        ctx, tr, text, x, textY, color, scale);
                case CENTER -> ProceduralRenderer.drawScaledCenteredText(
                        ctx, tr, text, x + width / 2, textY, color, scale);
                case RIGHT  -> {
                    int textW = Math.round(tr.getWidth(text) * scale);
                    ProceduralRenderer.drawScaledText(
                            ctx, tr, text, x + width - textW, textY, color, scale);
                }
            }
        } else {
            switch (align) {
                case LEFT   -> ProceduralRenderer.drawText(
                        ctx, tr, text, x, textY, color);
                case CENTER -> ProceduralRenderer.drawCenteredText(
                        ctx, tr, text, x + width / 2, textY, color);
                case RIGHT  -> {
                    int textW = tr.getWidth(text);
                    ProceduralRenderer.drawText(
                            ctx, tr, text, x + width - textW, textY, color);
                }
            }
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public String getText()           { return text; }
    public void setText(String t)     { this.text = t != null ? t : ""; }
    public int  getColor()            { return color; }
    public void setColor(int c)       { this.color = c; }
    public Align getAlign()           { return align; }
    public void setAlign(Align a)     { this.align = a; }
    public float getScale()           { return scale; }
    public void setScale(float s)     { this.scale = s; }
}
