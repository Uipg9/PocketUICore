package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Rich Tooltip — Builder-based multi-line tooltips with per-line colours,
 * bold/scaled title, horizontal separators, and word wrapping.
 * <p>
 * Unlike the basic {@link UIComponent#setTooltip(String...)} system which
 * renders plain white text, RichTooltip allows full formatting control.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     RichTooltip tooltip = RichTooltip.builder()
 *             .title("Wheat Field Lv.3")
 *             .titleColor(ProceduralRenderer.COL_ACCENT_TEAL)
 *             .separator()
 *             .line("Yield: 15/tick", ProceduralRenderer.COL_SUCCESS)
 *             .line("Growth: 85%", ProceduralRenderer.COL_WARNING)
 *             .separator()
 *             .line("Click to harvest", ProceduralRenderer.COL_TEXT_MUTED)
 *             .maxWidth(200)
 *             .build();
 *
 *     // Attach to a component:
 *     myButton.setRichTooltip(tooltip);
 *
 *     // Or render manually:
 *     RichTooltip.renderTooltip(ctx, tooltip, mouseX, mouseY);
 * }</pre>
 */
public final class RichTooltip {

    // ── Line types ───────────────────────────────────────────────────────

    private sealed interface TooltipEntry {
        int getHeight(TextRenderer tr);
    }

    private record TitleLine(String text, int color, float scale) implements TooltipEntry {
        @Override public int getHeight(TextRenderer tr) {
            return Math.round(tr.fontHeight * scale) + 2;
        }
    }

    private record TextLine(String text, int color) implements TooltipEntry {
        @Override public int getHeight(TextRenderer tr) {
            return tr.fontHeight + 2;
        }
    }

    private record SeparatorLine(int color) implements TooltipEntry {
        @Override public int getHeight(TextRenderer tr) {
            return 5; // 2px padding + 1px line + 2px padding
        }
    }

    // ── Data ─────────────────────────────────────────────────────────────
    private final List<TooltipEntry> entries;
    private final int maxWidth;

    private RichTooltip(List<TooltipEntry> entries, int maxWidth) {
        this.entries  = entries;
        this.maxWidth = maxWidth;
    }

    // =====================================================================
    //  Builder
    // =====================================================================

    /**
     * Create a new RichTooltip builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<TooltipEntry> entries = new ArrayList<>();
        private int maxWidth = 250;

        private Builder() {}

        /**
         * Add a title line (bold, scaled, coloured).
         *
         * @param text the title text
         * @return this builder
         */
        public Builder title(String text) {
            return title(text, ProceduralRenderer.COL_TEXT_PRIMARY);
        }

        /**
         * Add a title line with a custom colour.
         *
         * @param text  the title text
         * @param color ARGB colour
         * @return this builder
         */
        public Builder title(String text, int color) {
            return title(text, color, 1.0f);
        }

        /**
         * Set the title colour for the most recently added title.
         *
         * @param color ARGB colour
         * @return this builder
         */
        public Builder titleColor(int color) {
            // Rewrite the last entry if it's a title
            for (int i = entries.size() - 1; i >= 0; i--) {
                if (entries.get(i) instanceof TitleLine tl) {
                    entries.set(i, new TitleLine(tl.text(), color, tl.scale()));
                    break;
                }
            }
            return this;
        }

        /**
         * Add a title line with a custom colour and scale.
         *
         * @param text  the title text
         * @param color ARGB colour
         * @param scale text scale (1.0 = normal, 1.2 = slightly larger)
         * @return this builder
         */
        public Builder title(String text, int color, float scale) {
            entries.add(new TitleLine(text, color, scale));
            return this;
        }

        /**
         * Add a text line with default colour.
         *
         * @param text the line text
         * @return this builder
         */
        public Builder line(String text) {
            return line(text, ProceduralRenderer.COL_TEXT_PRIMARY);
        }

        /**
         * Add a text line with a custom colour.
         *
         * @param text  the line text
         * @param color ARGB colour
         * @return this builder
         */
        public Builder line(String text, int color) {
            entries.add(new TextLine(text, color));
            return this;
        }

        /**
         * Add a horizontal separator line.
         *
         * @return this builder
         */
        public Builder separator() {
            return separator(ProceduralRenderer.COL_BORDER);
        }

        /**
         * Add a coloured horizontal separator line.
         *
         * @param color ARGB colour
         * @return this builder
         */
        public Builder separator(int color) {
            entries.add(new SeparatorLine(color));
            return this;
        }

