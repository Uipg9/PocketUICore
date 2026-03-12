package com.pocketuicore.screen;

import com.pocketuicore.component.DarkPanel;
import com.pocketuicore.component.FocusManager;
import com.pocketuicore.component.NotificationManager;
import com.pocketuicore.component.UIComponent;
import com.pocketuicore.controller.ControllerHandler;
import com.pocketuicore.render.TooltipRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

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

    protected PocketScreen(Text title) {
        super(title);
        this.focusContext = getClass().getSimpleName();
    }

    protected PocketScreen(Text title, String focusContext) {
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
    public void close() {
        ControllerHandler.getInstance().disable();
        FocusManager.getInstance().popContext(focusContext);
        super.close();
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (root != null) {
            // Layer 1 — Main UI tree
            root.render(ctx, mouseX, mouseY, delta);

            // Layer 2 — Notifications (above UI, below tooltips)
            NotificationManager.renderAll(ctx, width, height, delta);

            // Layer 3 — Tooltips (always on top of everything)
            if (root instanceof DarkPanel panel) {
                panel.renderTooltipPass(ctx, mouseX, mouseY);
            } else {
                UIComponent.renderTooltip(ctx, root, mouseX, mouseY);
            }
            TooltipRenderer.flush(ctx);
        }
    }

    // =====================================================================
    //  Input forwarding
    // =====================================================================

    @Override
    public boolean mouseClicked(Click click, boolean fromKeyboard) {
        if (root != null && root.mouseClicked(click, fromKeyboard)) return true;
        return super.mouseClicked(click, fromKeyboard);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (root != null && root.mouseReleased(click)) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (root != null && root.mouseScrolled(mouseX, mouseY, hAmount, vAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (root != null && root.mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY)) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (root != null) root.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (root != null && root.keyPressed(input)) return true;
        return super.keyPressed(input);
    }
}
