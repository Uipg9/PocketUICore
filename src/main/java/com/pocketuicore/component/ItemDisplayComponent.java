package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * Item Display Component — Native ItemStack Rendering
 * <p>
 * Renders a vanilla {@link ItemStack} as a UI element with optional
 * count overlay, custom count labels, and a background highlight.
 * Useful for displaying item costs, rewards, or inventory slots in
 * custom UIs.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     ItemDisplayComponent display = new ItemDisplayComponent(
 *             50, 50, Items.DIAMOND.getDefaultStack());
 *     display.getItemStack().setCount(64);
 *     display.setShowCount(true);
 * }</pre>
 */
public class ItemDisplayComponent extends UIComponent {

    private static final int ITEM_SIZE = 16;

    // ── State ────────────────────────────────────────────────────────────
    private ItemStack itemStack;
    private boolean showCount;
    private boolean showDecorations;
    private String countOverride;   // nullable — custom count label
    private boolean drawBackground;
    private int backgroundColor;
    private int backgroundRadius;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create an item display at the given position.
     * Default size is 18 x 18 (16 px item + 1 px padding each side).
     *
     * @param stack the ItemStack to display
     */
    public ItemDisplayComponent(int x, int y, ItemStack stack) {
        super(x, y, 18, 18);
        this.itemStack        = stack != null ? stack : ItemStack.EMPTY;
        this.showCount        = true;
        this.showDecorations  = true;
        this.drawBackground   = false;
        this.backgroundColor  = ProceduralRenderer.COL_BG_ELEVATED;
        this.backgroundRadius = 2;
    }

    /**
     * Create with explicit size.  The item model is centred within the
     * component bounds.
     */
    public ItemDisplayComponent(int x, int y, int width, int height, ItemStack stack) {
        super(x, y, width, height);
        this.itemStack        = stack != null ? stack : ItemStack.EMPTY;
        this.showCount        = true;
        this.showDecorations  = true;
        this.drawBackground   = false;
        this.backgroundColor  = ProceduralRenderer.COL_BG_ELEVATED;
        this.backgroundRadius = 2;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (itemStack.isEmpty()) return;

        // Optional background highlight
        if (drawBackground) {
            ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height,
                    backgroundRadius, backgroundColor);
        }

        // Centre the 16 x 16 item within the component bounds
        int ix = x + (width - ITEM_SIZE) / 2;
        int iy = y + (height - ITEM_SIZE) / 2;

        // Draw the item model
        ctx.drawItem(itemStack, ix, iy);

        // Draw decorations (count number, durability bar)
        if (showDecorations) {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer tr = client.textRenderer;
            if (countOverride != null) {
                ctx.drawStackOverlay(tr, itemStack, ix, iy, countOverride);
            } else if (showCount) {
                ctx.drawStackOverlay(tr, itemStack, ix, iy);
            }
        }
    }

    // =====================================================================
    //  API
    // =====================================================================

    /** Set the displayed ItemStack. */
    public void setItemStack(ItemStack stack) {
        this.itemStack = stack != null ? stack : ItemStack.EMPTY;
    }

    /** Get the current ItemStack. */
    public ItemStack getItemStack() { return itemStack; }

    /** Whether to show the item count number (default {@code true}). */
    public void setShowCount(boolean b)          { this.showCount = b; }
    public boolean isShowCount()                 { return showCount; }

    /** Whether to show item decorations — count + durability bar (default {@code true}). */
    public void setShowDecorations(boolean b)    { this.showDecorations = b; }
    public boolean isShowDecorations()           { return showDecorations; }

    /**
     * Override the count label (e.g. "x5" or a currency symbol).
     * Set to {@code null} to use the default ItemStack count.
     */
    public void setCountOverride(String s)       { this.countOverride = s; }
    public String getCountOverride()             { return countOverride; }

    /** Whether to draw a rounded background behind the item. */
    public void setDrawBackground(boolean b)     { this.drawBackground = b; }
    public boolean isDrawBackground()            { return drawBackground; }

    public void setBackgroundColor(int c)        { this.backgroundColor = c; }
    public void setBackgroundRadius(int r)       { this.backgroundRadius = r; }
}
