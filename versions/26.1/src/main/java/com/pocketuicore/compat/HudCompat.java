package com.pocketuicore.compat;

import com.pocketuicore.component.HudOverlayComponent;
import com.pocketuicore.render.GuiGraphicsAdapter;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public final class HudCompat {
    public static void register() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("pocketuicore", "main_overlay"),
            (guiGraphics, tickCounter) ->
                HudOverlayComponent.renderAll(
                    new GuiGraphicsAdapter(guiGraphics),
                    tickCounter.getGameTimeDeltaPartialTick(false)));
    }
}
