package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;

/**
 * A toggle switch with an animated sliding knob.
 * <p>
 * Click or press A/Cross to toggle between on and off states.
 * The knob slides smoothly between positions.
 *
 * @since 1.12.0
 */
public class ToggleSwitch extends UIComponent {

    private boolean toggled = false;
    private float knobPosition = 0f; // 0 = off (left), 1 = on (right)

    private int offColor   = 0xFF3A3A4A;
    private int onColor    = ProceduralRenderer.COL_ACCENT_TEAL;
    private int knobColor  = 0xFFE0E0E0;
    private int borderColor = ProceduralRenderer.COL_BORDER;

    private Consumer<Boolean> onToggled;

    public ToggleSwitch(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public ToggleSwitch(int x, int y) {
        this(x, y, 36, 18);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Animate knob position
        float target = toggled ? 1f : 0f;
        knobPosition += (target - knobPosition) * 0.3f;
        if (Math.abs(knobPosition - target) < 0.01f) knobPosition = target;

        int radius = height / 2;

        // Track
        int trackColor = ProceduralRenderer.lerpColor(offColor, onColor, knobPosition);
        if (!enabled) {
            trackColor = ProceduralRenderer.darken(
                    ProceduralRenderer.saturate(trackColor, 0f), 0.5f);
        }
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, radius, trackColor);
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height, radius, borderColor);

        // Knob
        int knobDiameter = height - 4;
        int knobX = x + 2 + (int) ((width - knobDiameter - 4) * knobPosition);
        int knobY = y + 2;
        int kc = enabled ? knobColor : ProceduralRenderer.darken(knobColor, 0.5f);
        ProceduralRenderer.fillRoundedRect(ctx, knobX, knobY,
                knobDiameter, knobDiameter, knobDiameter / 2, kc);

        // Focus ring
        if (FocusManager.getInstance().isFocused(this)) {
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int alpha = (int) (pulse * 200);
            int focusColor = ProceduralRenderer.withAlpha(
                    ProceduralRenderer.COL_ACCENT_TEAL, alpha);
            ProceduralRenderer.drawRoundedBorder(ctx,
                    x - 1, y - 1, width + 2, height + 2, radius + 1, focusColor);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button == 0 && isHovered(mouseX, mouseY)) {
            toggle();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        if (FocusManager.getInstance().isFocused(this)) {
            // Space or Enter to toggle
            if (keyCode == 32 || keyCode == 257) {
                toggle();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void toggle() {
        toggled = !toggled;
        UISoundManager.playClick();
        if (onToggled != null) onToggled.accept(toggled);
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public boolean isToggled()                         { return toggled; }
    public ToggleSwitch setToggled(boolean on)         { this.toggled = on; knobPosition = on ? 1f : 0f; return this; }
    public ToggleSwitch setOnToggled(Consumer<Boolean> cb) { this.onToggled = cb; return this; }
    public ToggleSwitch setOffColor(int c)             { this.offColor = c; return this; }
    public ToggleSwitch setOnColor(int c)              { this.onColor = c; return this; }
    public ToggleSwitch setKnobColor(int c)            { this.knobColor = c; return this; }
    public ToggleSwitch setBorderColor(int c)          { this.borderColor = c; return this; }
}
