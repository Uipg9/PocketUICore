package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A dropdown / combo-box component.
 * <p>
 * Shows the currently selected item in a collapsed state. Clicking
 * expands a scrollable list of options below the component.
 *
 * @param <T> the type of items in the dropdown
 * @since 1.12.0
 */
public class Dropdown<T> extends UIComponent {

    private final List<T> items = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean expanded = false;

    private Function<T, String> labelExtractor = Object::toString;
    private Consumer<T> onSelect;

    // ── Appearance ───────────────────────────────────────────────────────
    private int backgroundColor  = ProceduralRenderer.COL_BG_SURFACE;
    private int borderColor      = ProceduralRenderer.COL_BORDER;
    private int hoverColor       = ProceduralRenderer.COL_HOVER;
    private int textColor        = ProceduralRenderer.COL_TEXT_PRIMARY;
    private int cornerRadius     = 4;
    private int itemHeight       = 18;
    private int maxVisibleItems  = 6;

    public Dropdown(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;

        // ── Collapsed header ─────────────────────────────────────────────
        int bgColor = enabled ? backgroundColor : ProceduralRenderer.darken(backgroundColor, 0.5f);
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, bgColor);
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, cornerRadius, borderColor);

        // Selected label
        String label = selectedIndex >= 0 && selectedIndex < items.size()
                ? labelExtractor.apply(items.get(selectedIndex))
                : "";
        int tc = enabled ? textColor : ProceduralRenderer.withAlpha(ProceduralRenderer.COL_TEXT_MUTED, 128);
        int textY = y + (height - tr.fontHeight) / 2;
        ProceduralRenderer.drawText(ctx, tr, label, x + 6, textY, tc);

        // Arrow indicator
        String arrow = expanded ? "▲" : "▼";
        int arrowX = x + width - tr.getWidth(arrow) - 6;
        ProceduralRenderer.drawText(ctx, tr, arrow, arrowX, textY, tc);

        // Focus ring
        if (FocusManager.getInstance().isFocused(this)) {
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int alpha = (int) (pulse * 200);
            int focusColor = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_ACCENT_TEAL, alpha);
            ProceduralRenderer.drawRoundedBorder(ctx,
                    x - 1, y - 1, width + 2, height + 2, cornerRadius + 1, focusColor);
        }

        // ── Expanded list ────────────────────────────────────────────────
        if (expanded && !items.isEmpty()) {
            int visibleCount = Math.min(items.size(), maxVisibleItems);
            int listH = visibleCount * itemHeight;
            int listY = y + height + 1;

            // Background
            ProceduralRenderer.fillRoundedRect(ctx, x, listY, width, listH, cornerRadius, backgroundColor);
            ProceduralRenderer.drawRoundedBorder(ctx, x, listY, width, listH, cornerRadius, borderColor);

            // Items
            for (int i = 0; i < visibleCount; i++) {
                int iy = listY + i * itemHeight;
                boolean hovered = mouseX >= x && mouseX < x + width
                        && mouseY >= iy && mouseY < iy + itemHeight;
                if (hovered) {
                    ProceduralRenderer.fillRect(ctx, x + 1, iy, width - 2, itemHeight, hoverColor);
                }
                if (i == selectedIndex) {
                    ProceduralRenderer.fillRect(ctx, x + 1, iy, 2, itemHeight,
                            ProceduralRenderer.COL_ACCENT);
                }
                String itemLabel = labelExtractor.apply(items.get(i));
                int itemTextY = iy + (itemHeight - tr.fontHeight) / 2;
                ProceduralRenderer.drawText(ctx, tr, itemLabel, x + 6, itemTextY, textColor);
            }
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button != 0) return false;

        if (expanded) {
            // Check if clicking on an item
            int listY = y + height + 1;
            int visibleCount = Math.min(items.size(), maxVisibleItems);
            if (mouseX >= x && mouseX < x + width
                    && mouseY >= listY && mouseY < listY + visibleCount * itemHeight) {
                int idx = (int) ((mouseY - listY) / itemHeight);
                if (idx >= 0 && idx < items.size()) {
                    selectItem(idx);
                }
                expanded = false;
                return true;
            }
            // Click on header while expanded — collapse
            if (isHovered(mouseX, mouseY)) {
                expanded = false;
                UISoundManager.playClick();
                return true;
            }
            // Click outside — collapse
            expanded = false;
            return false;
        } else if (isHovered(mouseX, mouseY)) {
            expanded = true;
            UISoundManager.playClick();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        if (!FocusManager.getInstance().isFocused(this)) return false;

        // Enter/Space to toggle expansion
        if (keyCode == 257 || keyCode == 32) {
            expanded = !expanded;
            UISoundManager.playClick();
            return true;
        }
        // Arrow keys when expanded
        if (expanded) {
            if (keyCode == 264) { // Down
                int next = Math.min(selectedIndex + 1, items.size() - 1);
                selectItem(next);
                return true;
            } else if (keyCode == 265) { // Up
                int prev = Math.max(selectedIndex - 1, 0);
                selectItem(prev);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isHovered(int mouseX, int mouseY) {
        // Include the expanded dropdown in the hit area
        if (expanded) {
            int visibleCount = Math.min(items.size(), maxVisibleItems);
            int totalH = height + 1 + visibleCount * itemHeight;
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + totalH;
        }
        return super.isHovered(mouseX, mouseY);
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    private void selectItem(int index) {
        if (index < 0 || index >= items.size()) return;
        this.selectedIndex = index;
        UISoundManager.playSelect();
        if (onSelect != null) onSelect.accept(items.get(index));
    }

    public Dropdown<T> setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        if (selectedIndex >= items.size()) selectedIndex = items.isEmpty() ? -1 : 0;
        return this;
    }

    public T getSelectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size()
                ? items.get(selectedIndex) : null;
    }

    public int getSelectedIndex() { return selectedIndex; }

    public Dropdown<T> setSelectedIndex(int idx) {
        this.selectedIndex = Math.max(-1, Math.min(idx, items.size() - 1));
        return this;
    }

    public boolean isExpanded() { return expanded; }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public Dropdown<T> setLabelExtractor(Function<T, String> fn) { this.labelExtractor = fn; return this; }
    public Dropdown<T> setOnSelect(Consumer<T> cb)               { this.onSelect = cb; return this; }

    /**
     * Alias for {@link #setOnSelect(Consumer)} — standardised onChange callback.
     * @since 1.14.0
     */
    public Dropdown<T> setOnChange(Consumer<T> cb)               { return setOnSelect(cb); }
    public Dropdown<T> setBackgroundColor(int c)                 { this.backgroundColor = c; return this; }
    public Dropdown<T> setBorderColor(int c)                     { this.borderColor = c; return this; }
    public Dropdown<T> setHoverColor(int c)                      { this.hoverColor = c; return this; }
    public Dropdown<T> setTextColor(int c)                       { this.textColor = c; return this; }
    public Dropdown<T> setCornerRadius(int r)                    { this.cornerRadius = r; return this; }
    public Dropdown<T> setItemHeight(int h)                      { this.itemHeight = Math.max(12, h); return this; }
    public Dropdown<T> setMaxVisibleItems(int n)                 { this.maxVisibleItems = Math.max(1, n); return this; }
}
