package com.pocketuicore;

import com.pocketuicore.animation.AnimationTicker;
import com.pocketuicore.command.PocketCommandRegister;
import com.pocketuicore.component.HudOverlayComponent;
import com.pocketuicore.controller.ControllerHandler;
import com.pocketuicore.economy.ClientNetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * PocketUICore — Client-side entrypoint.
 * <p>
 * Registers the animation ticker on the client tick loop.
 * All rendering is driven by screens that consume the library;
 * this entrypoint just keeps the animation engine pumping.
 */
public class PocketUICoreClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Pump the animation engine every client tick (~20 Hz).
        // AnimationTicker itself uses nanoTime for sub-tick interpolation,
        // but the tick callback lets it clean up completed animations.
        // Runs unconditionally so animations in menus / title screens also tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AnimationTicker.getInstance().tick();
            ControllerHandler.getInstance().tick();
        });

        // Register HUD overlay rendering
        HudRenderCallback.EVENT.register(HudOverlayComponent::renderAll);

        // Register /pocket and /p client commands
        PocketCommandRegister.register();

        // Register client-side economy payload receiver
        ClientNetworkHandler.register();

        PocketUICore.LOGGER.info("[PocketUICore] Client systems initialized — animation ticker + controller handler + HUD overlay + commands + economy sync running.");
    }
}
