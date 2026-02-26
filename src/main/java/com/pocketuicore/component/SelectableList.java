package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Generic typed selectable list — click, keyboard, or numbered-shortcut
 * selection with hover highlight, active selection state, and per-item
 * custom rendering.
 * <p>
 * Built on {@link DarkPanel} for automatic scrolling support when items
 * exceed the visible area.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     SelectableList<String> list = new SelectableList<>(10, 10, 200, 160);
 *     list.setItemHeight(20);
 *     list.setItemRenderer((ctx, item, x, y, w, h, hov, sel) ->
 *          ProceduralRenderer.drawText(ctx, item, x + 4, y + 4,
 *              sel ? ProceduralRenderer.COL_ACCENT : ProceduralRenderer.COL_TEXT_PRIMARY));
 *     list.setItems(List.of("Apple", "Banana", "Cherry"));
 *     list.setOnSelect(fruit -> System.out.println("Picked: " + fruit));
 * }</pre>
 *
 * @param <T> the item type
 */
public final class SelectableList<T> extends UIComponent {

    // ── Functional interface for custom item rendering ───────────────────
    @FunctionalInterface
    public interface ItemRenderer<T> {
        /**
         * Render a single item.
         *
         * @param ctx      draw context
         * @param item     the item to render
         * @param x        left edge x
         * @param y        top edge y
         * @param width    available width
         * @param height   row height
         * @param hovered  true if the mouse is over this row
         * @param selected true if this item is the active selection
         */
        void render(DrawContext ctx, T item,
                    int x, int y, int width, int height,
                    boolean hovered, boolean selected);
    }

    // ── Configuration ────────────────────────────────────────────────────
    private final List<T> items = new ArrayList<>();
    private int itemHeight = 20;
    private int selectedIndex = -1;
    private int hoveredIndex  = -1;
    private boolean numberShortcuts = false; // 1-9 key shortcuts

    // ── Callbacks ────────────────────────────────────────────────────────
    private Consumer<T> onSelect;
    private ItemRenderer<T> itemRenderer;

    // ── Scroll state ─────────────────────────────────────────────────────
    private int scrollOffset = 0;

    // ── Colours ──────────────────────────────────────────────────────────
    private int bgColor       = ProceduralRenderer.COL_BG_SURFACE;
    private int borderColor   = ProceduralRenderer.COL_BORDER;
    private int hoverColor    = ProceduralRenderer.COL_HOVER;
    private int selectColor   = ProceduralRenderer.setAlpha(ProceduralRenderer.COL_ACCENT, 0.25f);

    // =====================================================================
    //  Constructor
    // =====================================================================

    public SelectableList(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // =====================================================================
    //  Data
    // =====================================================================

    /** Replace all items. Resets selection and scroll. */
    public void setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedIndex = -1;
        this.scrollOffset  = 0;
    }

    /** Add a single item at the end. */
    public void addItem(T item) {
        items.add(item);
    }

    /** Remove a specific item. */
    public void removeItem(T item) {
        int idx = items.indexOf(item);
        if (idx >= 0) {
            items.remove(idx);
            if (selectedIndex == idx) selectedIndex = -1;
            else if (selectedIndex > idx) selectedIndex--;
        }
    }

    /** Clear all items. */
    public void clearItems() {
        items.clear();
        selectedIndex = -1;
        scrollOffset  = 0;
    }

    /** @return unmodifiable view of the items. */
    public List<T> getItems() { return List.copyOf(items); }

    /** @return the number of items. */
    public int getItemCount() { return items.size(); }

    // =====================================================================
    //  Selection
    // =====================================================================

    /** Programmatically select by index (-1 to deselect). */
    public void setSelectedIndex(int index) {
        this.selectedIndex = Math.max(-1, Math.min(index, items.size() - 1));
    }

    /** @return currently selected index, or -1. */
    public int getSelectedIndex() { return selectedIndex; }

    /** @return the currently selected item, or {@code null}. */
    public T getSelected() {
        return (selectedIndex >= 0 && selectedIndex < items.size())
                ? items.get(selectedIndex) : null;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY,
                              float delta) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // ── Background ──────────────────────────────────────────────────
        ProceduralRenderer.fillRoundedRectWithBorder(
                ctx, x, y, width, height, 4, bgColor, borderColor);

        // ── Determine hovered row ────────────────────────────────────────
        hoveredIndex = -1;
        if (isHovered(mouseX, mouseY)) {
            int relY = mouseY - y + scrollOffset;
            int idx = relY / itemHeight;
            if (idx >= 0 && idx < items.size()) hoveredIndex = idx;
        }

        // ── Scissor for clipping ─────────────────────────────────────────
        ctx.enableScissor(x, y, x + width, y + height);

