package com.pocketuicore.controller;

import com.pocketuicore.input.InputHelper;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller Glyphs — Maps gamepad buttons to display labels and renders
 * inline button prompts in the UI.
 * <p>
 * When the user is using a controller, call prompts like {@code [A]},
 * {@code [B]}, {@code [LB]}, {@code [RB]} can be shown instead of keyboard
 * hints.  Supports Xbox-style, PlayStation-style, and generic labels.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // Get the display string for the "confirm" action:
 *     String label = ControllerGlyphs.getActionLabel(ControllerGlyphs.Action.CONFIRM);
 *     // → "[A]" (Xbox) / "[✕]" (PlayStation) / "Enter" (keyboard)
 *
 *     // Render a button prompt inline:
 *     ControllerGlyphs.renderPrompt(ctx, Action.CONFIRM, x, y);
 * }</pre>
 *
 * @since 1.13.0
 */
public final class ControllerGlyphs {

    private ControllerGlyphs() { /* static utility */ }

    // =====================================================================
    //  Controller style
    // =====================================================================

    /** The visual style for controller button labels. */
    public enum GlyphStyle {
        /** Xbox-style labels: A, B, X, Y, LB, RB, LT, RT */
        XBOX,
        /** PlayStation-style labels: ✕, ○, □, △, L1, R1, L2, R2 */
        PLAYSTATION,
        /** Nintendo-style labels: B, A, Y, X, L, R, ZL, ZR */
        NINTENDO,
        /** Generic labels: Confirm, Back, etc. */
        GENERIC
    }

    private static GlyphStyle style = GlyphStyle.XBOX;

    /** Set the controller glyph style. */
    public static void setStyle(GlyphStyle newStyle) { style = newStyle; }

    /** @return the current glyph style. */
    public static GlyphStyle getStyle() { return style; }

    // =====================================================================
    //  Actions
    // =====================================================================

    /** Logical actions that map to buttons. */
    public enum Action {
        CONFIRM,
        BACK,
        TAB_LEFT,
        TAB_RIGHT,
        NAVIGATE_UP,
        NAVIGATE_DOWN,
        NAVIGATE_LEFT,
        NAVIGATE_RIGHT,
        SCROLL_UP,
        SCROLL_DOWN,
        TRIGGER_LEFT,
        TRIGGER_RIGHT
    }

    // =====================================================================
    //  Label tables
    // =====================================================================

    private static final Map<Action, String> XBOX_LABELS = new HashMap<>();
    private static final Map<Action, String> PS_LABELS = new HashMap<>();
    private static final Map<Action, String> NINTENDO_LABELS = new HashMap<>();
    private static final Map<Action, String> GENERIC_LABELS = new HashMap<>();
    private static final Map<Action, String> KEYBOARD_LABELS = new HashMap<>();

