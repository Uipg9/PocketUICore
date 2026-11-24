package com.pocketuicore.screen;

import com.pocketcanvascore.render.PocketCanvas;
import com.pocketuicore.component.FocusManager;
import com.pocketuicore.component.HoverButton;
import com.pocketuicore.component.ItemDisplayComponent;
import com.pocketuicore.component.PercentageBar;
import com.pocketuicore.component.TextLabel;
import com.pocketuicore.component.UIComponent;
import com.pocketuicore.component.VerticalListPanel;
import com.pocketuicore.controller.ControllerHandler;
import com.pocketuicore.data.ObservableState;
import com.pocketuicore.economy.ClientNetworkHandler;
import com.pocketuicore.render.GuiGraphicsAdapter;
import com.pocketuicore.render.ProceduralRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import com.pocketuicore.input.PocketKeys;

/**
 * Pocket Menu Screen - Main GUI for the All-in-One mod.
 * <p>
 * Opens via {@code /p} or {@code /pocket}. Uses PocketUICore's
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
        super(Component.literal("Pocket Menu"));
    }

    // =====================================================================
    //  Initialisation (re-called on resize)
    // =====================================================================

    @Override
    protected void init() {
        super.init();

        FocusManager fm = FocusManager.getInstance();
        fm.clear();

        // Reactive wallet state (bound to server-synced balance)
        walletState = ClientNetworkHandler.CLIENT_BALANCE;

        // Main panel - centred on screen
        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2;
        mainPanel = new VerticalListPanel(px, py, PANEL_W, PANEL_H, 14, 8);
        mainPanel.setCornerRadius(8);
        mainPanel.setBackgroundColor(ProceduralRenderer.COL_BG_PRIMARY);
        mainPanel.setBorderColor(ProceduralRenderer.COL_ACCENT);

        // Title label
        TextLabel titleLabel = new TextLabel(0, 0, 0, 18, "Pocket Menu",
                ProceduralRenderer.COL_ACCENT_TEAL, TextLabel.Align.CENTER, 1.3f);
        mainPanel.addChild(titleLabel);

        // Wallet balance (auto-updates via ObservableState binding)
        TextLabel walletLabel = new TextLabel(0, 0, 0, 14, "");
        walletLabel.setAlign(TextLabel.Align.CENTER);
        walletLabel.setColor(ProceduralRenderer.COL_SUCCESS);
        ObservableState<String> walletText = walletState.map(v -> "Balance: $" + v);
        ObservableState.bindText(walletText, walletLabel);
        mainPanel.addChild(walletLabel);

        // Estate Growth Progress Bar (live server-synced)
        TextLabel estateLabel = new TextLabel(0, 0, 0, 14, "");
        estateLabel.setAlign(TextLabel.Align.CENTER);
        estateLabel.setColor(ProceduralRenderer.COL_ACCENT_TEAL);
        ObservableState<String> estateText = ClientNetworkHandler.CLIENT_ESTATE_GROWTH
                .map(pct -> String.format("Estate Growth: %.1f%%", pct));
        ObservableState.bindText(estateText, estateLabel);
        mainPanel.addChild(estateLabel);

        PercentageBar estateBar = new PercentageBar(0, 0, 0, 12, 0.0f);
        estateBar.setBarColor(ProceduralRenderer.COL_ACCENT_TEAL);
        estateBar.setTooltip("Passive Income", "Earn $50 when bar fills");
        ObservableState.bindProgress(
                ClientNetworkHandler.CLIENT_ESTATE_GROWTH.map(pct -> pct / 100.0f),
                estateBar);
        mainPanel.addChild(estateBar);

        // Estate item display (Diamond)
        ItemDisplayComponent estateItem = new ItemDisplayComponent(
                0, 0, new ItemStack(Items.DIAMOND));
        estateItem.setDrawBackground(true);
        estateItem.setBackgroundColor(ProceduralRenderer.COL_BG_ELEVATED);
        estateItem.setBackgroundRadius(4);
        estateItem.setShowCount(false);
        estateItem.setTooltip("Estate Upgrade Token",
                "Tier: Diamond", "Cost: $100");
        mainPanel.addChild(estateItem);

        // Upgrade button
        HoverButton upgradeBtn = new HoverButton(0, 0, 0, 26,
                "Upgrade Estate", () -> {
            Integer bal = walletState.get();
            if (bal != null && bal >= 100) {
                walletState.set(bal - 100);
            }
        });
        upgradeBtn.setTooltip("Click to upgrade your estate",
                "Costs $100 per upgrade");
        mainPanel.addChild(upgradeBtn);

        // Auto-layout
        mainPanel.layout();

        // Focus registration (controller / keyboard nav)
        fm.register(upgradeBtn);
        fm.register(estateItem);
        fm.register(estateBar);
        fm.focusFirst();

        // Controller support - enable gamepad polling + scroll
        ControllerHandler ch = ControllerHandler.getInstance();
        ch.setScrollTarget(mainPanel);
        ch.enable();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        GuiGraphicsAdapter rc = new GuiGraphicsAdapter(ctx, delta);
        PocketCanvas canvas = new PocketCanvas(rc);

        // Dark overlay behind everything
        ProceduralRenderer.drawFullScreenOverlay(rc, this.width, this.height,
                ProceduralRenderer.COL_OVERLAY);

        // Panel + all children
        mainPanel.render(canvas, mouseX, mouseY, canvas.delta());

        // Tooltips - must be last so they paint on top of everything
        UIComponent.renderTooltip(rc, mainPanel, mouseX, mouseY);
    }

    // =====================================================================
    //  Input forwarding
    // =====================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent MouseButtonEvent, boolean bl) {
        if (mainPanel.mouseClicked(MouseButtonEvent.x(), MouseButtonEvent.y(), MouseButtonEvent.button())) return true;
        return super.mouseClicked(MouseButtonEvent, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent MouseButtonEvent) {
        if (mainPanel.mouseReleased(MouseButtonEvent.x(), MouseButtonEvent.y(), MouseButtonEvent.button())) return true;
        return super.mouseReleased(MouseButtonEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                  double hAmount, double vAmount) {
        if (mainPanel.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent MouseButtonEvent, double deltaX, double deltaY) {
        if (mainPanel.mouseDragged(MouseButtonEvent.x(), MouseButtonEvent.y(), MouseButtonEvent.button(), deltaX, deltaY)) return true;
        return super.mouseDragged(MouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent KeyEvent) {
        int keyCode = KeyEvent.key();
        int modifiers = KeyEvent.modifiers();
        FocusManager fm = FocusManager.getInstance();

        switch (keyCode) {
            case PocketKeys.KEY_TAB -> {
                if ((modifiers & PocketKeys.MOD_SHIFT) != 0) fm.navigatePrevious();
                else fm.navigateNext();
                return true;
            }
            case PocketKeys.KEY_UP    -> { fm.navigateDirection(FocusManager.Direction.UP);    return true; }
            case PocketKeys.KEY_DOWN  -> { fm.navigateDirection(FocusManager.Direction.DOWN);  return true; }
            case PocketKeys.KEY_LEFT  -> { fm.navigateDirection(FocusManager.Direction.LEFT);  return true; }
            case PocketKeys.KEY_RIGHT -> { fm.navigateDirection(FocusManager.Direction.RIGHT); return true; }
            case PocketKeys.KEY_ENTER, PocketKeys.KEY_KP_ENTER -> {
                fm.activateFocused();
                return true;
            }
        }
        return super.keyPressed(KeyEvent);
    }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    @Override
    public void removed() {
        ControllerHandler.getInstance().disable();
        FocusManager.getInstance().clear();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
