package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;

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

    // ── Relative positioning ─────────────────────────────────────────────
    private int offsetX;
    private int offsetY;
    private boolean useRelativePosition = false;

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
    private static final long DEFAULT_TOOLTIP_DELAY_NS = 500_000_000L;
    /** Per-instance tooltip delay — defaults to the static default. */
    private long tooltipDelayNs = DEFAULT_TOOLTIP_DELAY_NS;
    /** Rich tooltip (takes priority over plain tooltipLines). */
    private RichTooltip richTooltip;

    // ── Click handler ────────────────────────────────────────────────────
    private Runnable onClick;

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
        // onClick handler — fires on left-click when hovered
        if (button == 0 && onClick != null && isHovered(mouseX, mouseY)) {
            onClick.run();
            return true;
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

    /**
     * Forward a mouse-drag event.
     *
     * @return {@code true} if the drag was consumed.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double deltaX, double deltaY) {
        if (!visible || !enabled) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return false;
    }

    /**
     * Forward a key press.
     *
     * @return {@code true} if the key was consumed.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    /**
     * Forward a character typed event.
     *
     * @return {@code true} if the character was consumed.
     */
    public boolean charTyped(char chr, int modifiers) {
        if (!visible || !enabled) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).charTyped(chr, modifiers)) return true;
        }
        return false;
    }

    /**
     * Forward a mouse-moved event (for precise hover tracking).
     */
    public void mouseMoved(double mouseX, double mouseY) {
        if (!visible) return;
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).mouseMoved(mouseX, mouseY);
        }
    }

    // ── MC 1.21.11 Input API bridges (v1.10.0) ─────────────────────────

    /**
     * Bridge for MC 1.21.11's {@link Click} record.
     * Decomposes the record and delegates to
     * {@link #mouseClicked(double, double, int)}.
     *
     * @param click the Click record from the new input API
     * @return {@code true} if the click was consumed
     * @since 1.10.0
     */
    public boolean mouseClicked(Click click) {
        return mouseClicked(click.x(), click.y(), click.buttonInfo().button());
    }

    /**
     * Bridge for MC 1.21.11's {@link Click} record with keyboard flag.
     * The {@code fromKeyboard} parameter is provided for compatibility
     * but is currently unused by the component tree.
     *
     * @param click        the Click record
     * @param fromKeyboard {@code true} if the click originated from a keyboard
     * @return {@code true} if the click was consumed
     * @since 1.10.0
     */
    public boolean mouseClicked(Click click, boolean fromKeyboard) {
        return mouseClicked(click.x(), click.y(), click.buttonInfo().button());
    }

    /**
     * Bridge for MC 1.21.11's {@link Click} record on mouse release.
     * Decomposes the record and delegates to
     * {@link #mouseReleased(double, double, int)}.
     *
     * @param click the Click record
     * @return {@code true} if the release was consumed
     * @since 1.10.0
     */
    public boolean mouseReleased(Click click) {
        return mouseReleased(click.x(), click.y(), click.buttonInfo().button());
    }

    /**
     * Bridge for MC 1.21.11's {@link KeyInput} record.
     * Decomposes the record and delegates to
     * {@link #keyPressed(int, int, int)}.
     *
     * @param input the KeyInput record
     * @return {@code true} if the key was consumed
     * @since 1.10.0
     */
    public boolean keyPressed(KeyInput input) {
        return keyPressed(input.key(), input.scancode(), input.modifiers());
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
        child.resolveRelativePosition();
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

    /** Returns the parent component, or {@code null} if this is a root. */
    public UIComponent getParent() {
        return parent;
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

    // ── Relative / offset positioning ────────────────────────────────────

    /**
     * Set a parent-relative offset. When the parent's position changes
     * the child's absolute position is recomputed as
     * {@code parentX + offsetX, parentY + offsetY}.
     *
     * @param offsetX X offset from parent's top-left
     * @param offsetY Y offset from parent's top-left
     */
    public void setRelativePosition(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.useRelativePosition = true;
        resolveRelativePosition();
    }

    /** @return the X offset when using relative positioning. */
    public int getOffsetX() { return offsetX; }

    /** @return the Y offset when using relative positioning. */
    public int getOffsetY() { return offsetY; }

    /** @return {@code true} if this component uses parent-relative positioning. */
    public boolean isUsingRelativePosition() { return useRelativePosition; }

    /** Stop using relative positioning — reverts to absolute coordinates. */
    public void clearRelativePosition() { this.useRelativePosition = false; }

    /**
     * Resolve this component's absolute position from its parent + offset.
     * Called automatically when added to a parent or when the parent moves.
     */
    void resolveRelativePosition() {
        if (!useRelativePosition || parent == null) return;
        this.x = parent.x + offsetX;
        this.y = parent.y + offsetY;
        // Cascade to children that also use relative positioning
        for (UIComponent child : children) {
            child.resolveRelativePosition();
        }
    }

    /**
     * Update position and cascade to relative children.
     */
    public void setPositionAndResolve(int x, int y) {
        this.x = x;
        this.y = y;
        for (UIComponent child : children) {
            child.resolveRelativePosition();
        }
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { this.visible = v; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }

    public boolean isBlockInputInBounds() { return blockInputInBounds; }
    public void setBlockInputInBounds(boolean b) { this.blockInputInBounds = b; }

    /**
     * Set a click handler on any component.  When set, the component
     * will consume left-clicks within its bounds and invoke this callback.
     *
     * @param handler action to run on click, or {@code null} to remove
     */
    public void setOnClick(Runnable handler) { this.onClick = handler; }

    /** @return the current click handler, or {@code null}. */
    public Runnable getOnClick() { return onClick; }

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
        return (tooltipLines != null && tooltipLines.length > 0) || richTooltip != null;
    }

    /**
     * Set a {@link RichTooltip} on this component.
     * Takes priority over plain tooltip lines set via {@link #setTooltip(String...)}.
     *
     * @param tooltip the rich tooltip, or {@code null} to remove
     */
    public void setRichTooltip(RichTooltip tooltip) {
        this.richTooltip = tooltip;
    }

    /** @return the rich tooltip, or {@code null} if none. */
    public RichTooltip getRichTooltip() { return richTooltip; }

    /**
     * Set the tooltip hover delay in milliseconds.
     * Default is 500 ms. Set to 0 for instant tooltips.
     *
     * @param ms delay in milliseconds
     */
    public void setTooltipDelayMs(long ms) {
        this.tooltipDelayNs = Math.max(0, ms) * 1_000_000L;
    }

    /** @return tooltip delay in milliseconds. */
    public long getTooltipDelayMs() {
        return tooltipDelayNs / 1_000_000L;
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
        if (elapsed < target.tooltipDelayNs) return;

        // Rich tooltip takes priority
        if (target.richTooltip != null) {
            RichTooltip.renderTooltip(ctx, target.richTooltip, mouseX, mouseY);
            return;
        }

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
