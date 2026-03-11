package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * A numeric spinner / number input with increment and decrement buttons.
 * <p>
 * Combines a text display with clickable +/− buttons on either side.
 * Supports keyboard arrow keys when focused.
 *
 * @since 1.12.0
 */
public class SpinnerComponent extends UIComponent {

    private float value;
    private float minValue;
    private float maxValue;
    private float step = 1f;
    private boolean integerMode = true;

    private Consumer<Float> onValueChanged;

    // ── Appearance ───────────────────────────────────────────────────────
    private int backgroundColor = ProceduralRenderer.COL_BG_SURFACE;
    private int borderColor     = ProceduralRenderer.COL_BORDER;
    private int buttonColor     = ProceduralRenderer.COL_BG_ELEVATED;
    private int buttonHoverColor = ProceduralRenderer.COL_HOVER;
    private int textColor       = ProceduralRenderer.COL_TEXT_PRIMARY;
    private int cornerRadius    = 4;
    private int buttonWidth     = 18;

    // ── State ────────────────────────────────────────────────────────────
    private boolean hoveredMinus;
    private boolean hoveredPlus;

    public SpinnerComponent(int x, int y, int width, int height,
                            float min, float max, float initial) {
        super(x, y, width, height);
        this.minValue = min;
        this.maxValue = max;
        this.value = clamp(initial);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        // Background
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, backgroundColor);
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, cornerRadius, borderColor);

        // Minus button
        hoveredMinus = enabled && mouseX >= x && mouseX < x + buttonWidth
                && mouseY >= y && mouseY < y + height;
        int minusBg = hoveredMinus ? buttonHoverColor : buttonColor;
        if (!enabled) minusBg = ProceduralRenderer.darken(buttonColor, 0.5f);
        ProceduralRenderer.fillRoundedRect(ctx, x, y, buttonWidth, height, cornerRadius, minusBg);
        int minusTextY = y + (height - tr.fontHeight) / 2;
        ProceduralRenderer.drawCenteredText(ctx, tr, "-", x + buttonWidth / 2, minusTextY,
                enabled ? textColor : ProceduralRenderer.COL_TEXT_MUTED);

        // Plus button
        int plusX = x + width - buttonWidth;
        hoveredPlus = enabled && mouseX >= plusX && mouseX < plusX + buttonWidth
                && mouseY >= y && mouseY < y + height;
        int plusBg = hoveredPlus ? buttonHoverColor : buttonColor;
        if (!enabled) plusBg = ProceduralRenderer.darken(buttonColor, 0.5f);
        ProceduralRenderer.fillRoundedRect(ctx, plusX, y, buttonWidth, height, cornerRadius, plusBg);
        ProceduralRenderer.drawCenteredText(ctx, tr, "+", plusX + buttonWidth / 2, minusTextY,
                enabled ? textColor : ProceduralRenderer.COL_TEXT_MUTED);

        // Value text
        String display = integerMode ? String.valueOf((int) value) : String.format("%.1f", value);
        ProceduralRenderer.drawCenteredText(ctx, tr, display, x + width / 2, minusTextY,
                enabled ? textColor : ProceduralRenderer.COL_TEXT_MUTED);

        // Focus ring
        if (FocusManager.getInstance().isFocused(this)) {
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int alpha = (int) (pulse * 200);
            int focusColor = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_ACCENT_TEAL, alpha);
            ProceduralRenderer.drawRoundedBorder(ctx,
                    x - 1, y - 1, width + 2, height + 2, cornerRadius + 1, focusColor);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || button != 0) return false;
        if (!isHovered(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button);

        if (mouseX < x + buttonWidth) {
            setValue(value - step);
            UISoundManager.playClick();
            return true;
        } else if (mouseX >= x + width - buttonWidth) {
            setValue(value + step);
            UISoundManager.playClick();
            return true;
        }
        return true; // consume click on the value area
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        if (!FocusManager.getInstance().isFocused(this)) return false;

        if (keyCode == 265) { // Up
            setValue(value + step);
            return true;
        } else if (keyCode == 264) { // Down
            setValue(value - step);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (!visible || !enabled) return false;
        if (isHovered(mouseX, mouseY) && vAmount != 0) {
            setValue(value + (float) vAmount * step);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    public float getValue() { return value; }

    public SpinnerComponent setValue(float newValue) {
        float old = this.value;
        this.value = quantize(clamp(newValue));
        if (this.value != old && onValueChanged != null) {
            onValueChanged.accept(this.value);
        }
        return this;
    }

    private float clamp(float v) { return Math.max(minValue, Math.min(maxValue, v)); }
    private float quantize(float v) {
        if (step <= 0f) return v;
        return Math.round((v - minValue) / step) * step + minValue;
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public SpinnerComponent setRange(float min, float max) { this.minValue = min; this.maxValue = max; setValue(value); return this; }
    public SpinnerComponent setStep(float s)               { this.step = Math.max(0.001f, s); return this; }
    public SpinnerComponent setIntegerMode(boolean b)      { this.integerMode = b; return this; }
    public SpinnerComponent setOnValueChanged(Consumer<Float> cb) { this.onValueChanged = cb; return this; }
    public SpinnerComponent setBackgroundColor(int c)      { this.backgroundColor = c; return this; }
    public SpinnerComponent setBorderColor(int c)          { this.borderColor = c; return this; }
    public SpinnerComponent setButtonColor(int c)          { this.buttonColor = c; return this; }
    public SpinnerComponent setButtonHoverColor(int c)     { this.buttonHoverColor = c; return this; }
    public SpinnerComponent setTextColor(int c)            { this.textColor = c; return this; }
    public SpinnerComponent setCornerRadius(int r)         { this.cornerRadius = r; return this; }
    public SpinnerComponent setButtonWidth(int w)          { this.buttonWidth = Math.max(12, w); return this; }
    public float getMinValue()  { return minValue; }
    public float getMaxValue()  { return maxValue; }
    public float getStep()      { return step; }
    public boolean isIntegerMode() { return integerMode; }
}
