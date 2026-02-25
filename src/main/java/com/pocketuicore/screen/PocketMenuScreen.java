package com.pocketuicore.screen;

import com.pocketuicore.component.*;
import com.pocketuicore.data.ObservableState;
import com.pocketuicore.economy.ClientNetworkHandler;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Pocket Menu Screen — Main GUI for the All-in-One mod.
 * <p>
 * Opens via {@code /p} or {@code /pocket}.  Uses PocketUICore's
 * {@link VerticalListPanel} for zero-manual-math layout, reactive
 * {@link ObservableState} bound to the server-synced
 * {@link ClientNetworkHandler#CLIENT_BALANCE} for the wallet display, and
 * {@link FocusManager} for controller/keyboard navigation.
 */
public class PocketMenuScreen extends Screen {

    private static final int PANEL_W = 250;
    private static final int PANEL_H = 210;

    private VerticalListPanel mainPanel;
    private ObservableState<Integer> walletState;

    public PocketMenuScreen() {
        super(Text.literal("Pocket Menu"));
    }

    // =====================================================================
    //  Initialisation (re-called on resize)
    // =====================================================================

    @Override
    protected void init() {
        super.init();

        FocusManager fm = FocusManager.getInstance();
        fm.clear();

        // ── Reactive wallet state (bound to server-synced balance) ────
        walletState = ClientNetworkHandler.CLIENT_BALANCE;

        // ── Main panel — centred on screen ───────────────────────────────
        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2;
        mainPanel = new VerticalListPanel(px, py, PANEL_W, PANEL_H, 14, 8);
        mainPanel.setCornerRadius(8);
        mainPanel.setBackgroundColor(ProceduralRenderer.COL_BG_PRIMARY);
        mainPanel.setBorderColor(ProceduralRenderer.COL_ACCENT);

        // ── Title label ──────────────────────────────────────────────────
        TextLabel titleLabel = new TextLabel(0, 0, 0, 18, "Pocket Menu",
                ProceduralRenderer.COL_ACCENT_TEAL, TextLabel.Align.CENTER, 1.3f);
        mainPanel.addChild(titleLabel);

        // ── Wallet balance (auto-updates via ObservableState binding) ────
        TextLabel walletLabel = new TextLabel(0, 0, 0, 14, "");
        walletLabel.setAlign(TextLabel.Align.CENTER);
        walletLabel.setColor(ProceduralRenderer.COL_SUCCESS);
        ObservableState<String> walletText = walletState.map(v -> "Balance: $" + v);
        ObservableState.bindText(walletText, walletLabel);
        mainPanel.addChild(walletLabel);

        // ── Estate item display (Diamond) ────────────────────────────────
        ItemDisplayComponent estateItem = new ItemDisplayComponent(
                0, 0, new ItemStack(Items.DIAMOND));
        estateItem.setDrawBackground(true);
        estateItem.setBackgroundColor(ProceduralRenderer.COL_BG_ELEVATED);
        estateItem.setBackgroundRadius(4);
        estateItem.setShowCount(false);
        estateItem.setTooltip("Estate Upgrade Token",
                "Tier: Diamond", "Cost: $100");
        mainPanel.addChild(estateItem);

        // ── Upgrade button ───────────────────────────────────────────────
        HoverButton upgradeBtn = new HoverButton(0, 0, 0, 26,
                "Upgrade Estate", () -> {
            if (walletState.get() != null && walletState.get() >= 100) {
                walletState.set(walletState.get() - 100);
            }
        });
        upgradeBtn.setTooltip("Click to upgrade your estate",
                "Costs $100 per upgrade");
        mainPanel.addChild(upgradeBtn);

        // ── Auto-layout ──────────────────────────────────────────────────
        mainPanel.layout();

        // ── Focus registration (controller / keyboard nav) ───────────────
        fm.register(upgradeBtn);
        fm.register(estateItem);
        fm.focusFirst();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dark overlay behind everything
        ProceduralRenderer.drawFullScreenOverlay(ctx, this.width, this.height,
                ProceduralRenderer.COL_OVERLAY);

        // Panel + all children
        mainPanel.render(ctx, mouseX, mouseY, delta);

        // Tooltips — must be last so they paint on top of everything
        UIComponent.renderTooltip(ctx, mainPanel, mouseX, mouseY);
    }

    // =====================================================================
    //  Input forwarding
    // =====================================================================

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (mainPanel.mouseClicked(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (mainPanel.mouseReleased(click.x(), click.y(), click.button())) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                  double hAmount, double vAmount) {
        if (mainPanel.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        int modifiers = keyInput.modifiers();
        FocusManager fm = FocusManager.getInstance();

        switch (keyCode) {
            case GLFW.GLFW_KEY_TAB -> {
                if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) fm.navigatePrevious();
                else fm.navigateNext();
                return true;
            }
            case GLFW.GLFW_KEY_UP    -> { fm.navigateDirection(FocusManager.Direction.UP);    return true; }
            case GLFW.GLFW_KEY_DOWN  -> { fm.navigateDirection(FocusManager.Direction.DOWN);  return true; }
            case GLFW.GLFW_KEY_LEFT  -> { fm.navigateDirection(FocusManager.Direction.LEFT);  return true; }
            case GLFW.GLFW_KEY_RIGHT -> { fm.navigateDirection(FocusManager.Direction.RIGHT); return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                fm.activateFocused();
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    @Override
    public void removed() {
        FocusManager.getInstance().clear();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
