package com.pocketuicore.component;

/**
 * Anchor Layout — Constraint-based positioning for UI components.
 * <p>
 * Allows anchoring a child component to the edges or centre of its parent,
 * with optional margin offsets.  This provides a declarative way to position
 * components relative to their parent's bounds.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     DarkPanel parent = new DarkPanel(0, 0, 400, 300);
 *     HoverButton btn = new HoverButton(0, 0, 80, 24, "OK", () -> {});
 *
 *     // Anchor to bottom-right with 10px margin:
 *     AnchorLayout.anchor(btn, parent, Anchor.BOTTOM_RIGHT, 10, 10);
 *
 *     // Centre horizontally, anchor to top:
 *     AnchorLayout.anchor(btn, parent, Anchor.TOP_CENTER, 0, 8);
 *
 *     // Fill parent with margins:
 *     AnchorLayout.fill(btn, parent, 10);
 * }</pre>
 *
 * @since 1.13.0
 */
public final class AnchorLayout {

    private AnchorLayout() { /* static utility */ }

    /** Anchor positions within a parent component. */
    public enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT
    }

    /**
     * Position a child component relative to a parent using an anchor point.
     *
     * @param child   the component to position
     * @param parent  the reference parent component
     * @param anchor  the anchor point within the parent
     * @param marginX horizontal margin from the anchor edge
     * @param marginY vertical margin from the anchor edge
     */
    public static void anchor(UIComponent child, UIComponent parent,
                               Anchor anchor, int marginX, int marginY) {
        int px = parent.getX();
        int py = parent.getY();
        int pw = parent.getWidth();
        int ph = parent.getHeight();
        int cw = child.getWidth();
        int ch = child.getHeight();

        int cx, cy;
        switch (anchor) {
            case TOP_LEFT -> {
                cx = px + marginX;
                cy = py + marginY;
            }
            case TOP_CENTER -> {
                cx = px + (pw - cw) / 2 + marginX;
                cy = py + marginY;
            }
            case TOP_RIGHT -> {
                cx = px + pw - cw - marginX;
                cy = py + marginY;
            }
            case CENTER_LEFT -> {
                cx = px + marginX;
                cy = py + (ph - ch) / 2 + marginY;
            }
            case CENTER -> {
                cx = px + (pw - cw) / 2 + marginX;
                cy = py + (ph - ch) / 2 + marginY;
            }
            case CENTER_RIGHT -> {
                cx = px + pw - cw - marginX;
                cy = py + (ph - ch) / 2 + marginY;
            }
            case BOTTOM_LEFT -> {
                cx = px + marginX;
                cy = py + ph - ch - marginY;
            }
            case BOTTOM_CENTER -> {
                cx = px + (pw - cw) / 2 + marginX;
                cy = py + ph - ch - marginY;
            }
            case BOTTOM_RIGHT -> {
                cx = px + pw - cw - marginX;
                cy = py + ph - ch - marginY;
            }
            default -> {
                cx = px + marginX;
                cy = py + marginY;
            }
        }

        child.setPosition(cx, cy);
    }

    /**
     * Position a child at the given anchor with zero margins.
     */
    public static void anchor(UIComponent child, UIComponent parent, Anchor anchor) {
        anchor(child, parent, anchor, 0, 0);
    }

    /**
     * Resize and position a child to fill its parent with a uniform margin.
     *
     * @param child  the component to fill
     * @param parent the reference parent
     * @param margin margin on all four sides
     */
    public static void fill(UIComponent child, UIComponent parent, int margin) {
        child.setPosition(parent.getX() + margin, parent.getY() + margin);
        child.setSize(parent.getWidth() - margin * 2, parent.getHeight() - margin * 2);
    }

    /**
     * Resize and position a child to fill its parent with separate margins.
     *
     * @param child   the component to fill
     * @param parent  the reference parent
     * @param left    left margin
     * @param top     top margin
     * @param right   right margin
     * @param bottom  bottom margin
     */
    public static void fill(UIComponent child, UIComponent parent,
                             int left, int top, int right, int bottom) {
        child.setPosition(parent.getX() + left, parent.getY() + top);
        child.setSize(parent.getWidth() - left - right, parent.getHeight() - top - bottom);
    }

    /**
     * Centre a child within its parent (both axes).
     */
    public static void center(UIComponent child, UIComponent parent) {
        anchor(child, parent, Anchor.CENTER, 0, 0);
    }

    /**
     * Centre a child horizontally within its parent at a given Y offset.
     *
     * @param yOffset vertical offset from parent top
     */
    public static void centerHorizontally(UIComponent child, UIComponent parent, int yOffset) {
        int cx = parent.getX() + (parent.getWidth() - child.getWidth()) / 2;
        child.setPosition(cx, parent.getY() + yOffset);
    }

    /**
     * Centre a child vertically within its parent at a given X offset.
     *
     * @param xOffset horizontal offset from parent left
     */
    public static void centerVertically(UIComponent child, UIComponent parent, int xOffset) {
        int cy = parent.getY() + (parent.getHeight() - child.getHeight()) / 2;
        child.setPosition(parent.getX() + xOffset, cy);
    }

    /**
     * Distribute children evenly across the horizontal axis of a parent.
     *
     * @param children the components to distribute
     * @param parent   the reference parent
     * @param marginY  vertical margin from parent top
     */
    public static void distributeHorizontally(java.util.List<UIComponent> children,
                                               UIComponent parent, int marginY) {
        if (children.isEmpty()) return;
        int totalW = 0;
        for (UIComponent c : children) totalW += c.getWidth();
        int gap = (parent.getWidth() - totalW) / (children.size() + 1);
        int curX = parent.getX() + gap;
        for (UIComponent c : children) {
            c.setPosition(curX, parent.getY() + marginY);
            curX += c.getWidth() + gap;
        }
    }

    /**
     * Distribute children evenly across the vertical axis of a parent.
     *
     * @param children the components to distribute
     * @param parent   the reference parent
     * @param marginX  horizontal margin from parent left
     */
    public static void distributeVertically(java.util.List<UIComponent> children,
                                             UIComponent parent, int marginX) {
        if (children.isEmpty()) return;
        int totalH = 0;
        for (UIComponent c : children) totalH += c.getHeight();
        int gap = (parent.getHeight() - totalH) / (children.size() + 1);
        int curY = parent.getY() + gap;
        for (UIComponent c : children) {
            c.setPosition(parent.getX() + marginX, curY);
            curY += c.getHeight() + gap;
        }
    }
}
