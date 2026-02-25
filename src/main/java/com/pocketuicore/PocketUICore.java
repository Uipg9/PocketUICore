package com.pocketuicore;

import com.pocketuicore.economy.EconomyManager;
import com.pocketuicore.economy.EstateManager;
import com.pocketuicore.economy.SyncBalancePayload;
import com.pocketuicore.economy.SyncEstatePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PocketUICore — Server-side entrypoint.
 * <p>
 * Registers server-side systems (notification manager, economy payload, etc.).
 * All rendering and animation lives in the client entrypoint.
 */
public class PocketUICore implements ModInitializer {

    public static final String MOD_ID = "pocketuicore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register S2C payload types
        PayloadTypeRegistry.playS2C()
                .register(SyncBalancePayload.ID, SyncBalancePayload.CODEC);
        PayloadTypeRegistry.playS2C()
                .register(SyncEstatePayload.ID, SyncEstatePayload.CODEC);

        // Sync balance + estate growth to client when a player joins
        // Also restore persisted estate growth from disk
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            EstateManager.loadGrowth(handler.player, server);
            EconomyManager.syncOnJoin(handler.player);
            EstateManager.syncOnJoin(handler.player);
        });

        // Save estate growth to disk on disconnect, then clean up
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                EstateManager.saveGrowth(handler.player, server)
        );

        // Tick the estate passive-income system every server tick
        ServerTickEvents.END_SERVER_TICK.register(EstateManager::tick);

        LOGGER.info("[PocketUICore] Core library loaded — procedural UI, economy, estate, caravan, notifications, animations ready.");
    }
}