        /**
         * Set the maximum width for word wrapping. Default is 250.
         *
         * @param maxWidth maximum width in pixels
         * @return this builder
         */
        public Builder maxWidth(int maxWidth) {
            this.maxWidth = Math.max(50, maxWidth);
            return this;
        }

        /**
         * Build the RichTooltip.
         *
         * @return the built tooltip
         */
        public RichTooltip build() {
            return new RichTooltip(new ArrayList<>(entries), maxWidth);
        }
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    /**
     * Render this RichTooltip at the given mouse position.
     *
     * @param ctx    the DrawContext
     * @param mouseX mouse X coordinate
     * @param mouseY mouse Y coordinate
     */
    public static void renderTooltip(DrawContext ctx, RichTooltip tooltip,
                                      int mouseX, int mouseY) {
        if (tooltip == null || tooltip.entries.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int pad = 5;

        // Expand text lines that exceed maxWidth via word wrap
        List<TooltipEntry> expanded = expandEntries(tooltip, tr);

        // Calculate dimensions
        int maxW = 0;
        int totalH = 0;
        for (TooltipEntry entry : expanded) {
            totalH += entry.getHeight(tr);
            if (entry instanceof TitleLine tl) {
                maxW = Math.max(maxW, Math.round(tr.getWidth(tl.text()) * tl.scale()));
            } else if (entry instanceof TextLine tl) {
                maxW = Math.max(maxW, tr.getWidth(tl.text()));
            }
        }

        int boxW = maxW + pad * 2;
        int boxH = totalH + pad * 2;

        // Position near cursor, clamped to screen
        int bx = mouseX + 12;
        int by = mouseY - 12;
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        if (bx + boxW > sw) bx = mouseX - boxW - 4;
        if (by + boxH > sh) by = sh - boxH;
        if (by < 0) by = 0;

        // Background + border
        ProceduralRenderer.fillRoundedRect(ctx, bx, by, boxW, boxH,
                4, 0xF0100020);
        ProceduralRenderer.drawRoundedBorder(ctx, bx, by, boxW, boxH,
                4, ProceduralRenderer.COL_ACCENT);

        // Render entries
        int ty = by + pad;
        for (TooltipEntry entry : expanded) {
            if (entry instanceof TitleLine tl) {
                if (tl.scale() != 1.0f) {
                    ProceduralRenderer.drawScaledText(ctx, tr, tl.text(),
                            bx + pad, ty, tl.color(), tl.scale());
                } else {
                    ProceduralRenderer.drawText(ctx, tr, tl.text(),
                            bx + pad, ty, tl.color());
                }
            } else if (entry instanceof TextLine tl) {
                ProceduralRenderer.drawText(ctx, tr, tl.text(),
                        bx + pad, ty, tl.color());
            } else if (entry instanceof SeparatorLine sl) {
                int sepY = ty + 2;
                ProceduralRenderer.drawDivider(ctx, bx + pad, sepY,
                        boxW - pad * 2, sl.color());
            }
            ty += entry.getHeight(tr);
        }
    }

    /**
     * Expand text lines that exceed the max width via word wrapping.
     */
    private static List<TooltipEntry> expandEntries(RichTooltip tooltip, TextRenderer tr) {
        List<TooltipEntry> result = new ArrayList<>();
        for (TooltipEntry entry : tooltip.entries) {
            if (entry instanceof TextLine tl) {
                int textWidth = tr.getWidth(tl.text());
                if (textWidth > tooltip.maxWidth && tooltip.maxWidth > 0) {
                    for (String wrapped : wrapText(tl.text(), tr, tooltip.maxWidth)) {
                        result.add(new TextLine(wrapped, tl.color()));
                    }
                } else {
                    result.add(tl);
                }
            } else if (entry instanceof TitleLine tl) {
                int textWidth = Math.round(tr.getWidth(tl.text()) * tl.scale());
                if (textWidth > tooltip.maxWidth && tooltip.maxWidth > 0) {
                    int effectiveMax = (int) (tooltip.maxWidth / tl.scale());
                    for (String wrapped : wrapText(tl.text(), tr, effectiveMax)) {
                        result.add(new TitleLine(wrapped, tl.color(), tl.scale()));
                    }
                } else {
                    result.add(tl);
                }
            } else {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Simple word wrap.
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
                current.append(word);
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
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    /** @return the number of entries in this tooltip. */
    public int getEntryCount() { return entries.size(); }

    /** @return true if this tooltip has no entries. */
    public boolean isEmpty() { return entries.isEmpty(); }
}
