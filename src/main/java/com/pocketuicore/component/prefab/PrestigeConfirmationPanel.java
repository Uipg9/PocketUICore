package com.pocketuicore.component.prefab;

import com.pocketuicore.component.DarkPanel;
import com.pocketuicore.component.HoverButton;
import com.pocketuicore.component.UIComponent;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Prefab: Prestige Confirmation Panel
 * <p>
 * A single-line-constructible dialog that warns the player about
 * prestige mechanics and provides Confirm / Cancel actions.
 * <p>
 * <b>Prestige rules displayed:</b>
 * <ul>
 *   <li>Ender Chest will be wiped clean</li>
 *   <li>Player keeps exactly <em>one</em> item of their choice</li>
 *   <li>All progress resets to Tier 1</li>
 *   <li>The "Beacon Effect" helps locate old items post-prestige</li>
 * </ul>
 * <p>
 * <b>Usage (one line):</b>
 * <pre>{@code
 *     PrestigeConfirmationPanel panel = new PrestigeConfirmationPanel(
 *         screenWidth / 2, screenHeight / 2,
 *         () -> { confirmLogic(); },
 *         () -> { cancelLogic();  }
 *     );
 * }</pre>
 */
public class PrestigeConfirmationPanel extends DarkPanel {

    // ── Layout constants ─────────────────────────────────────────────────
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 210;
    private static final int PAD     = 14;
    private static final int BTN_W   = 110;
    private static final int BTN_H   = 22;
    private static final int LINE_H  = 12;

    // ── State ────────────────────────────────────────────────────────────
    private boolean beaconEffectEnabled = true;
    private final HoverButton beaconToggle;
    private final HoverButton confirmBtn;
    private final HoverButton cancelBtn;

    // ── Callbacks ────────────────────────────────────────────────────────
    private Runnable onConfirm;
    private Runnable onCancel;

    // =====================================================================
    //  Construction
    // =====================================================================

    /**
     * Build the prestige confirmation dialog, centred on screen.
     *
     * @param centerX   screen centre X
     * @param centerY   screen centre Y
     * @param onConfirm action when player confirms prestige
     * @param onCancel  action when player cancels
     */
    public PrestigeConfirmationPanel(int centerX, int centerY,
                                      Runnable onConfirm, Runnable onCancel) {
        super(centerX - PANEL_W / 2, centerY - PANEL_H / 2, PANEL_W, PANEL_H);
        this.onConfirm = onConfirm;
        this.onCancel  = onCancel;

        setCornerRadius(8);
        setBackgroundColor(ProceduralRenderer.COL_BG_PRIMARY);
        setBorderColor(ProceduralRenderer.COL_ACCENT);

        int px = getX();
        int py = getY();

        // ── Beacon Effect toggle (acts as a button that flips state) ─────
        int toggleY = py + 124;
        beaconToggle = new HoverButton(
                px + PAD, toggleY, PANEL_W - PAD * 2, 18,
                beaconToggleLabel(), this::toggleBeacon);
        beaconToggle.setNormalColor(ProceduralRenderer.COL_BG_ELEVATED);
        beaconToggle.setHoverColor(ProceduralRenderer.COL_HOVER);
        beaconToggle.setCornerRadius(3);
        addChild(beaconToggle);

        // ── Buttons ──────────────────────────────────────────────────────
        int btnY = py + PANEL_H - PAD - BTN_H;
        int gap  = (PANEL_W - BTN_W * 2) / 3;

        // Confirm — vibrant accent red to reinforce "dangerous action"
        confirmBtn = new HoverButton(
                px + gap, btnY, BTN_W, BTN_H,
                "Confirm Prestige", () -> { if (this.onConfirm != null) this.onConfirm.run(); });
        confirmBtn.setNormalColor(ProceduralRenderer.COL_ACCENT);
        confirmBtn.setHoverColor(0xFFFF5A75);
        confirmBtn.setPressedColor(0xFFCC3040);
        addChild(confirmBtn);

        // Cancel — subdued
        cancelBtn = new HoverButton(
                px + gap * 2 + BTN_W, btnY, BTN_W, BTN_H,
                "Cancel", () -> { if (this.onCancel != null) this.onCancel.run(); });
        cancelBtn.setNormalColor(ProceduralRenderer.COL_BG_ELEVATED);
        cancelBtn.setHoverColor(ProceduralRenderer.COL_HOVER);
        addChild(cancelBtn);
    }

