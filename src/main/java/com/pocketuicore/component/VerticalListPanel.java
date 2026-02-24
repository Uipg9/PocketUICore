package com.pocketuicore.component;

/**
 * Auto-Layout: Vertical List Panel
 * <p>
 * A {@link DarkPanel} that stacks its children vertically with consistent
 * spacing.  Optionally stretches each child's width to fill the panel.
 * <p>
 * If the total content height exceeds the panel height, the panel
 * becomes scrollable automatically.
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
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public int  getPadding()                 { return padding; }
    public void setPadding(int p)            { this.padding = p; }
    public int  getSpacing()                 { return spacing; }
    public void setSpacing(int s)            { this.spacing = s; }
    public boolean isStretchWidth()          { return stretchWidth; }
    public void setStretchWidth(boolean b)   { this.stretchWidth = b; }
}
