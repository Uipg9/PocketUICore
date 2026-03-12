package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Auto-Layout: Vertical List Panel
 * <p>
 * A {@link DarkPanel} that stacks its children vertically with consistent
 * spacing.  Optionally stretches each child's width to fill the panel.
 * <p>
 * If the total content height exceeds the panel height, the panel
 * becomes scrollable automatically. A scroll bar indicator is rendered
 * when scrollable.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     VerticalListPanel list = new VerticalListPanel(10, 10, 200, 400, 6, 4);
 *     list.addChild(item1);
 *     list.addChild(item2);
 *     list.addChild(item3);
 *     list.layout(); // stacks vertically with 4 px gaps
 * }</pre>
 */
public class VerticalListPanel extends DarkPanel {

    private int padding;
    private int spacing;
    private boolean stretchWidth;
    private boolean suppressLayout;
    private boolean showScrollBar = true;
    private int scrollBarWidth = 3;
    private int scrollBarColor = ProceduralRenderer.COL_TEXT_MUTED;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create a vertical list panel.
     *
     * @param padding inner padding from panel edges
     * @param spacing vertical gap between children
     */
    public VerticalListPanel(int x, int y, int width, int height,
                             int padding, int spacing) {
        super(x, y, width, height);
        this.padding      = padding;
        this.spacing       = spacing;
        this.stretchWidth  = true;
    }

    // =====================================================================
    //  Auto-layout on tree changes
    // =====================================================================

    @Override
    public UIComponent addChild(UIComponent child) {
        super.addChild(child);
        layout();
        return this;
    }

    @Override
    public void removeChild(UIComponent child) {
        super.removeChild(child);
        layout();
    }

    @Override
    public void clearChildren() {
        super.clearChildren();
        layout();
    }

    // =====================================================================
    //  Layout
    // =====================================================================

    /**
     * Recompute child positions.  Call after adding / removing children
     * or resizing the panel.
     * <p>
     * When {@link #isStretchWidth()} is {@code true} (default), each
     * child's width is set to {@code panelWidth - 2 * padding}.
     */
    public void layout() {
        if (suppressLayout || children.isEmpty()) return;

        int curY   = y + padding;
        int innerW = width - padding * 2;

        for (UIComponent child : children) {
            child.setPosition(x + padding, curY);
            if (stretchWidth) {
                child.setSize(innerW, child.getHeight());
            }
            curY += child.getHeight() + spacing;
        }

        // Total content height (subtract trailing spacing, add bottom padding)
        int totalHeight = (curY - spacing + padding) - y;
        if (totalHeight > height) {
            setScrollable(true, totalHeight);
        } else {
            setScrollable(false, height);
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public int  getPadding()                 { return padding; }
    public VerticalListPanel setPadding(int p)            { this.padding = p; return this; }
    public int  getSpacing()                 { return spacing; }
    public VerticalListPanel setSpacing(int s)            { this.spacing = s; return this; }
    public boolean isStretchWidth()          { return stretchWidth; }
    public VerticalListPanel setStretchWidth(boolean b)   { this.stretchWidth = b; return this; }

    /** Show or hide the scroll bar indicator. Default is {@code true}. @since 1.15.0 */
    public VerticalListPanel setShowScrollBar(boolean b)  { this.showScrollBar = b; return this; }
    public boolean isShowScrollBar()         { return showScrollBar; }

    /** Set scroll bar width in pixels. Default is 3. @since 1.15.0 */
    public VerticalListPanel setScrollBarWidth(int w)     { this.scrollBarWidth = Math.max(1, w); return this; }
    public int getScrollBarWidth()           { return scrollBarWidth; }

    /** Set scroll bar color. @since 1.15.0 */
    public VerticalListPanel setScrollBarColor(int c)     { this.scrollBarColor = c; return this; }
    public int getScrollBarColor()           { return scrollBarColor; }

    // =====================================================================
    //  Scroll bar rendering
    // =====================================================================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        // Render scroll bar on top, outside scissor region
        if (showScrollBar && isScrollable() && getContentHeight() > getHeight()) {
            int contentH = getContentHeight();
            float ratio = (float) getHeight() / contentH;
            int barH = Math.max(10, (int) (getHeight() * ratio));
            int maxScroll = contentH - getHeight();
            float scrollFrac = maxScroll > 0 ? (float) getScrollOffset() / maxScroll : 0;
            int barY = getY() + (int) ((getHeight() - barH) * scrollFrac);
            int barX = getX() + getWidth() - scrollBarWidth - 1;
            ProceduralRenderer.fillRoundedRect(ctx, barX, barY, scrollBarWidth, barH,
                    scrollBarWidth / 2, scrollBarColor);
        }
    }

    /**
     * Suppress automatic {@link #layout()} calls during bulk child operations.
     * Remember to call {@link #layout()} manually after re-enabling.
     *
     * @param suppress {@code true} to suppress, {@code false} to re-enable
     * @since 1.13.0
     */
    public VerticalListPanel setSuppressLayout(boolean suppress) {
        this.suppressLayout = suppress;
        return this;
    }
    public boolean isSuppressLayout() { return suppressLayout; }
}
