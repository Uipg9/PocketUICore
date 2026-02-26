package com.pocketuicore.component;

import com.pocketuicore.animation.AnimationTicker;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Function;

/**
 * Animated numeric display that counts up/down with easing, optional
 * colour flash on value change, and a scale bounce effect.
 * <p>
 * Ideal for scores, currency readouts, health counters, or any number
 * that should transition smoothly rather than jump.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     AnimatedValue score = new AnimatedValue(10, 100, 50, 12);
 *     score.setFormatter(v -> UIFormatUtils.formatGold((int) v));
 *     score.setValue(7500); // triggers smooth count-up
 *     // in render:
 *     score.render(ctx, mouseX, mouseY, delta);
 * }</pre>
 */
public final class AnimatedValue extends UIComponent {

    // ── Configuration ────────────────────────────────────────────────────
    private float displayValue;    // currently rendered value
    private float targetValue;     // value we're animating toward
    private int textColor       = ProceduralRenderer.COL_TEXT_PRIMARY;
    private int flashColor      = ProceduralRenderer.COL_SUCCESS;
    private int decreaseFlashColor = ProceduralRenderer.COL_ERROR;
    private float textScale     = 1f;
    private boolean centerText  = false;
    private boolean flashOnChange  = true;
    private boolean bounceOnChange = true;

    // ── Formatter (default shows integer) ────────────────────────────────
    private Function<Float, String> formatter = v -> String.valueOf((int) v.floatValue());

    // ── Animation namespace ──────────────────────────────────────────────
    private final String animNs;
    private static int instanceCounter = 0;

    // ── Flash / bounce state ─────────────────────────────────────────────
    private long flashStartNs   = 0;
    private int activeFlashColor;
    private long bounceStartNs  = 0;
    private static final long FLASH_DURATION_NS  = 400_000_000L; // 400 ms
    private static final long BOUNCE_DURATION_NS = 300_000_000L; // 300 ms
    private static final float BOUNCE_SCALE      = 1.25f;

    // ── Animation keys ───────────────────────────────────────────────────
    private static final String ANIM_COUNT = "count";

    // =====================================================================
    //  Constructor
    // =====================================================================

    /**
     * @param x            component x position
     * @param y            component y position
     * @param width        component width (used for centering and bounds)
     * @param height       component height
     * @param initialValue starting display value
     */
    public AnimatedValue(int x, int y, int width, int height,
                         float initialValue) {
        super(x, y, width, height);
        this.displayValue = initialValue;
        this.targetValue  = initialValue;
        this.animNs = "animVal_" + (instanceCounter++);
    }

    public AnimatedValue(int x, int y, int width, int height) {
        this(x, y, width, height, 0f);
    }

    // =====================================================================
    //  Value API
    // =====================================================================

    /**
     * Set a new target value. The display will animate toward it.
     *
     * @param value    the new target
     * @param durationMs animation duration in milliseconds (default 400)
     */
    public void setValue(float value, long durationMs) {
        if (Math.abs(value - targetValue) < 0.001f) return;

        boolean increasing = value > targetValue;
        targetValue = value;
        long now = System.nanoTime();

        // Start count animation
        AnimationTicker.getInstance().start(
                animNs + ANIM_COUNT,
                displayValue, value,
                durationMs,
                AnimationTicker.EasingType.EASE_OUT
        );

        // Trigger visual effects
        if (flashOnChange) {
            flashStartNs = now;
            activeFlashColor = increasing ? flashColor : decreaseFlashColor;
        }
        if (bounceOnChange) {
            bounceStartNs = now;
        }
    }

    /** Set value with default 400 ms animation. */
    public void setValue(float value) {
        setValue(value, 400);
    }

    /** Jump immediately to the value — no animation. */
    public void setValueImmediate(float value) {
        this.displayValue = value;
        this.targetValue  = value;
        AnimationTicker.getInstance().cancel(animNs + ANIM_COUNT);
    }

    /** @return the current target value. */
    public float getTargetValue() { return targetValue; }

    /** @return the currently displayed (animated) value. */
    public float getDisplayValue() { return displayValue; }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY,
                              float delta) {
        long now = System.nanoTime();
        AnimationTicker ticker = AnimationTicker.getInstance();

        // ── Update display value from tween ──────────────────────────────
        if (ticker.isActive(animNs + ANIM_COUNT)) {
            displayValue = ticker.get(animNs + ANIM_COUNT);
        } else {
            displayValue = targetValue;
        }

        // ── Resolve display text ─────────────────────────────────────────
        String text = formatter.apply(displayValue);

        // ── Determine colour (flash overlay) ─────────────────────────────
        int color = textColor;
        if (flashStartNs > 0) {
            long flashElapsed = now - flashStartNs;
            if (flashElapsed < FLASH_DURATION_NS) {
                float t = (float) flashElapsed / FLASH_DURATION_NS;
                color = ProceduralRenderer.lerpColor(activeFlashColor, textColor, t);
            } else {
                flashStartNs = 0;
            }
        }

        // ── Determine scale (bounce overlay) ─────────────────────────────
        float scale = textScale;
        if (bounceStartNs > 0) {
            long bounceElapsed = now - bounceStartNs;
            if (bounceElapsed < BOUNCE_DURATION_NS) {
                float t = (float) bounceElapsed / BOUNCE_DURATION_NS;
                // Overshoot then settle: starts at BOUNCE_SCALE, eases back to 1
                float easedT = t * t * (3f - 2f * t); // smoothstep
                scale = textScale * (BOUNCE_SCALE + (1f - BOUNCE_SCALE) * easedT);
            } else {
                bounceStartNs = 0;
            }
        }

        // ── Draw ─────────────────────────────────────────────────────────
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (centerText) {
            ProceduralRenderer.drawScaledCenteredText(
                    ctx, tr, text,
                    x + width / 2, y + height / 2 - (int)(4 * scale),
                    color, scale
            );
        } else {
            ProceduralRenderer.drawScaledText(
                    ctx, tr, text,
                    x, y + height / 2 - (int)(4 * scale),
                    color, scale
            );
        }
    }

    // =====================================================================
    //  Configuration accessors
    // =====================================================================

    public AnimatedValue setFormatter(Function<Float, String> formatter) {
        this.formatter = formatter;
        return this;
    }

    public AnimatedValue setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public AnimatedValue setFlashColor(int increaseColor) {
        this.flashColor = increaseColor;
        return this;
    }

    public AnimatedValue setDecreaseFlashColor(int color) {
        this.decreaseFlashColor = color;
        return this;
    }

    public AnimatedValue setTextScale(float scale) {
        this.textScale = scale;
        return this;
    }

    public AnimatedValue setCenterText(boolean center) {
        this.centerText = center;
        return this;
    }

    public AnimatedValue setFlashOnChange(boolean flash) {
        this.flashOnChange = flash;
        return this;
    }

    public AnimatedValue setBounceOnChange(boolean bounce) {
        this.bounceOnChange = bounce;
        return this;
    }
}
