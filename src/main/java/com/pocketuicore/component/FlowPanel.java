package com.pocketuicore.component;

/**
 * A wrapping flow layout that places children left-to-right, advancing
 * to the next row when the current row would exceed the panel width.
 * <p>
 * Useful for tag lists, icon grids with variable sizes, or any layout
 * where items should wrap naturally.
 *
 * @since 1.12.0
 */
public class FlowPanel extends DarkPanel {

    private int padding  = 6;
    private int spacingX = 4;
    private int spacingY = 4;

    public FlowPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // ── Tree hooks ───────────────────────────────────────────────────────

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
     * Recompute child positions with wrapping.
     */
    public void layout() {
        if (children.isEmpty()) return;

        int innerW = width - padding * 2;
        int curX   = x + padding;
        int curY   = y + padding;
        int rowH   = 0;

        for (UIComponent child : children) {
            int cw = child.getWidth();
            int ch = child.getHeight();

            // Wrap to next row if this child would exceed the panel width
            if (curX + cw > x + width - padding && curX != x + padding) {
                curX  = x + padding;
                curY += rowH + spacingY;
                rowH  = 0;
            }

            child.setPosition(curX, curY);
            curX += cw + spacingX;
            rowH = Math.max(rowH, ch);
        }

        int totalHeight = (curY + rowH + padding) - y;
        if (totalHeight > height) {
            setScrollable(true, totalHeight);
        } else {
            setScrollable(false, height);
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public int  getPadding()              { return padding; }
    public FlowPanel setPadding(int p)    { this.padding = p; return this; }
    public int  getSpacingX()             { return spacingX; }
    public FlowPanel setSpacingX(int s)   { this.spacingX = s; return this; }
    public int  getSpacingY()             { return spacingY; }
    public FlowPanel setSpacingY(int s)   { this.spacingY = s; return this; }
}