        int contentH = items.size() * itemHeight;
        // Clamp scroll
        int maxScroll = Math.max(0, contentH - height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < items.size(); i++) {
            int iy = y + i * itemHeight - scrollOffset;
            // Skip if off-screen
            if (iy + itemHeight < y || iy > y + height) continue;

            boolean hov = (i == hoveredIndex);
            boolean sel = (i == selectedIndex);

            // Row background
            if (sel) {
                ctx.fill(x + 1, iy, x + width - 1, iy + itemHeight, selectColor);
            } else if (hov) {
                ctx.fill(x + 1, iy, x + width - 1, iy + itemHeight, hoverColor);
            }

            // Item content
            if (itemRenderer != null) {
                itemRenderer.render(ctx, items.get(i),
                        x + 2, iy, width - 4, itemHeight, hov, sel);
            } else {
                // Default: toString()
                String text = items.get(i).toString();
                int color = sel ? ProceduralRenderer.COL_ACCENT
                        : ProceduralRenderer.COL_TEXT_PRIMARY;
                ProceduralRenderer.drawText(ctx, textRenderer, text,
                        x + 6, iy + (itemHeight - 8) / 2, color);
            }

            // Number shortcut badge (1-9)
            if (numberShortcuts && i < 9) {
                String num = String.valueOf(i + 1);
                ProceduralRenderer.drawText(ctx, textRenderer, num,
                        x + width - 14, iy + (itemHeight - 8) / 2,
                        ProceduralRenderer.COL_TEXT_MUTED);
            }
        }

        ctx.disableScissor();

        // ── Scroll indicator ─────────────────────────────────────────────
        if (contentH > height) {
            float ratio = (float) height / contentH;
            int barH = Math.max(10, (int) (height * ratio));
            float scrollFrac = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
            int barY = y + (int) ((height - barH) * scrollFrac);
            ctx.fill(x + width - 3, barY, x + width - 1, barY + barH,
                    ProceduralRenderer.COL_TEXT_MUTED);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < items.size()) {
            selectedIndex = hoveredIndex;
            UISoundManager.playClick();
            if (onSelect != null) onSelect.accept(items.get(selectedIndex));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        if (!visible || !enabled) return false;
        if (isHovered((int) mouseX, (int) mouseY)) {
            scrollOffset -= (int) (verticalAmount * itemHeight);
            int maxScroll = Math.max(0, items.size() * itemHeight - height);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;

        // Arrow navigation
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            if (items.isEmpty()) return false;
            if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedIndex = (selectedIndex <= 0) ? items.size() - 1 : selectedIndex - 1;
            } else {
                selectedIndex = (selectedIndex >= items.size() - 1) ? 0 : selectedIndex + 1;
            }
            ensureVisible(selectedIndex);
            UISoundManager.playClick();
            return true;
        }

        // Enter = select
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (selectedIndex >= 0 && selectedIndex < items.size()) {
                UISoundManager.playClick();
                if (onSelect != null) onSelect.accept(items.get(selectedIndex));
            }
            return true;
        }

        // Number shortcuts 1-9
        if (numberShortcuts) {
            int num = keyCode - GLFW.GLFW_KEY_1;
            if (num >= 0 && num < 9 && num < items.size()) {
                selectedIndex = num;
                ensureVisible(selectedIndex);
                UISoundManager.playClick();
                if (onSelect != null) onSelect.accept(items.get(selectedIndex));
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Scroll so that the item at the given index is visible. */
    private void ensureVisible(int index) {
        int iy = index * itemHeight;
        if (iy < scrollOffset) {
            scrollOffset = iy;
        } else if (iy + itemHeight > scrollOffset + height) {
            scrollOffset = iy + itemHeight - height;
        }
    }

    // =====================================================================
    //  Configuration
    // =====================================================================

    public SelectableList<T> setItemHeight(int h) {
        this.itemHeight = h;
        return this;
    }

    public SelectableList<T> setItemRenderer(ItemRenderer<T> renderer) {
        this.itemRenderer = renderer;
        return this;
    }

    public SelectableList<T> setOnSelect(Consumer<T> callback) {
        this.onSelect = callback;
        return this;
    }

    public SelectableList<T> setNumberShortcuts(boolean enabled) {
        this.numberShortcuts = enabled;
        return this;
    }

    public SelectableList<T> setBackgroundColor(int color) {
        this.bgColor = color;
        return this;
    }

    public SelectableList<T> setBorderColor(int color) {
        this.borderColor = color;
        return this;
    }

    public SelectableList<T> setHoverColor(int color) {
        this.hoverColor = color;
        return this;
    }

    public SelectableList<T> setSelectColor(int color) {
        this.selectColor = color;
        return this;
    }

    public int getItemHeight() { return itemHeight; }
    public boolean hasNumberShortcuts() { return numberShortcuts; }
}