    static {
        XBOX_LABELS.put(Action.CONFIRM, "A");
        XBOX_LABELS.put(Action.BACK, "B");
        XBOX_LABELS.put(Action.TAB_LEFT, "LB");
        XBOX_LABELS.put(Action.TAB_RIGHT, "RB");
        XBOX_LABELS.put(Action.NAVIGATE_UP, "\u2191"); // ↑
        XBOX_LABELS.put(Action.NAVIGATE_DOWN, "\u2193"); // ↓
        XBOX_LABELS.put(Action.NAVIGATE_LEFT, "\u2190"); // ←
        XBOX_LABELS.put(Action.NAVIGATE_RIGHT, "\u2192"); // →
        XBOX_LABELS.put(Action.SCROLL_UP, "RS\u2191");
        XBOX_LABELS.put(Action.SCROLL_DOWN, "RS\u2193");
        XBOX_LABELS.put(Action.TRIGGER_LEFT, "LT");
        XBOX_LABELS.put(Action.TRIGGER_RIGHT, "RT");

        PS_LABELS.put(Action.CONFIRM, "\u2715"); // ✕
        PS_LABELS.put(Action.BACK, "\u25CB"); // ○
        PS_LABELS.put(Action.TAB_LEFT, "L1");
        PS_LABELS.put(Action.TAB_RIGHT, "R1");
        PS_LABELS.put(Action.NAVIGATE_UP, "\u2191");
        PS_LABELS.put(Action.NAVIGATE_DOWN, "\u2193");
        PS_LABELS.put(Action.NAVIGATE_LEFT, "\u2190");
        PS_LABELS.put(Action.NAVIGATE_RIGHT, "\u2192");
        PS_LABELS.put(Action.SCROLL_UP, "R3\u2191");
        PS_LABELS.put(Action.SCROLL_DOWN, "R3\u2193");
        PS_LABELS.put(Action.TRIGGER_LEFT, "L2");
        PS_LABELS.put(Action.TRIGGER_RIGHT, "R2");

        NINTENDO_LABELS.put(Action.CONFIRM, "B");
        NINTENDO_LABELS.put(Action.BACK, "A");
        NINTENDO_LABELS.put(Action.TAB_LEFT, "L");
        NINTENDO_LABELS.put(Action.TAB_RIGHT, "R");
        NINTENDO_LABELS.put(Action.NAVIGATE_UP, "\u2191");
        NINTENDO_LABELS.put(Action.NAVIGATE_DOWN, "\u2193");
        NINTENDO_LABELS.put(Action.NAVIGATE_LEFT, "\u2190");
        NINTENDO_LABELS.put(Action.NAVIGATE_RIGHT, "\u2192");
        NINTENDO_LABELS.put(Action.SCROLL_UP, "RS\u2191");
        NINTENDO_LABELS.put(Action.SCROLL_DOWN, "RS\u2193");
        NINTENDO_LABELS.put(Action.TRIGGER_LEFT, "ZL");
        NINTENDO_LABELS.put(Action.TRIGGER_RIGHT, "ZR");

        GENERIC_LABELS.put(Action.CONFIRM, "Confirm");
        GENERIC_LABELS.put(Action.BACK, "Back");
        GENERIC_LABELS.put(Action.TAB_LEFT, "Prev Tab");
        GENERIC_LABELS.put(Action.TAB_RIGHT, "Next Tab");
        GENERIC_LABELS.put(Action.NAVIGATE_UP, "Up");
        GENERIC_LABELS.put(Action.NAVIGATE_DOWN, "Down");
        GENERIC_LABELS.put(Action.NAVIGATE_LEFT, "Left");
        GENERIC_LABELS.put(Action.NAVIGATE_RIGHT, "Right");
        GENERIC_LABELS.put(Action.SCROLL_UP, "Scroll Up");
        GENERIC_LABELS.put(Action.SCROLL_DOWN, "Scroll Down");
        GENERIC_LABELS.put(Action.TRIGGER_LEFT, "L Trigger");
        GENERIC_LABELS.put(Action.TRIGGER_RIGHT, "R Trigger");

        KEYBOARD_LABELS.put(Action.CONFIRM, "Enter");
        KEYBOARD_LABELS.put(Action.BACK, "Esc");
        KEYBOARD_LABELS.put(Action.TAB_LEFT, "Shift+Tab");
        KEYBOARD_LABELS.put(Action.TAB_RIGHT, "Tab");
        KEYBOARD_LABELS.put(Action.NAVIGATE_UP, "\u2191");
        KEYBOARD_LABELS.put(Action.NAVIGATE_DOWN, "\u2193");
        KEYBOARD_LABELS.put(Action.NAVIGATE_LEFT, "\u2190");
        KEYBOARD_LABELS.put(Action.NAVIGATE_RIGHT, "\u2192");
        KEYBOARD_LABELS.put(Action.SCROLL_UP, "Scroll\u2191");
        KEYBOARD_LABELS.put(Action.SCROLL_DOWN, "Scroll\u2193");
        KEYBOARD_LABELS.put(Action.TRIGGER_LEFT, "Q");
        KEYBOARD_LABELS.put(Action.TRIGGER_RIGHT, "E");
    }

    // =====================================================================
    //  Label retrieval
    // =====================================================================

    /**
     * Get the display label for an action based on the current input mode
     * and glyph style. When the user is on keyboard/mouse, keyboard labels
     * are returned.
     *
     * @param action the logical action
     * @return a short display string like "A", "Enter", "LB", etc.
     */
    public static String getActionLabel(Action action) {
        if (InputHelper.isKeyboardMouseActive()) {
            return KEYBOARD_LABELS.getOrDefault(action, "?");
        }
        return switch (style) {
            case XBOX        -> XBOX_LABELS.getOrDefault(action, "?");
            case PLAYSTATION -> PS_LABELS.getOrDefault(action, "?");
            case NINTENDO    -> NINTENDO_LABELS.getOrDefault(action, "?");
            case GENERIC     -> GENERIC_LABELS.getOrDefault(action, "?");
        };
    }

