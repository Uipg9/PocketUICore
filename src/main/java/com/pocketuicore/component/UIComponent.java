package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Module 2 — Component Tree API: Base class
 * <p>
 * Every UI element in the Pocket system extends {@code UIComponent}.
 * Components form a logical tree (parent → children) and support hit-testing,
 * visibility toggling, and per-frame rendering.
 * <p>
 * Coordinates are <b>absolute screen pixels</b>. Layout helpers that resolve
 * relative offsets can be built on top — this base layer stays simple and fast.
 */
public abstract class UIComponent {

    // ── Geometry ─────────────────────────────────────────────────────────
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    // ── State ────────────────────────────────────────────────────────────
    protected boolean visible = true;
    protected boolean enabled = true;
    /**
     * When {@code true}, this component consumes all input events within its
     * bounds even if no child handles them — preventing click-through to
     * siblings rendered behind it (strict Z-index blocking).
     */
    protected boolean blockInputInBounds = false;

    // ── Tooltip ──────────────────────────────────────────────────────────
    private String[] tooltipLines;
    private long tooltipHoverStartNanos;
    private boolean tooltipWasHovered;
    /** Tooltip hover delay in nanoseconds (default 0.5 s). */
    private static final long TOOLTIP_DELAY_NS = 500_000_000L;

    // ── Tree ─────────────────────────────────────────────────────────────
    protected UIComponent parent;
    protected final List<UIComponent> children = new ArrayList<>();

    // =====================================================================
    //  Construction
    // =====================================================================

    protected UIComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    /**
     * Draw this component and all visible children.
     * Implementations should call {@code super.render()} <b>after</b>
     * drawing themselves so children paint on top.
     */
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        renderSelf(ctx, mouseX, mouseY, delta);
        updateTooltipHover(mouseX, mouseY);
        for (UIComponent child : children) {
            child.render(ctx, mouseX, mouseY, delta);
        }
    }

    /**
     * Override this instead of {@link #render} to draw only <em>this</em>
     * component's visuals. Children are rendered automatically afterwards.
     */
    protected abstract void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta);

    // =====================================================================
    //  Input
    // =====================================================================

    /**
     * Forward a mouse-click to this component tree.
     *
     * @return {@code true} if the click was consumed.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        // Children get first crack (back-to-front / highest Z first)
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        // Strict Z-index blocking: consume the event if within bounds
        if (blockInputInBounds && isHovered(mouseX, mouseY)) return true;
        return false;
    }

    /**
     * Forward a mouse-release.
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseReleased(mouseX, mouseY, button)) return true;
        }
        if (blockInputInBounds && isHovered(mouseX, mouseY)) return true;
        return false;
    }

    /**
     * Forward a mouse-scroll.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!visible || !enabled) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
        }
        if (blockInputInBounds && isHovered(mouseX, mouseY)) return true;
        return false;
    }

    // =====================================================================
    //  Hit-testing
    // =====================================================================

    /**
     * Point-in-rect hit test using the component's absolute bounds.
     */
    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width
            && mouseY >= y && mouseY < y + height;
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return isHovered((int) mouseX, (int) mouseY);
    }

    // =====================================================================
    //  Tree manipulation
    // =====================================================================

    public void addChild(UIComponent child) {
        child.parent = this;
        children.add(child);
    }

    public void removeChild(UIComponent child) {
        children.remove(child);
        if (child.parent == this) child.parent = null;
    }

    public void clearChildren() {
        for (UIComponent c : children) c.parent = null;
        children.clear();
    }

    public List<UIComponent> getChildren() {
        return Collections.unmodifiableList(children);
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public int getX()       { return x; }
    public int getY()       { return y; }
    public int getWidth()   { return width; }
    public int getHeight()  { return height; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setSize(int w, int h)     { this.width = w; this.height = h; }
    public void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }

    public boolean isBlockInputInBounds() { return blockInputInBounds; }
    public void setBlockInputInBounds(boolean b) { this.blockInputInBounds = b; }

    // =====================================================================
    //  Tooltip API
    // =====================================================================

    /**
     * Set tooltip lines for this component.  The tooltip is displayed
     * after the mouse hovers over the component for 0.5 seconds.
     *
     * @param lines one or more lines of tooltip text (pass {@code null}
     *              or empty to remove)
     */
    public void setTooltip(String... lines) {
        this.tooltipLines = (lines != null && lines.length > 0) ? lines : null;
    }

    /** @return the tooltip lines, or {@code null} if none. */
    public String[] getTooltip() { return tooltipLines; }

    /** @return {@code true} if this component has tooltip text. */
    public boolean hasTooltip() {
        return tooltipLines != null && tooltipLines.length > 0;
    }

    /** Track how long the mouse has been hovering over this component. */
    private void updateTooltipHover(int mouseX, int mouseY) {
        boolean hov = isHovered(mouseX, mouseY);
        if (hov && !tooltipWasHovered) {
            tooltipHoverStartNanos = System.nanoTime();
        }
        tooltipWasHovered = hov;
    }

    // =====================================================================
    //  Static tooltip renderer — call AFTER all components have rendered
    // =====================================================================

    /**
     * Walk an entire component tree, find the deepest hovered component
     * that has a tooltip, and render the tooltip box if the hover delay
     * has elapsed.
     * <p>
     * Call this method <b>at the very end</b> of your screen's render
     * method so the tooltip paints on top of everything.
     *
     * @param ctx    the DrawContext
     * @param root   the root UIComponent tree
     * @param mouseX current mouse X
     * @param mouseY current mouse Y
     */
    public static void renderTooltip(DrawContext ctx, UIComponent root,
                                      int mouseX, int mouseY) {
        UIComponent target = findDeepestTooltipTarget(root, mouseX, mouseY);
        if (target == null) return;

        long elapsed = System.nanoTime() - target.tooltipHoverStartNanos;
        if (elapsed < TOOLTIP_DELAY_NS) return;

        drawTooltipBox(ctx, target.tooltipLines, mouseX, mouseY);
    }

    private static UIComponent findDeepestTooltipTarget(UIComponent comp,
                                                         int mx, int my) {
        if (!comp.visible || !comp.isHovered(mx, my)) return null;
        // Children in reverse (top-most Z first)
        for (int i = comp.children.size() - 1; i >= 0; i--) {
            UIComponent result = findDeepestTooltipTarget(comp.children.get(i), mx, my);
            if (result != null) return result;
        }
        return comp.hasTooltip() ? comp : null;
    }

    private static void drawTooltipBox(DrawContext ctx, String[] lines,
                                        int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        int pad   = 4;
        int lineH = tr.fontHeight + 2;
        int maxW  = 0;
        for (String line : lines) {
            maxW = Math.max(maxW, tr.getWidth(line));
        }
        int boxW = maxW + pad * 2;
        int boxH = lines.length * lineH + pad * 2 - 2;

        int bx = mouseX + 12;
        int by = mouseY - 12;

        // Clamp to screen edges
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        if (bx + boxW > sw) bx = mouseX - boxW - 4;
        if (by + boxH > sh) by = sh - boxH;
        if (by < 0) by = 0;

        ProceduralRenderer.fillRoundedRect(ctx, bx, by, boxW, boxH,
                3, 0xF0100020);
        ProceduralRenderer.drawRoundedBorder(ctx, bx, by, boxW, boxH,
                3, ProceduralRenderer.COL_ACCENT);

        int ty = by + pad;
        for (String line : lines) {
            ProceduralRenderer.drawText(ctx, tr, line, bx + pad, ty,
                    ProceduralRenderer.COL_TEXT_PRIMARY);
            ty += lineH;
        }
    }
}
