package com.pocketuicore.screen;

import com.pocketcanvascore.render.PocketCanvas;
import com.pocketuicore.component.DarkPanel;
import com.pocketuicore.component.FocusManager;
import com.pocketuicore.component.NotificationManager;
import com.pocketuicore.component.UIComponent;
import com.pocketuicore.controller.ControllerHandler;
import com.pocketuicore.render.GuiGraphicsAdapter;
import com.pocketuicore.render.RenderContext;
import com.pocketuicore.render.RenderContexts;
import com.pocketuicore.render.TooltipRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Convenience base class for screens built with PocketUICore.
 * <p>
 * Handles all the repetitive boilerplate:
 * <ul>
 *   <li>{@link FocusManager} context push / pop</li>
 *   <li>{@link ControllerHandler} enable / disable</li>
 *   <li>Input forwarding to a root {@link UIComponent}</li>
 *   <li>Tooltip rendering (second pass)</li>
 * </ul>
 *
 * Subclasses override {@link #buildUI()} to construct their component
 * tree and return the root component.
 *
 * @since 1.12.0
 */
public abstract class PocketScreen extends Screen {

    /** The root component of this screen's UI tree. */
    protected UIComponent root;

    /** The FocusManager context name for this screen. */
    private final String focusContext;

    protected PocketScreen(Component title) {
        super(title);
        this.focusContext = getClass().getSimpleName();
    }

    protected PocketScreen(Component title, String focusContext) {
        super(title);
        this.focusContext = focusContext;
    }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    @Override
    protected void init() {
        super.init();
        FocusManager.getInstance().pushContext(focusContext);
        ControllerHandler.getInstance().enable();
        root = buildUI();
        if (root != null) {
            onBuildComplete(root);
        }
    }

    /**
     * Build the component tree for this screen.
     * Called once during {@link #init()}.
     * <p>
     * Use {@link #width} and {@link #height} (inherited from Screen) to
     * position your root component. For centering, call
     * {@link UIComponent#centerOnScreen(int, int)} on your root:
     * <pre>{@code
     *     DarkPanel panel = new DarkPanel(0, 0, 300, 200);
     *     panel.centerOnScreen(width, height);
     *     return panel;
     * }</pre>
     *
     * @return the root component (typically a DarkPanel)
     */
    protected abstract UIComponent buildUI();

    /**
     * Called after {@link #buildUI()} returns a non-null root.
     * Override to perform additional setup (e.g. focus initialization).
     * The default implementation does nothing.
     *
     * @param root the root component returned by buildUI
     * @since 1.14.0
     */
    protected void onBuildComplete(UIComponent root) {}

    @Override
    public void onClose() {
        NotificationManager.clearAll();
        ControllerHandler.getInstance().disable();
        FocusManager.getInstance().popContext(focusContext);
        super.onClose();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        GuiGraphicsAdapter nativeCtx = new GuiGraphicsAdapter(ctx, delta);
        PocketCanvas canvas = new PocketCanvas(nativeCtx);
        RenderContext rc = RenderContexts.from(canvas);
        if (root != null) {
            // Layer 1 - Main UI tree
            root.render(canvas, mouseX, mouseY, canvas.delta());

            // Layer 1.5 - Overlay pass (e.g. expanded dropdowns above siblings)
            root.renderOverlay(rc, mouseX, mouseY, canvas.delta());

            // Layer 2 - Notifications (above UI, below tooltips)
            NotificationManager.renderAll(rc, width, height, canvas.delta());

            // Layer 3 - Tooltips (always on top of everything)
            if (root instanceof DarkPanel panel) {
                panel.renderTooltipPass(rc, mouseX, mouseY);
            } else {
                UIComponent.renderTooltip(rc, root, mouseX, mouseY);
            }
            TooltipRenderer.flush(rc);
        }
    }

    // =====================================================================
    //  Input forwarding
    // =====================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent MouseButtonEvent, boolean fromKeyboard) {
        if (root != null && root.mouseClicked(MouseButtonEvent, fromKeyboard)) return true;
        return super.mouseClicked(MouseButtonEvent, fromKeyboard);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent MouseButtonEvent) {
        if (root != null && root.mouseReleased(MouseButtonEvent)) return true;
        return super.mouseReleased(MouseButtonEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (root != null && root.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent MouseButtonEvent, double deltaX, double deltaY) {
        if (root != null && root.mouseDragged(MouseButtonEvent.x(), MouseButtonEvent.y(), MouseButtonEvent.button(), deltaX, deltaY)) return true;
        return super.mouseDragged(MouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (root != null) root.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (root != null && root.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (root != null && root.charTyped(input)) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
