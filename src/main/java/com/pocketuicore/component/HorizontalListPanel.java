package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;

/**
 * A horizontal layout container that arranges children left-to-right
 * with configurable padding and spacing.
 * <p>
 * Mirrors {@link VerticalListPanel} but along the X axis. When
 * {@link #isStretchHeight()} is {@code true} (default), each child
 * is resized to fill the panel's inner height.
 *
 * @since 1.12.0
 */
public class HorizontalListPanel extends DarkPanel {

    private int padding  = 6;
    private int spacing  = 4;
    private boolean stretchHeight = true;

    public HorizontalListPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // ── Tree hooks ───────────────────────────────────────────────────────

    @Override
    public void addChild(UIComponent child) {
        super.addChild(child);
        layout();
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
     * Recompute child positions left-to-right.
     */
    public void layout() {
        if (children.isEmpty()) return;

        int curX   = x + padding;
        int innerH = height - padding * 2;

        for (UIComponent child : children) {
            child.setPosition(curX, y + padding);
            if (stretchHeight) {
                child.setSize(child.getWidth(), innerH);
            }
            curX += child.getWidth() + spacing;
        }

        // Total content width (subtract trailing spacing, add right padding)
        int totalWidth = (curX - spacing + padding) - x;
        if (totalWidth > width) {
            setScrollableH(true, totalWidth);
        } else {
            setScrollableH(false, width);
        }
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public int  getPadding()                  { return padding; }
    public HorizontalListPanel setPadding(int p)           { this.padding = p; return this; }
    public int  getSpacing()                  { return spacing; }
    public HorizontalListPanel setSpacing(int s)           { this.spacing = s; return this; }
    public boolean isStretchHeight()          { return stretchHeight; }
    public HorizontalListPanel setStretchHeight(boolean b) { this.stretchHeight = b; return this; }
}
