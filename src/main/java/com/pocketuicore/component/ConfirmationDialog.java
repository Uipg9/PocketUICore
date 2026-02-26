package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/**
 * Modal confirmation dialog — darkened overlay, centred panel with
 * title, message, and Confirm / Cancel buttons.
 * <p>
 * <ul>
 *   <li>Auto-focuses Cancel for safety (user must deliberately choose Confirm).</li>
 *   <li>Escape key cancels.</li>
 *   <li>Optional auto-cancel timeout (e.g. "Cancelling in 10 s…").</li>
 *   <li>Pushes a FocusManager context on show and pops it on dismiss.</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     ConfirmationDialog dialog = new ConfirmationDialog(
 *            screenW, screenH,
 *            "Delete All?",
 *            "This action cannot be undone.",
 *            () -> deleteAll(),
 *            () -> {}  // cancel no-op
 *     );
 *     dialog.show();
 *     // in render():  dialog.render(ctx, mouseX, mouseY, delta);
 *     // in keyPressed(): dialog.keyPressed(keyCode, scanCode, modifiers);
 * }</pre>
 */
public final class ConfirmationDialog extends UIComponent {

    // ── Layout constants ─────────────────────────────────────────────────
    private static final int DIALOG_WIDTH  = 240;
    private static final int DIALOG_HEIGHT = 130;
    private static final int BUTTON_W      = 90;
    private static final int BUTTON_H      = 22;
    private static final int PAD           = 12;

    // ── Internals ────────────────────────────────────────────────────────
    private final DarkPanel panel;
    private final TextLabel titleLabel;
    private final TextLabel messageLabel;
    private final HoverButton confirmButton;
    private final HoverButton cancelButton;

    private final Runnable onConfirm;
    private final Runnable onCancel;

    private boolean active = false;

    // ── Optional timeout ─────────────────────────────────────────────────
    private long timeoutMs = 0;       // 0 = no timeout
    private long showTimeMs;

    // ── Focus context key ────────────────────────────────────────────────
    private static final String FOCUS_CTX = "pocketui_confirm_dialog";

    // =====================================================================
    //  Constructor
    // =====================================================================

    /**
     * @param screenW   full screen width (for centering)
     * @param screenH   full screen height (for centering)
     * @param title     dialog title
     * @param message   descriptive message
     * @param onConfirm callback when confirmed
     * @param onCancel  callback when cancelled
     */
    public ConfirmationDialog(int screenW, int screenH,
                              String title, String message,
                              Runnable onConfirm, Runnable onCancel) {
        super(0, 0, screenW, screenH);
        this.onConfirm = onConfirm;
        this.onCancel  = onCancel;

        int px = (screenW - DIALOG_WIDTH) / 2;
        int py = (screenH - DIALOG_HEIGHT) / 2;

        // Panel
        panel = new DarkPanel(px, py, DIALOG_WIDTH, DIALOG_HEIGHT,
                ProceduralRenderer.COL_BG_PRIMARY, ProceduralRenderer.COL_ACCENT,
                8, true, true);

        // Title
        titleLabel = new TextLabel(
                px + PAD, py + PAD,
                DIALOG_WIDTH - PAD * 2, 12,
                title, ProceduralRenderer.COL_TEXT_PRIMARY
        );
        titleLabel.setAlign(TextLabel.Align.CENTER);

        // Message
        messageLabel = new TextLabel(
                px + PAD, py + PAD + 22,
                DIALOG_WIDTH - PAD * 2, 40,
                message, ProceduralRenderer.COL_TEXT_MUTED
        );
        messageLabel.setAlign(TextLabel.Align.CENTER);
        messageLabel.setWrapWidth(DIALOG_WIDTH - PAD * 2);

        // Buttons
        int btnY = py + DIALOG_HEIGHT - BUTTON_H - PAD;
        int gap  = 12;
        int totalW = BUTTON_W * 2 + gap;
        int bx   = px + (DIALOG_WIDTH - totalW) / 2;

        confirmButton = new HoverButton(
                bx, btnY, BUTTON_W, BUTTON_H,
                "Confirm", this::doConfirm,
                ProceduralRenderer.COL_SUCCESS,
                ProceduralRenderer.lighten(ProceduralRenderer.COL_SUCCESS, 0.2f),
                ProceduralRenderer.darken(ProceduralRenderer.COL_SUCCESS, 0.2f),
                ProceduralRenderer.COL_TEXT_PRIMARY, 4
        );

        cancelButton = new HoverButton(
                bx + BUTTON_W + gap, btnY, BUTTON_W, BUTTON_H,
                "Cancel", this::doCancel
        );

        // Build tree
        addChild(panel);
        panel.addChild(titleLabel);
        panel.addChild(messageLabel);
        panel.addChild(confirmButton);
        panel.addChild(cancelButton);

        // Start invisible
        visible = false;
    }

    // =====================================================================
    //  Show / Dismiss
    // =====================================================================

    /** Show the dialog (pushes focus context, focuses Cancel). */
    public void show() {
        visible = true;
        active  = true;
        showTimeMs = System.currentTimeMillis();
        FocusManager.getInstance().pushContext(FOCUS_CTX);
        FocusManager.getInstance().register(cancelButton);
        FocusManager.getInstance().register(confirmButton);
        FocusManager.getInstance().focus(cancelButton);
        UISoundManager.playClick();
    }

    /** @return {@code true} if the dialog is currently visible. */
    public boolean isActive() { return active; }

    private void dismiss() {
        visible = false;
        active  = false;
        FocusManager.getInstance().popContext();
    }

    private void doConfirm() {
        dismiss();
        onConfirm.run();
    }

    private void doCancel() {
        dismiss();
        onCancel.run();
    }

    // =====================================================================
    //  Timeout
    // =====================================================================

    /**
     * Set an auto-cancel timeout. The dialog will cancel itself after the
     * specified duration.
     *
     * @param timeoutMs timeout in milliseconds (0 to disable)
     */
    public ConfirmationDialog setTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    @Override
    protected void renderSelf(DrawContext ctx, int mouseX, int mouseY,
                              float delta) {
        if (!active) return;

        // ── Timeout check ────────────────────────────────────────────────
        if (timeoutMs > 0) {
            long elapsed = System.currentTimeMillis() - showTimeMs;
            if (elapsed >= timeoutMs) {
                doCancel();
                return;
            }
        }

        // ── Darkened overlay ─────────────────────────────────────────────
        ProceduralRenderer.drawFullScreenOverlay(ctx, width, height, ProceduralRenderer.COL_OVERLAY);
    }

    // =====================================================================
    //  Input
    // =====================================================================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            doCancel();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            // If Cancel is focused, cancel; if Confirm is focused, confirm
            UIComponent focused = FocusManager.getInstance().getFocused();
            if (focused == confirmButton) {
                doConfirm();
            } else {
                doCancel();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active) return false;
        // Consume all clicks (modal)
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!active) return false;
        super.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    // =====================================================================
    //  Accessors
    // =====================================================================

    /** Change the title text. */
    public void setTitle(String title) { titleLabel.setText(title); }

    /** Change the message text. */
    public void setMessage(String message) { messageLabel.setText(message); }

    /** Change the confirm button label. */
    public void setConfirmLabel(String label) { confirmButton.setLabel(label); }

    /** Change the cancel button label. */
    public void setCancelLabel(String label) { cancelButton.setLabel(label); }
}
