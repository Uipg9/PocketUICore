package com.pocketuicore.component;

/**
 * Auto-Layout: Grid Panel
 * <p>
 * A {@link DarkPanel} that automatically arranges its children in a
 * uniform grid.  Specify the number of columns and the padding, and
 * child positions are computed automatically in {@link #layout()}.
 * <p>
 * Children are placed left-to-right, top-to-bottom.  Each cell has
 * equal width; cell height is taken from the tallest child in each row.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     GridPanel grid = new GridPanel(10, 10, 300, 200, 3, 6);
 *     grid.addChild(buttonA);
 *     grid.addChild(buttonB);
 *     grid.addChild(buttonC);
 *     grid.addChild(buttonD);
 *     grid.layout(); // positions A,B,C in row 1; D in row 2
 * }</pre>
 */
public class GridPanel extends DarkPanel {

    private int columns;
    private int padding;
    private int cellSpacingX;
    private int cellSpacingY;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create a grid panel.
     *
     * @param columns number of columns in the grid (clamped to &ge; 1)
     * @param padding inner padding from panel edges to the first cell
     */
    public GridPanel(int x, int y, int width, int height,
                     int columns, int padding) {
        super(x, y, width, height);
        this.columns      = Math.max(1, columns);
        this.padding       = padding;
        this.cellSpacingX  = 4;
        this.cellSpacingY  = 4;
    }

    // =====================================================================
    //  Layout
    // =====================================================================

    /**
     * Recompute child positions based on the current column count and
     * padding.  Call after adding / removing children or resizing the panel.
     * <p>
     * Each child's width is set to the computed cell width; height is
     * left untouched.  If the total content height exceeds the panel
     * height the panel becomes scrollable automatically.
     */
    public void layout() {
        if (children.isEmpty()) return;

        int innerW        = width - padding * 2;
        int totalSpacingX = cellSpacingX * (columns - 1);
        int cellW         = (innerW - totalSpacingX) / columns;

        int col       = 0;
        int curY      = y + padding;
        int rowHeight = 0;

        for (UIComponent child : children) {
            int cellX = x + padding + col * (cellW + cellSpacingX);
            child.setPosition(cellX, curY);
            child.setSize(cellW, child.getHeight());

            rowHeight = Math.max(rowHeight, child.getHeight());

            col++;
            if (col >= columns) {
                col = 0;
                curY += rowHeight + cellSpacingY;
                rowHeight = 0;
            }
        }

        // Account for the last (potentially partial) row
        int totalHeight = (curY + rowHeight + padding) - y;
        if (totalHeight > height) {
            setScrollable(true, totalHeight);
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public int  getColumns()               { return columns; }
    public void setColumns(int c)          { this.columns = Math.max(1, c); }
    public int  getPadding()               { return padding; }
    public void setPadding(int p)          { this.padding = p; }
    public int  getCellSpacingX()          { return cellSpacingX; }
    public void setCellSpacingX(int s)     { this.cellSpacingX = s; }
    public int  getCellSpacingY()          { return cellSpacingY; }
    public void setCellSpacingY(int s)     { this.cellSpacingY = s; }
}
