package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pocketuicore.render.RenderContext;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Component Label Component
 * <p>
 * A Component-rendering component with left / centre / right alignment,
 * optional scaling, shadow rendering, and word-wrapping support.
 * <p>
 * Supports both plain {@link String} and Minecraft {@link Component} values.
 * When a {@code Component} is set, its formatted string representation is
 * used for rendering with the {@code Component} variant overloads.
 * <p>
 * <b>Wrapping:</b> Set {@link #setWrapWidth(int)} to a positive pixel
 * width and the label will automatically break Component into multiple lines.
 * The component's {@code height} is updated to fit all wrapped lines.
 * <p>
 * Pairs naturally with {@link com.pocketuicore.data.ObservableState}
 * for reactive Component updates:
 * <pre>{@code
 *     ObservableState<String> name = new ObservableState<>("Steve");
 *     TextLabel label = new TextLabel(10, 10, 100, 12, "Steve");
 *     name.addListener(label::setText);
 * }</pre>
 */
public class TextLabel extends UIComponent {

    /** Component alignment within the label bounds. */
    public enum Align { LEFT, CENTER, RIGHT }

    // ── State ────────────────────────────────────────────────────────────
    private String Component;
    private Component richText;
    private int color;
    private Align align;
    private float scale;

    /** Wrap width in pixels. 0 = no wrapping (single line). */
    private int wrapWidth = 0;
    /** Cached wrapped lines (invalidated when Component or wrapWidth changes). */
    private List<String> wrappedLines;
    private boolean linesDirty = true;

    // =====================================================================
    //  Construction
    // =====================================================================

    public TextLabel(int x, int y, int width, int height, String Component) {
        this(x, y, width, height, Component,
             ProceduralRenderer.COL_TEXT_PRIMARY, Align.LEFT, 1.0f);
    }

    public TextLabel(int x, int y, int width, int height, String Component, int color) {
        this(x, y, width, height, Component, color, Align.LEFT, 1.0f);
    }

    public TextLabel(int x, int y, int width, int height, String Component,
                     int color, Align align, float scale) {
        super(x, y, width, height);
        this.Component  = Component != null ? Component : "";
        this.color = color;
        this.align = align;
        this.scale = scale;
    }

    /**
     * Create a TextLabel from a Minecraft {@link Component} object.
     * This preserves formatting and uses the Component's string value internally.
     *
     * @param x      absolute X
     * @param y      absolute Y
     * @param width  label width
     * @param height label height
     * @param richText the Minecraft Component
     */
    public TextLabel(int x, int y, int width, int height, Component richText) {
        this(x, y, width, height, richText,
             ProceduralRenderer.COL_TEXT_PRIMARY, Align.LEFT, 1.0f);
    }

    /**
     * Create a TextLabel from a Minecraft {@link Component} with appearance options.
     */
    public TextLabel(int x, int y, int width, int height, Component richText,
                     int color, Align align, float scale) {
        super(x, y, width, height);
        this.richText = richText;
        this.Component = richText != null ? richText.getString() : "";
        this.color = color;
        this.align = align;
        this.scale = scale;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(RenderContext ctx, int mouseX, int mouseY, float delta) {
        if (Component.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        Font tr = client.font;

        // Multi-line / wrapped rendering
        if (wrapWidth > 0) {
            rebuildWrappedLines(tr);
            renderWrappedLines(ctx, tr);
            return;
        }

        // Single-line rendering
        int textY = y + (height - Math.round(tr.lineHeight * scale)) / 2;

        if (scale != 1.0f) {
            switch (align) {
                case LEFT   -> ProceduralRenderer.drawScaledText(
                        ctx, tr, Component, x, textY, color, scale);
                case CENTER -> ProceduralRenderer.drawScaledCenteredText(
                        ctx, tr, Component, x + width / 2, textY, color, scale);
                case RIGHT  -> {
                    int textW = Math.round(tr.width(Component) * scale);
                    ProceduralRenderer.drawScaledText(
                            ctx, tr, Component, x + width - textW, textY, color, scale);
                }
            }
        } else {
            switch (align) {
                case LEFT   -> ProceduralRenderer.drawText(
                        ctx, tr, Component, x, textY, color);
                case CENTER -> ProceduralRenderer.drawCenteredText(
                        ctx, tr, Component, x + width / 2, textY, color);
                case RIGHT  -> {
                    int textW = tr.width(Component);
                    ProceduralRenderer.drawText(
                            ctx, tr, Component, x + width - textW, textY, color);
                }
            }
        }
    }

    // =====================================================================
    //  Wrapping helpers
    // =====================================================================

    /**
     * Rebuild the cached wrapped-line list when Component or wrapWidth changed.
     */
    private void rebuildWrappedLines(Font tr) {
        if (!linesDirty && wrappedLines != null) return;
        int effectiveWrap = (int) (wrapWidth / scale);
        wrappedLines = wrapText(Component, tr, effectiveWrap);
        // Auto-adjust height to fit all lines
        int lineH = Math.round(tr.lineHeight * scale);
        this.height = Math.max(lineH, wrappedLines.size() * (lineH + 1));
        linesDirty = false;
    }

    /**
     * Simple word-wrap: splits at spaces, falls back to character-level
     * breaking for words wider than the wrap width.
     */
    private static List<String> wrapText(String Component, Font tr, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (maxWidth <= 0 || Component.isEmpty()) {
            lines.add(Component);
            return lines;
        }

        String[] words = Component.split(" ", -1);
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.isEmpty()) {
                // First word on the line
                if (tr.width(word) > maxWidth) {
                    // Char-level break for oversized words
                    for (char c : word.toCharArray()) {
                        if (tr.width(current.toString() + c) > maxWidth && !current.isEmpty()) {
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
                if (tr.width(test) > maxWidth) {
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
    private void renderWrappedLines(RenderContext ctx, Font tr) {
        int lineH = Math.round(tr.lineHeight * scale) + 1;
        int lineY = y;

        for (String line : wrappedLines) {
            if (scale != 1.0f) {
                switch (align) {
                    case LEFT   -> ProceduralRenderer.drawScaledText(
                            ctx, tr, line, x, lineY, color, scale);
                    case CENTER -> ProceduralRenderer.drawScaledCenteredText(
                            ctx, tr, line, x + width / 2, lineY, color, scale);
                    case RIGHT  -> {
                        int textW = Math.round(tr.width(line) * scale);
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
                        int textW = tr.width(line);
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

    public String getText()           { return Component; }

    public TextLabel setText(String t) {
        this.Component = t != null ? t : "";
        this.richText = null;
        this.linesDirty = true;
        return this;
    }

    /**
     * Set the label Component from a Minecraft {@link Component} object.
     */
    public TextLabel setText(Component t) {
        this.richText = t;
        this.Component = t != null ? t.getString() : "";
        this.linesDirty = true;
        return this;
    }

    /** Get the Minecraft Component, or null if set via String. */
    public Component getRichText()         { return richText; }

    public int  getColor()            { return color; }
    public TextLabel setColor(int c)       { this.color = c; return this; }
    public Align getAlign()           { return align; }
    public TextLabel setAlign(Align a)     { this.align = a; return this; }
    public float getScale()           { return scale; }

    public TextLabel setScale(float s) {
        this.scale = s;
        this.linesDirty = true;
        return this;
    }

    /**
     * Set the wrapping width in pixels. When positive, Component wraps at
     * word boundaries. Set to 0 to disable wrapping.
     */
    public TextLabel setWrapWidth(int wrapWidth) {
        this.wrapWidth = Math.max(0, wrapWidth);
        this.linesDirty = true;
        return this;
    }

    /** Get the current wrap width (0 = no wrapping). */
    public int getWrapWidth() { return wrapWidth; }

    /** Get the number of wrapped lines (1 if no wrapping). */
    public int getLineCount() {
        if (wrapWidth <= 0) return 1;
        if (linesDirty) {
            Font tr = Minecraft.getInstance().font;
            rebuildWrappedLines(tr);
        }
        return wrappedLines != null ? wrappedLines.size() : 1;
    }
}
