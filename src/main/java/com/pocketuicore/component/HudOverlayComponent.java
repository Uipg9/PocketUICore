package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Module 2 — HUD Overlay Component
 * <p>
 * Renders persistent on-screen displays directly on the game HUD,
 * independent of any GUI screen.  Typical uses: custom tool durability
 * bars, performance tier indicators, buff timers.
 * <p>
 * Automatically registered with Fabric's {@code HudRenderCallback} via
 * the client entrypoint.  Creating an overlay adds it to the global list;
 * call {@link #remove()} to unregister.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     HudOverlayComponent hud = new HudOverlayComponent(
 *             Anchor.BOTTOM_LEFT, 8, 40);
 *     hud.setDurability(120, 1561, "Diamond Pickaxe");
 *     hud.setTier("Efficiency V");
 *     // Renders automatically every frame.
 *     // Later:
 *     hud.remove();
 * }</pre>
 */
public class HudOverlayComponent extends UIComponent {

    // ── Global overlay registry ──────────────────────────────────────────
    private static final List<HudOverlayComponent> REGISTERED = new CopyOnWriteArrayList<>();

    // ── Layout constants ─────────────────────────────────────────────────
    private static final int PANEL_WIDTH       = 140;
    private static final int PANEL_HEIGHT_BASE = 38;
    private static final int PANEL_HEIGHT_TIER = 52;
    private static final int BAR_WIDTH         = 116;
    private static final int BAR_HEIGHT        = 6;
    private static final int PADDING           = 6;
    private static final int INNER_PAD         = 4;

    // ── Positioning ──────────────────────────────────────────────────────

    /** Screen-corner anchor for overlay positioning. */
    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private Anchor anchor;
    private int marginX;
    private int marginY;

    // ── Durability ───────────────────────────────────────────────────────
    private final PercentageBar durabilityBar;
    private String toolName = "";
    private int durCurrent  = 0;
    private int durMax      = 1;

    // ── Tier display ─────────────────────────────────────────────────────
    private String tierLabel = "";
    private boolean showTier = false;
    private int tierColor    = ProceduralRenderer.COL_ACCENT_TEAL;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Create a HUD overlay anchored to a screen corner.
     *
     * @param anchor  which corner to stick to
     * @param marginX pixels from the horizontal edge
     * @param marginY pixels from the vertical edge
     */
    public HudOverlayComponent(Anchor anchor, int marginX, int marginY) {
        super(0, 0, PANEL_WIDTH, PANEL_HEIGHT_BASE);
        this.anchor  = anchor;
        this.marginX = marginX;
        this.marginY = marginY;

        // Internal durability bar (positioned relative; reposition() sets coords)
        this.durabilityBar = new PercentageBar(0, 0, BAR_WIDTH, BAR_HEIGHT, 1.0f);
        this.durabilityBar.setShowPercentage(false);
        this.durabilityBar.setCornerRadius(BAR_HEIGHT / 2);

        REGISTERED.add(this);
    }

    // =====================================================================
    //  Static renderer — PocketUICoreClient registers via HudRenderCallback
    // =====================================================================

