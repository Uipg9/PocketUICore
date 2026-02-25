package com.pocketuicore.controller;

import com.pocketuicore.component.FocusManager;
import com.pocketuicore.screen.PocketMenuScreen;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.virtualmouse.VirtualMouseBehaviour;

/**
 * Controlify Screen Processor for {@link PocketMenuScreen}
 * <p>
 * Tells Controlify that our screens handle controller input natively via
 * {@link ControllerHandler} and {@link FocusManager}.  The virtual mouse
 * is disabled to prevent double-handling of input.
 * <p>
 * When Controlify's button-mode is active, this processor intercepts
 * built-in GUI bindings and routes them to our focus system, providing
 * seamless integration with Controlify's rebinding and button-guide
 * rendering.
 */
public class PocketMenuScreenProcessor extends ScreenProcessor<PocketMenuScreen> {

    public PocketMenuScreenProcessor(PocketMenuScreen screen) {
        super(screen);
    }

    // ── Disable vmouse on PocketUI screens ───────────────────────────────
    @Override
    public VirtualMouseBehaviour virtualMouseBehaviour() {
        return VirtualMouseBehaviour.DISABLED;
    }

    // ── Button handling (vmouse OFF) ─────────────────────────────────────
    @Override
    protected void handleButtons(ControllerEntity controller) {
        // Let the base processor run first (handles some defaults)
        super.handleButtons(controller);

        FocusManager fm = FocusManager.getInstance();

        // GUI_PRESS → activate focused component
        if (ControlifyBindings.GUI_PRESS.on(controller).justPressed()) {
            fm.activateFocused();
        }

        // GUI_BACK → close screen
        if (ControlifyBindings.GUI_BACK.on(controller).justPressed()) {
            this.screen.close();
        }

        // D-pad / navigation is handled by ControllerHandler's GLFW polling,
        // so we don't duplicate it here. Controlify and our handler coexist:
        // Controlify disables vmouse via this processor, and ControllerHandler
        // handles the actual directional navigation + cursor snapping.
    }
}
