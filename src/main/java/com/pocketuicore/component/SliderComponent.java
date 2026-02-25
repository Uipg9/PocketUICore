package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * SliderComponent — A horizontal slider for picking a value in a range.
 * <p>
 * Renders a track with a draggable thumb. Supports mouse drag, click-to-set,
 * keyboard left/right arrows (when focused), and an optional value-changed
 * callback.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     SliderComponent vol = new SliderComponent(10, 40, 120, 16, 0f, 1f, 0.5f);
 *     vol.setOnValueChanged(v -> System.out.println("Volume: " + v));
 *     panel.addChild(vol);
 * }</pre>
 */
public class SliderComponent extends UIComponent {

    // ── Configuration ────────────────────────────────────────────────────
    private float minValue;
    private float maxValue;
    private float value;
    private float step = 0f; // 0 = continuous

    // ── Appearance ───────────────────────────────────────────────────────
    private int trackColor     = 0xFF555555;
    private int fillColor      = 0xFF44AAFF;
    private int thumbColor     = 0xFFFFFFFF;
    private int thumbSize      = 8;
    private int trackHeight    = 4;

    // ── State ────────────────────────────────────────────────────────────
    private boolean dragging = false;
    private boolean focused  = false;

    // ── Callback ─────────────────────────────────────────────────────────
    private Consumer<Float> onValueChanged;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create a new slider.
     *
     * @param x      absolute X position
     * @param y      absolute Y position
     * @param width  total slider width (including thumb room)
     * @param height total slider height
     * @param min    minimum value
     * @param max    maximum value
     * @param initial the starting value
     */
    public SliderComponent(int x, int y, int width, int height,
                           float min, float max, float initial) {
        super(x, y, width, height);
        this.minValue = min;
        this.maxValue = max;
        this.value = clamp(initial);
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    /** Get the current value. */
    public float getValue() { return value; }

    /** Set the value programmatically (clamps to range, applies step). */
    public void setValue(float newValue) {
        float old = this.value;
        this.value = quantize(clamp(newValue));
        if (this.value != old && onValueChanged != null) {
            onValueChanged.accept(this.value);
        }
    }

    /** Set the step size (0 = continuous). */
    public void setStep(float step) { this.step = Math.max(0f, step); }

    public float getMinValue() { return minValue; }
    public float getMaxValue() { return maxValue; }

    public void setRange(float min, float max) {
        this.minValue = min;
        this.maxValue = max;
        setValue(this.value); // re-clamp
    }

    /** Register a callback fired whenever the value changes. */
    public void setOnValueChanged(Consumer<Float> cb) { this.onValueChanged = cb; }

    // ── Appearance setters ───────────────────────────────────────────────

    public void setTrackColor(int color)  { this.trackColor = color; }
    public void setFillColor(int color)   { this.fillColor = color; }
    public void setThumbColor(int color)  { this.thumbColor = color; }
    public void setThumbSize(int size)    { this.thumbSize = Math.max(2, size); }
    public void setTrackHeight(int h)     { this.trackHeight = Math.max(1, h); }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int trackY = y + (height - trackHeight) / 2;

        // Track background
        ProceduralRenderer.fillRoundedRect(ctx, x, trackY, width, trackHeight,
                trackHeight / 2, trackColor);

        // Fill portion
        float pct = normalizedValue();
        int fillW = (int) (usableWidth() * pct);
        if (fillW > 0) {
            ProceduralRenderer.fillRoundedRect(ctx, x, trackY, fillW, trackHeight,
                    trackHeight / 2, fillColor);
        }

        // Thumb
        int thumbX = x + (int) (usableWidth() * pct) - thumbSize / 2;
        int thumbY = y + (height - thumbSize) / 2;
        ProceduralRenderer.fillRoundedRect(ctx, thumbX, thumbY, thumbSize, thumbSize,
                thumbSize / 2, thumbColor);

        // Focus ring
        if (focused) {
            ProceduralRenderer.drawRoundedBorder(ctx, thumbX - 1, thumbY - 1,
                    thumbSize + 2, thumbSize + 2, thumbSize / 2 + 1, fillColor);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button == 0 && isHovered((int) mouseX, (int) mouseY)) {
            dragging = true;
            focused = true;
            setValueFromMouse(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                 double deltaX, double deltaY) {
        if (dragging && button == 0) {
            setValueFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused || !visible || !enabled) return false;

        float range = maxValue - minValue;
        float increment = step > 0 ? step : range * 0.01f;

        // GLFW: LEFT=263, RIGHT=262
        if (keyCode == 263) {
            setValue(value - increment);
            return true;
        } else if (keyCode == 262) {
            setValue(value + increment);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // =====================================================================
    //  Focus
    // =====================================================================

    /** Set whether this slider has keyboard/controller focus. */
    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean isFocused() { return focused; }

    // =====================================================================
    //  Internal helpers
    // =====================================================================

    private void setValueFromMouse(double mouseX) {
        float pct = (float) (mouseX - x) / usableWidth();
        pct = Math.max(0f, Math.min(1f, pct));
        setValue(minValue + pct * (maxValue - minValue));
    }

    private float normalizedValue() {
        if (maxValue <= minValue) return 0f;
        return (value - minValue) / (maxValue - minValue);
    }

    private int usableWidth() {
        return Math.max(1, width);
    }

    private float clamp(float v) {
        return Math.max(minValue, Math.min(maxValue, v));
    }

    private float quantize(float v) {
        if (step <= 0f) return v;
        return Math.round((v - minValue) / step) * step + minValue;
    }
}