    /**
     * Renders all registered HUD overlays.  Automatically skipped when a
     * GUI screen is open so overlays never obstruct menus.
     */
    public static void renderAll(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        float delta = tickCounter.getTickProgress(false);

        for (HudOverlayComponent overlay : REGISTERED) {
            if (overlay.visible) {
                overlay.height = overlay.showTier && !overlay.tierLabel.isEmpty()
                        ? PANEL_HEIGHT_TIER : PANEL_HEIGHT_BASE;
                overlay.reposition(screenW, screenH);
                overlay.render(ctx, -1, -1, delta);
            }
        }
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Panel background
        ProceduralRenderer.fillRoundedRect(ctx, x, y, width, height,
                4, ProceduralRenderer.withAlpha(ProceduralRenderer.COL_BG_SURFACE, 200));
        ProceduralRenderer.drawRoundedBorder(ctx, x, y, width, height,
                4, ProceduralRenderer.COL_BORDER);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int textX = x + PADDING;
        int textY = y + PADDING;

        // Tool name + durability fraction
        String info = toolName.isEmpty() ? "No Tool" : toolName;
        if (durMax > 0 && !toolName.isEmpty()) {
            info += "  " + durCurrent + "/" + durMax;
        }
        ProceduralRenderer.drawText(ctx, tr, info, textX, textY,
                ProceduralRenderer.COL_TEXT_PRIMARY);

        // Durability bar (positioned below text)
        int barX = x + (width - BAR_WIDTH) / 2;
        int barY = textY + tr.fontHeight + INNER_PAD;
        durabilityBar.setPosition(barX, barY);
        durabilityBar.applyHealthColors();
        durabilityBar.render(ctx, mouseX, mouseY, delta);

        // Tier label (below bar, if enabled)
        if (showTier && !tierLabel.isEmpty()) {
            int tierY = barY + BAR_HEIGHT + INNER_PAD;
            ProceduralRenderer.drawText(ctx, tr, tierLabel, textX, tierY, tierColor);
        }
    }

    // =====================================================================
    //  Positioning
    // =====================================================================

    private void reposition(int screenW, int screenH) {
        switch (anchor) {
            case TOP_LEFT     -> { x = marginX;                  y = marginY; }
            case TOP_RIGHT    -> { x = screenW - width - marginX; y = marginY; }
            case BOTTOM_LEFT  -> { x = marginX;                  y = screenH - height - marginY; }
            case BOTTOM_RIGHT -> { x = screenW - width - marginX; y = screenH - height - marginY; }
        }
    }

    // =====================================================================
    //  Durability API
    // =====================================================================

    /**
     * Update the durability display.
     *
     * @param current remaining durability
     * @param max     maximum durability
     * @param name    tool display name (e.g. "Diamond Pickaxe")
     */
    public void setDurability(int current, int max, String name) {
        this.durCurrent = current;
        this.durMax     = Math.max(max, 1);
        this.toolName   = name != null ? name : "";
        this.durabilityBar.setProgress((float) current / this.durMax);
    }

    /** Update only the current value (tool name and max remain). */
    public void setDurabilityCurrent(int current) {
        this.durCurrent = current;
        this.durabilityBar.setProgress((float) current / durMax);
    }

    /** Snap the bar to a value without easing (useful for init). */
    public void snapDurability(int current, int max, String name) {
        this.durCurrent = current;
        this.durMax     = Math.max(max, 1);
        this.toolName   = name != null ? name : "";
        this.durabilityBar.snapTo((float) current / this.durMax);
    }

    // =====================================================================
    //  Tier API
    // =====================================================================

    /**
     * Show a performance tier label below the durability bar.
     *
     * @param tier  display string (e.g. "Tier 3 — Efficiency V")
     * @param color text colour for the tier label
     */
    public void setTier(String tier, int color) {
        this.tierLabel = tier != null ? tier : "";
        this.tierColor = color;
        this.showTier  = true;
    }

    /** Show a tier label in the default teal accent colour. */
    public void setTier(String tier) {
        setTier(tier, ProceduralRenderer.COL_ACCENT_TEAL);
    }

    /** Hide the tier label and shrink the panel. */
    public void hideTier() {
        this.showTier = false;
    }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    /** Remove this overlay from the HUD. */
    public void remove() {
        REGISTERED.remove(this);
    }

    /** Remove all overlays from the HUD. */
    public static void removeAll() {
        REGISTERED.clear();
    }

    /** Get number of registered overlays. */
    public static int getOverlayCount() {
        return REGISTERED.size();
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    public void setAnchor(Anchor a)       { this.anchor = a; }
    public Anchor getAnchor()             { return anchor; }
    public void setMargin(int mx, int my) { this.marginX = mx; this.marginY = my; }
    public String getToolName()           { return toolName; }
    public String getTierLabel()          { return tierLabel; }
    public int getDurabilityCurrent()     { return durCurrent; }
    public int getDurabilityMax()         { return durMax; }
}
