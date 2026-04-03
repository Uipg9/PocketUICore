package com.pocketuicore.compat;

import com.pocketuicore.component.HudOverlayComponent;
import com.pocketuicore.render.DrawContextAdapter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class HudCompat {
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
            HudOverlayComponent.renderAll(
                new DrawContextAdapter(drawContext),
                tickCounter.getTickProgress(false)));
    }
}
