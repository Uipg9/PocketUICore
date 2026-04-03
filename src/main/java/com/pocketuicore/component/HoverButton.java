package com.pocketuicore.component;

import com.pocketcanvascore.render.PocketTheme;
import com.pocketuicore.animation.AnimationTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.render.RenderContext;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Module 2 — HoverButton
 * <p>
 * An interactive button rendered entirely with procedural graphics.
 * <ul>
 *   <li>Colour smoothly blends between <em>normal → hover → pressed</em>
 *       states via the {@link AnimationTicker}.</li>
 *   <li>Plays the vanilla UI MouseButtonEvent sound on press.</li>
 *   <li>Label Component is centred and shadow-drawn.</li>
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
     * @param onClick action fired on left-MouseButtonEvent release
     */
    public HoverButton(int x, int y, int width, int height,
                       String label, Runnable onClick) {
        this(x, y, width, height, label, onClick,
             PocketTheme.BACKGROUND_ELEVATED,
             PocketTheme.BACKGROUND_INPUT,
             PocketTheme.ACCENT_PRIMARY,
             PocketTheme.TEXT_MAIN,
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
    protected void renderSelf(RenderContext ctx, int mouseX, int mouseY, float delta) {
        hovered = isHovered(mouseX, mouseY) && enabled;

        // ── Transition smoothing ─────────────────────────────────────────
        // Frame-rate-independent lerp: converges ~87 % in ~8 frames at any fps.
        float target = hovered ? 1f : 0f;
        hoverTransition += (target - hoverTransition) * 0.25f;

        // ── Colour blending ──────────────────────────────────────────────
        int bgColor;
        if (!enabled) {
            // Disabled state: darken the normal color
            bgColor = ProceduralRenderer.darken(normalColor, 0.5f);
        } else if (pressed && hovered) {
            bgColor = pressedColor;
        } else {
            bgColor = ProceduralRenderer.lerpColor(normalColor, hoverColor, hoverTransition);
        }

        // ── Draw button body ─────────────────────────────────────────────
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height, cornerRadius, bgColor);

        // ── Draw label ───────────────────────────────────────────────────
        Minecraft client = Minecraft.getInstance();
        Font tr = client.font;
        int textY = y + (height - tr.lineHeight) / 2;
        int tc;
        if (!enabled) {
            // Disabled Component: use muted color
            tc = ProceduralRenderer.withAlpha(PocketTheme.TEXT_MUTED, 128);
        } else {
            tc = textColor;
        }
        ctx.drawCenteredTextWithShadow(tr, net.minecraft.network.chat.Component.literal(label),
                x + width / 2, textY, tc);

        // ── Focus ring (controller / keyboard navigation) ────────────────
        if (FocusManager.getInstance().isFocused(this)) {
            // Pulsing glow: sine wave oscillates alpha between ~40 % and ~80 %
            float pulse = (float) (Math.sin(System.nanoTime() / 200_000_000.0) * 0.3 + 0.7);
            int innerAlpha = (int) (pulse * 200);
            int outerAlpha = innerAlpha / 2;
                int innerColor = ProceduralRenderer.withAlpha(
                    PocketTheme.ACCENT_PRIMARY, innerAlpha);
                int outerColor = ProceduralRenderer.withAlpha(
                    PocketTheme.ACCENT_PRIMARY, outerAlpha);
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
        Minecraft client = Minecraft.getInstance();
        client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    /** @deprecated Use {@link #setText(String)} instead. */
    @Deprecated
    public HoverButton setLabel(String label)           { this.label = label; return this; }
    /** Primary setter — consistent with TextLabel. @since 1.12.0 */
    public HoverButton setText(String text)              { this.label = text; return this; }
    public String getLabel()                     { return label; }
    public HoverButton setOnClick(Runnable r)           { this.onClick = r; return this; }
    public HoverButton setNormalColor(int c)            { this.normalColor = c; return this; }
    public int  getNormalColor()                 { return normalColor; }
    public HoverButton setHoverColor(int c)             { this.hoverColor = c; return this; }
    public int  getHoverColor()                  { return hoverColor; }
    public HoverButton setPressedColor(int c)           { this.pressedColor = c; return this; }
    public int  getPressedColor()                { return pressedColor; }
    public HoverButton setTextColor(int c)              { this.textColor = c; return this; }
    public int  getTextColor()                   { return textColor; }
    public HoverButton setCornerRadius(int r)           { this.cornerRadius = r; return this; }
    public int  getCornerRadius()                { return cornerRadius; }
    public boolean isHoveredState()              { return hovered; }
    public boolean isPressedState()              { return pressed; }
}
