package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Text Label Component
 * <p>
 * A text-rendering component with left / centre / right alignment,
 * optional scaling, shadow rendering, and word-wrapping support.
 * <p>
 * Supports both plain {@link String} and Minecraft {@link Text} values.
 * When a {@code Text} is set, its formatted string representation is
 * used for rendering with the {@code Text} variant overloads.
 * <p>
 * <b>Wrapping:</b> Set {@link #setWrapWidth(int)} to a positive pixel
 * width and the label will automatically break text into multiple lines.
 * The component's {@code height} is updated to fit all wrapped lines.
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
    private Text richText;
    private int color;
    private Align align;
    private float scale;

    /** Wrap width in pixels. 0 = no wrapping (single line). */
    private int wrapWidth = 0;
    /** Cached wrapped lines (invalidated when text or wrapWidth changes). */
    private List<String> wrappedLines;
    private boolean linesDirty = true;

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

    /**
     * Create a TextLabel from a Minecraft {@link Text} object.
     * This preserves formatting and uses the text's string value internally.
     *
     * @param x      absolute X
     * @param y      absolute Y
     * @param width  label width
     * @param height label height
     * @param richText the Minecraft Text
     */
    public TextLabel(int x, int y, int width, int height, Text richText) {
        this(x, y, width, height, richText,
             ProceduralRenderer.COL_TEXT_PRIMARY, Align.LEFT, 1.0f);
    }

    /**
     * Create a TextLabel from a Minecraft {@link Text} with appearance options.
     */
    public TextLabel(int x, int y, int width, int height, Text richText,
                     int color, Align align, float scale) {
        super(x, y, width, height);
        this.richText = richText;
        this.text = richText != null ? richText.getString() : "";
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

        // Multi-line / wrapped rendering
        if (wrapWidth > 0) {
            rebuildWrappedLines(tr);
            renderWrappedLines(ctx, tr);
            return;
        }

        // Single-line rendering
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
    //  Wrapping helpers
    // =====================================================================

    /**
     * Rebuild the cached wrapped-line list when text or wrapWidth changed.
     */
    private void rebuildWrappedLines(TextRenderer tr) {
        if (!linesDirty && wrappedLines != null) return;
        int effectiveWrap = (int) (wrapWidth / scale);
        wrappedLines = wrapText(text, tr, effectiveWrap);
        // Auto-adjust height to fit all lines
        int lineH = Math.round(tr.fontHeight * scale);
        this.height = Math.max(lineH, wrappedLines.size() * (lineH + 1));
        linesDirty = false;
    }

    /**
     * Simple word-wrap: splits at spaces, falls back to character-level
     * breaking for words wider than the wrap width.
     */
    private static List<String> wrapText(String text, TextRenderer tr, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (maxWidth <= 0 || text.isEmpty()) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ", -1);
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.isEmpty()) {
                // First word on the line
                if (tr.getWidth(word) > maxWidth) {
                    // Char-level break for oversized words
                    for (char c : word.toCharArray()) {
                        if (tr.getWidth(current.toString() + c) > maxWidth && !current.isEmpty()) {
                            lines.add(current.toString());
                            current = new StringBuilder();
                        }
                        current.append(c);
                    }
                } else {
                    current.append(word);
                }
            } else {
                String test = current + " " + word;
                if (tr.getWidth(test) > maxWidth) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current.append(' ').append(word);
                }
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Render pre-wrapped lines with proper alignment and vertical spacing.
     */
    private void renderWrappedLines(DrawContext ctx, TextRenderer tr) {
        int lineH = Math.round(tr.fontHeight * scale) + 1;
        int lineY = y;

        for (String line : wrappedLines) {
            if (scale != 1.0f) {
                switch (align) {
                    case LEFT   -> ProceduralRenderer.drawScaledText(
                            ctx, tr, line, x, lineY, color, scale);
                    case CENTER -> ProceduralRenderer.drawScaledCenteredText(
                            ctx, tr, line, x + width / 2, lineY, color, scale);
                    case RIGHT  -> {
                        int textW = Math.round(tr.getWidth(line) * scale);
                        ProceduralRenderer.drawScaledText(
                                ctx, tr, line, x + width - textW, lineY, color, scale);
                    }
                }
            } else {
                switch (align) {
                    case LEFT   -> ProceduralRenderer.drawText(
                            ctx, tr, line, x, lineY, color);
                    case CENTER -> ProceduralRenderer.drawCenteredText(
                            ctx, tr, line, x + width / 2, lineY, color);
                    case RIGHT  -> {
                        int textW = tr.getWidth(line);
                        ProceduralRenderer.drawText(
                                ctx, tr, line, x + width - textW, lineY, color);
                    }
                }
            }
            lineY += lineH;
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public String getText()           { return text; }

    public void setText(String t) {
        this.text = t != null ? t : "";
        this.richText = null;
        this.linesDirty = true;
    }

    /**
     * Set the label text from a Minecraft {@link Text} object.
     */
    public void setText(Text t) {
        this.richText = t;
        this.text = t != null ? t.getString() : "";
        this.linesDirty = true;
    }

    /** Get the Minecraft Text, or null if set via String. */
    public Text getRichText()         { return richText; }

    public int  getColor()            { return color; }
    public void setColor(int c)       { this.color = c; }
    public Align getAlign()           { return align; }
    public void setAlign(Align a)     { this.align = a; }
    public float getScale()           { return scale; }

    public void setScale(float s) {
        this.scale = s;
        this.linesDirty = true;
    }

    /**
     * Set the wrapping width in pixels. When positive, text wraps at
     * word boundaries. Set to 0 to disable wrapping.
     */
    public void setWrapWidth(int wrapWidth) {
        this.wrapWidth = Math.max(0, wrapWidth);
        this.linesDirty = true;
    }

    /** Get the current wrap width (0 = no wrapping). */
    public int getWrapWidth() { return wrapWidth; }

    /** Get the number of wrapped lines (1 if no wrapping). */
    public int getLineCount() {
        if (wrapWidth <= 0) return 1;
        if (linesDirty) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            rebuildWrappedLines(tr);
        }
        return wrappedLines != null ? wrappedLines.size() : 1;
    }
}
