package com.pocketuicore.component;

import com.pocketuicore.animation.AnimationTicker;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

/**
 * Module 2 — HoverButton
 * <p>
 * An interactive button rendered entirely with procedural graphics.
 * <ul>
 *   <li>Colour smoothly blends between <em>normal → hover → pressed</em>
 *       states via the {@link AnimationTicker}.</li>
 *   <li>Plays the vanilla UI click sound on press.</li>
 *   <li>Label text is centred and shadow-drawn.</li>
 * </ul>
 */
public class HoverButton extends UIComponent {

    // ── Appearance ───────────────────────────────────────────────────────
    private String label;
    private int normalColor;
    private int hoverColor;
    private int pressedColor;
    private int textColor;
    private int cornerRadius;

    // ── State ────────────────────────────────────────────────────────────
    private boolean hovered;
    private boolean pressed;
    private float hoverTransition = 0f; // 0 = normal, 1 = fully hovered


    // ── Callback ─────────────────────────────────────────────────────────
    private Runnable onClick;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create a HoverButton with the library's default palette.
     *
     * @param label   button label
     * @param onClick action fired on left-click release
     */
    public HoverButton(int x, int y, int width, int height,
                       String label, Runnable onClick) {
        this(x, y, width, height, label, onClick,
             ProceduralRenderer.COL_BG_ELEVATED,
             ProceduralRenderer.COL_HOVER,
             ProceduralRenderer.COL_ACCENT,
             ProceduralRenderer.COL_TEXT_PRIMARY,
             4);
    }

    /**
     * Full constructor with custom colours.
     */
    public HoverButton(int x, int y, int width, int height,
                       String label, Runnable onClick,
                       int normalColor, int hoverColor, int pressedColor,
                       int textColor, int cornerRadius) {
        super(x, y, width, height);
        this.label        = label;
        this.onClick      = onClick;
        this.normalColor  = normalColor;
        this.hoverColor   = hoverColor;
        this.pressedColor = pressedColor;
        this.textColor    = textColor;
        this.cornerRadius = cornerRadius;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hovered = isHovered(mouseX, mouseY) && enabled;

        // ── Transition smoothing ─────────────────────────────────────────
        // Frame-rate-independent lerp: converges ~87 % in ~8 frames at any fps.
        float target = hovered ? 1f : 0f;
        hoverTransition += (target - hoverTransition) * 0.25f;

        // ── Colour blending ──────────────────────────────────────────────
        int bgColor;
        if (pressed && hovered) {
            bgColor = pressedColor;
        } else {
            bgColor = ProceduralRenderer.lerpColor(normalColor, hoverColor, hoverTransition);
        }

        if (!enabled) {
            bgColor = ProceduralRenderer.withAlpha(bgColor, 100);
        }

        // ── Draw ─────────────────────────────────────────────────────────
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, bgColor);

        // Label
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int textY = y + (height - tr.fontHeight) / 2;
        int tc = enabled ? textColor : ProceduralRenderer.COL_TEXT_MUTED;
        ProceduralRenderer.drawCenteredText(ctx, tr, label, x + width / 2, textY, tc);

        // ── Focus ring (controller / keyboard navigation) ────────────────
        if (FocusManager.getInstance().isFocused(this)) {
            // Pulsing glow: sine wave oscillates alpha between ~40 % and ~80 %
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int innerAlpha = (int) (pulse * 200);
            int outerAlpha = innerAlpha / 2;
            int innerColor = ProceduralRenderer.withAlpha(
                    ProceduralRenderer.COL_ACCENT_TEAL, innerAlpha);
            int outerColor = ProceduralRenderer.withAlpha(
                    ProceduralRenderer.COL_ACCENT_TEAL, outerAlpha);
            ProceduralRenderer.drawRoundedBorder(ctx,
                    x - 1, y - 1, width + 2, height + 2,
                    cornerRadius + 1, innerColor);
            ProceduralRenderer.drawRoundedBorder(ctx,
                    x - 2, y - 2, width + 4, height + 4,
                    cornerRadius + 2, outerColor);
        }
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button == 0 && isHovered(mouseX, mouseY)) {
            pressed = true;
            return true; // consume
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;
        if (button == 0 && pressed) {
            pressed = false;
            if (isHovered(mouseX, mouseY)) {
                playClickSound();
                if (onClick != null) onClick.run();
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void playClickSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(
                PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public void setLabel(String label)           { this.label = label; }
    public String getLabel()                     { return label; }
    public void setOnClick(Runnable r)           { this.onClick = r; }
    public void setNormalColor(int c)            { this.normalColor = c; }
    public void setHoverColor(int c)             { this.hoverColor = c; }
    public void setPressedColor(int c)           { this.pressedColor = c; }
    public void setTextColor(int c)              { this.textColor = c; }
    public void setCornerRadius(int r)           { this.cornerRadius = r; }
    public boolean isHoveredState()              { return hovered; }
    public boolean isPressedState()              { return pressed; }
}
