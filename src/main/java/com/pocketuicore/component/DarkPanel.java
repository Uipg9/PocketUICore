package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Module 2 — DarkPanel container
 * <p>
 * A rounded, shadowed, dark-mode panel that acts as a container for child
 * components. The panel itself renders:
 * <ol>
 *   <li>Drop shadow (optional)</li>
 *   <li>Background fill (rounded rect)</li>
 *   <li>Border (optional, 1 px rounded)</li>
 *   <li>All children</li>
 * </ol>
 */
public class DarkPanel extends UIComponent {

    // ── Appearance ───────────────────────────────────────────────────────
    private int backgroundColor;
    private int borderColor;
    private int cornerRadius;
    private boolean drawBorder;
    private boolean drawShadow;
    private int shadowLayers;
    private int shadowAlpha;

    // ── Scroll support ───────────────────────────────────────────────────
    private int scrollOffset = 0;
    private int contentHeight = 0;  // set externally if content exceeds panel
    private boolean scrollable = false;

    // =====================================================================
    //  Builders
    // =====================================================================

    /**
     * Construct a DarkPanel with the library's default surface colours.
     */
    public DarkPanel(int x, int y, int width, int height) {
        this(x, y, width, height,
             ProceduralRenderer.COL_BG_SURFACE,
             ProceduralRenderer.COL_BORDER,
             6, true, true);
    }

    /**
     * Full constructor.
     */
    public DarkPanel(int x, int y, int width, int height,
                     int bgColor, int borderColor, int cornerRadius,
                     boolean drawBorder, boolean drawShadow) {
        super(x, y, width, height);
        this.backgroundColor = bgColor;
        this.borderColor     = borderColor;
        this.cornerRadius    = cornerRadius;
        this.drawBorder      = drawBorder;
        this.drawShadow      = drawShadow;
        this.shadowLayers    = 6;
        this.shadowAlpha     = 102; // ~40 %
        this.blockInputInBounds = true; // DarkPanel blocks all input within bounds
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Shadow
        if (drawShadow) {
            ProceduralRenderer.drawDropShadow(ctx, x, y, width, height,
                    cornerRadius, shadowLayers, shadowAlpha);
        }
        // Background
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height,
                cornerRadius, backgroundColor);
        // Border
        if (drawBorder) {
            ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height,
                    cornerRadius, borderColor);
        }
    }

    /**
     * Overridden to apply scissoring and scroll offset when the panel is
     * scrollable.
     */
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        renderSelf(ctx, mouseX, mouseY, delta);

        if (scrollable) {
            ctx.enableScissor(x, y, x + width, y + height);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(0, -scrollOffset);
            for (UIComponent child : children) {
                child.render(ctx, mouseX, mouseY + scrollOffset, delta);
            }
            ctx.getMatrices().popMatrix();
            ctx.disableScissor();
        } else {
            for (UIComponent child : children) {
                child.render(ctx, mouseX, mouseY, delta);
            }
        }
    }

    // =====================================================================
    //  Input — strict Z-index blocking + scroll-offset adjustment
    // =====================================================================

    /**
     * Forwards to children with scroll-adjusted coordinates, then blocks
     * input within panel bounds to prevent click-through.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        double adjustedY = scrollable ? mouseY + scrollOffset : mouseY;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseClicked(mouseX, adjustedY, button)) return true;
        }
        return isHovered(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        double adjustedY = scrollable ? mouseY + scrollOffset : mouseY;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseReleased(mouseX, adjustedY, button)) return true;
        }
        return isHovered(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!visible || !enabled) return false;
        double adjustedY = scrollable ? mouseY + scrollOffset : mouseY;
        // Children get first chance at the scroll event
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseScrolled(mouseX, adjustedY, hAmount, vAmount)) return true;
        }
        // Own scroll handling
        if (scrollable && isHovered(mouseX, mouseY)) {
            scrollOffset -= (int) (vAmount * 20);
            int maxScroll = Math.max(0, contentHeight - height);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return isHovered(mouseX, mouseY);
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public void setBackgroundColor(int c) { this.backgroundColor = c; }
    public void setBorderColor(int c)     { this.borderColor = c; }
    public void setCornerRadius(int r)    { this.cornerRadius = r; }
    public void setDrawBorder(boolean b)  { this.drawBorder = b; }
    public void setDrawShadow(boolean b)  { this.drawShadow = b; }
    public void setShadow(int layers, int maxAlpha) {
        this.shadowLayers = layers;
        this.shadowAlpha  = maxAlpha;
    }

    public void setScrollable(boolean scrollable, int contentHeight) {
        this.scrollable    = scrollable;
        this.contentHeight = contentHeight;
        // Clamp existing offset to the new bounds
        int max = Math.max(0, contentHeight - height);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, max));
    }

    public int getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(int offset) {
        int max = Math.max(0, contentHeight - height);
        this.scrollOffset = Math.max(0, Math.min(offset, max));
    }

    /**
     * Scroll by a continuous amount (for analog controller sticks).
     * Positive values scroll down, negative scroll up.
     *
     * @param amount scroll delta in GUI pixels
     */
    public void scrollBy(double amount) {
        if (!scrollable) return;
        int max = Math.max(0, contentHeight - height);
        this.scrollOffset = Math.max(0, Math.min(
                this.scrollOffset + (int) amount, max));
    }

    /** @return {@code true} if this panel is currently scrollable. */
    public boolean isScrollable() { return scrollable; }

    /** @return total content height (0 if not scrollable). */
    public int getContentHeight() { return contentHeight; }
}
