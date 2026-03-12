package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.render.Theme;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A group of radio buttons — only one option can be selected at a time.
 * <p>
 * Each option is rendered as a circular indicator with a label.
 * Options are laid out vertically within the component bounds.
 * <p>
 * Respects the active {@link Theme} for its default colours.
 *
 * @since 1.12.0
 */
public class RadioGroup extends UIComponent {

    private final List<String> options = new ArrayList<>();
    private int selectedIndex = 0;
    private Consumer<Integer> onSelect;

    // ── Appearance ───────────────────────────────────────────────────────
    private static final int THEME_DEFAULT = Integer.MIN_VALUE;
    private int radioColor        = THEME_DEFAULT;
    private int selectedColor     = THEME_DEFAULT;
    private int textColor         = THEME_DEFAULT;
    private int hoverColor        = THEME_DEFAULT;
    private int radioSize         = 10;
    private int itemSpacing       = 4;

    public RadioGroup(int x, int y, int width) {
        super(x, y, width, 20);
    }

    public RadioGroup(int x, int y, int width, List<String> options) {
        super(x, y, width, 20);
        this.options.addAll(options);
        recalcHeight();
    }

    /**
     * Create a RadioGroup at (0,0) with the given options.
     * Width defaults to 120; use inside a stretch-width layout panel
     * to fill automatically.
     *
     * @param options list of option labels
     * @since 1.14.0
     */
    public RadioGroup(List<String> options) {
        this(0, 0, 120, options);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Theme theme = Theme.current();
        int radio    = radioColor    == THEME_DEFAULT ? theme.border()      : radioColor;
        int selected = selectedColor == THEME_DEFAULT ? theme.accentTeal()  : selectedColor;
        int txt      = textColor     == THEME_DEFAULT ? theme.textPrimary() : textColor;
        int hover    = hoverColor    == THEME_DEFAULT ? theme.hover()       : hoverColor;
        int mutedTxt = theme.textMuted();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int itemH = Math.max(radioSize, tr.fontHeight) + itemSpacing;

        for (int i = 0; i < options.size(); i++) {
            int iy = y + i * itemH;
            boolean hovered = enabled && mouseX >= x && mouseX < x + width
                    && mouseY >= iy && mouseY < iy + itemH;

            if (hovered) {
                ProceduralRenderer.fillRect(ctx, x, iy, width, itemH, hover);
            }

            // Radio circle
            int circleX = x + 4;
            int circleY = iy + (itemH - radioSize) / 2;
            int circleColor = (i == selectedIndex) ? selected : radio;
            if (!enabled) circleColor = ProceduralRenderer.darken(circleColor, 0.5f);

            // Outer ring
            ProceduralRenderer.drawRoundedBorder(ctx, circleX, circleY,
                    radioSize, radioSize, radioSize / 2, circleColor);

            // Inner dot when selected
            if (i == selectedIndex) {
                int innerSize = radioSize - 4;
                ProceduralRenderer.fillRoundedRect(ctx, circleX + 2, circleY + 2,
                        innerSize, innerSize, innerSize / 2, circleColor);
            }

            // Label
            int labelX = circleX + radioSize + 6;
            int labelY = iy + (itemH - tr.fontHeight) / 2;
            int tc = enabled ? txt : mutedTxt;
            ProceduralRenderer.drawText(ctx, tr, options.get(i), labelX, labelY, tc);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || button != 0) return false;
        if (!isHovered(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int itemH = Math.max(radioSize, tr.fontHeight) + itemSpacing;

        int idx = (int) ((mouseY - y) / itemH);
        if (idx >= 0 && idx < options.size() && idx != selectedIndex) {
            selectedIndex = idx;
            UISoundManager.playClick();
            if (onSelect != null) onSelect.accept(selectedIndex);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        if (!FocusManager.getInstance().isFocused(this)) return false;

        if (keyCode == 264 && selectedIndex < options.size() - 1) { // Down
            selectedIndex++;
            UISoundManager.playClick();
            if (onSelect != null) onSelect.accept(selectedIndex);
            return true;
        } else if (keyCode == 265 && selectedIndex > 0) { // Up
            selectedIndex--;
            UISoundManager.playClick();
            if (onSelect != null) onSelect.accept(selectedIndex);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    private void recalcHeight() {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int itemH = Math.max(radioSize, tr.fontHeight) + itemSpacing;
        this.height = options.size() * itemH;
    }

    public RadioGroup setOptions(List<String> opts) {
        this.options.clear();
        this.options.addAll(opts);
        if (selectedIndex >= options.size()) selectedIndex = 0;
        recalcHeight();
        return this;
    }

    public List<String> getOptions() { return List.copyOf(options); }

    public int getSelectedIndex()    { return selectedIndex; }
    public String getSelectedOption() {
        return selectedIndex >= 0 && selectedIndex < options.size()
                ? options.get(selectedIndex) : null;
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public RadioGroup setSelectedIndex(int idx)       { this.selectedIndex = Math.max(0, Math.min(idx, options.size() - 1)); return this; }
    public RadioGroup setOnSelect(Consumer<Integer> cb) { this.onSelect = cb; return this; }

    /**
     * Alias for {@link #setOnSelect(Consumer)} — standardised onChange callback.
     * @since 1.14.0
     */
    public RadioGroup setOnChange(Consumer<Integer> cb) { return setOnSelect(cb); }
    public RadioGroup setRadioColor(int c)            { this.radioColor = c; return this; }
    public RadioGroup setSelectedColor(int c)         { this.selectedColor = c; return this; }
    public RadioGroup setTextColor(int c)             { this.textColor = c; return this; }
    public RadioGroup setHoverColor(int c)            { this.hoverColor = c; return this; }
    public RadioGroup setRadioSize(int s)             { this.radioSize = Math.max(6, s); recalcHeight(); return this; }
    public RadioGroup setItemSpacing(int s)           { this.itemSpacing = s; recalcHeight(); return this; }
}