    /**
     * Get the controller-specific label (ignoring current input mode).
     *
     * @param action the logical action
     * @return the label for the currently set glyph style
     */
    public static String getControllerLabel(Action action) {
        return switch (style) {
            case XBOX        -> XBOX_LABELS.getOrDefault(action, "?");
            case PLAYSTATION -> PS_LABELS.getOrDefault(action, "?");
            case NINTENDO    -> NINTENDO_LABELS.getOrDefault(action, "?");
            case GENERIC     -> GENERIC_LABELS.getOrDefault(action, "?");
        };
    }

    /**
     * Get the keyboard-specific label (ignoring current input mode).
     *
     * @param action the logical action
     * @return the keyboard label
     */
    public static String getKeyboardLabel(Action action) {
        return KEYBOARD_LABELS.getOrDefault(action, "?");
    }

    // =====================================================================
    //  Rendering — inline button prompts
    // =====================================================================

    /** Colours for glyph badge rendering. */
    private static final int BADGE_BG = 0xDD333355;
    private static final int BADGE_BORDER = 0xDD666688;
    private static final int BADGE_TEXT = 0xFFEEEEEE;

    /**
     * Render a button prompt badge at the given position.
     * Draws a rounded-rect pill with the action label inside.
     *
     * @param ctx    the DrawContext
     * @param action the action to show
     * @param x      left edge of the badge
     * @param y      top edge of the badge
     * @return the width of the rendered badge (for layout chaining)
     */
    public static int renderPrompt(DrawContext ctx, Action action, int x, int y) {
        String label = getActionLabel(action);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int textW = tr.getWidth(label);
        int padH = 4;
        int padV = 2;
        int badgeW = textW + padH * 2;
        int badgeH = tr.fontHeight + padV * 2;

        ProceduralRenderer.fillRoundedRect(ctx, x, y, badgeW, badgeH, 3, BADGE_BG);
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, badgeW, badgeH, 3, BADGE_BORDER);
        ctx.drawTextWithShadow(tr, net.minecraft.text.Text.literal(label),
                x + padH, y + padV, BADGE_TEXT);

        return badgeW;
    }

    /**
     * Render an action prompt with label text after the badge.
     * Example output: {@code [A] Confirm}
     *
     * @param ctx         the DrawContext
     * @param action      the action to show
     * @param description the description text after the badge
     * @param x           left edge
     * @param y           top edge
     * @param textColor   colour for the description text
     * @return the total width of the rendered prompt
     */
    public static int renderPromptWithLabel(DrawContext ctx, Action action,
                                             String description, int x, int y,
                                             int textColor) {
        int badgeW = renderPrompt(ctx, action, x, y);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int gap = 4;
        ctx.drawTextWithShadow(tr, net.minecraft.text.Text.literal(description),
                x + badgeW + gap, y + 2, textColor);
        return badgeW + gap + tr.getWidth(description);
    }

    /**
     * Render a standard bottom-bar with common action prompts.
     * Typically rendered at the bottom of a PocketScreen.
     *
     * @param ctx     the DrawContext
     * @param screenW screen width
     * @param screenH screen height
     */
    public static void renderActionBar(DrawContext ctx, int screenW, int screenH) {
        int y = screenH - 16;
        int x = 8;
        int gap = 12;

        x += renderPromptWithLabel(ctx, Action.CONFIRM, "Select", x, y,
                ProceduralRenderer.COL_TEXT_MUTED) + gap;
        x += renderPromptWithLabel(ctx, Action.BACK, "Back", x, y,
                ProceduralRenderer.COL_TEXT_MUTED) + gap;
        x += renderPromptWithLabel(ctx, Action.TAB_LEFT, "", x, y,
                ProceduralRenderer.COL_TEXT_MUTED);
        renderPromptWithLabel(ctx, Action.TAB_RIGHT, "Tabs", x + 2, y,
                ProceduralRenderer.COL_TEXT_MUTED);
    }
}