    // =====================================================================
    //  Rendering — text labels drawn procedurally; buttons are children
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Draw panel background (shadow, fill, border) via DarkPanel
        super.renderSelf(ctx, mouseX, mouseY, delta);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int cx = x + width / 2;
        int py = y + PAD;

        // ── Title ────────────────────────────────────────────────────────
        ProceduralRenderer.drawCenteredText(ctx, tr,
                "!! Prestige Confirmation", cx, py, ProceduralRenderer.COL_WARNING);
        py += LINE_H + 2;

        // ── Divider ──────────────────────────────────────────────────────
        ProceduralRenderer.drawDivider(ctx, x + PAD, py, width - PAD * 2,
                ProceduralRenderer.COL_BORDER);
        py += 6;

        // ── Warning lines ────────────────────────────────────────────────
        ProceduralRenderer.drawText(ctx, tr,
                "This action CANNOT be undone:", x + PAD, py,
                ProceduralRenderer.COL_ERROR);
        py += LINE_H;

        ProceduralRenderer.drawText(ctx, tr,
                "- Your Ender Chest will be wiped.", x + PAD + 4, py,
                ProceduralRenderer.COL_TEXT_PRIMARY);
        py += LINE_H;

        ProceduralRenderer.drawText(ctx, tr,
                "- You may keep exactly ONE item.", x + PAD + 4, py,
                ProceduralRenderer.COL_TEXT_PRIMARY);
        py += LINE_H;

        ProceduralRenderer.drawText(ctx, tr,
                "- All progress resets to Tier 1.", x + PAD + 4, py,
                ProceduralRenderer.COL_TEXT_PRIMARY);
        py += LINE_H + 8;

        // ── Beacon Effect section ────────────────────────────────────────
        ProceduralRenderer.drawDivider(ctx, x + PAD, py, width - PAD * 2,
                ProceduralRenderer.COL_BORDER);
        py += 6;

        String beaconLabel = "Beacon Effect:";
        ProceduralRenderer.drawText(ctx, tr,
                beaconLabel, x + PAD, py,
                ProceduralRenderer.COL_ACCENT_TEAL);
        ProceduralRenderer.drawText(ctx, tr,
                " Reveals traces of your", x + PAD + tr.getWidth(beaconLabel), py,
                ProceduralRenderer.COL_TEXT_MUTED);
        py += LINE_H;

        ProceduralRenderer.drawText(ctx, tr,
                "old items for 5 minutes after prestige.",
                x + PAD, py, ProceduralRenderer.COL_TEXT_MUTED);
        // Toggle button is positioned as a child — renders automatically
    }

    // =====================================================================
    //  Beacon Effect toggle
    // =====================================================================

    private void toggleBeacon() {
        beaconEffectEnabled = !beaconEffectEnabled;
        beaconToggle.setLabel(beaconToggleLabel());
    }

    private String beaconToggleLabel() {
        return "Beacon Effect: " + (beaconEffectEnabled ? "[ON]" : "[OFF]");
    }

    /** @return whether the player has the Beacon Effect toggle on. */
    public boolean isBeaconEffectEnabled() {
        return beaconEffectEnabled;
    }

    /** Set the beacon effect toggle state programmatically. */
    public void setBeaconEffectEnabled(boolean enabled) {
        this.beaconEffectEnabled = enabled;
        beaconToggle.setLabel(beaconToggleLabel());
    }

    // =====================================================================
    //  Callbacks
    // =====================================================================

    public void setOnConfirm(Runnable r) { this.onConfirm = r; }
    public void setOnCancel(Runnable r)  { this.onCancel = r; }

    /**
     * Convenience: reposition the panel (and all children) to a new centre.
     * Useful after a window resize.
     */
    public void center(int screenW, int screenH) {
        int newX = (screenW - PANEL_W) / 2;
        int newY = (screenH - PANEL_H) / 2;
        int dx = newX - x;
        int dy = newY - y;
        setPosition(newX, newY);
        for (UIComponent child : getChildren()) {
            child.setPosition(child.getX() + dx, child.getY() + dy);
        }
    }
}
