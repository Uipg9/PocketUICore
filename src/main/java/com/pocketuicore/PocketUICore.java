package com.pocketuicore;

import com.pocketuicore.economy.EconomyManager;
import com.pocketuicore.economy.SyncBalancePayload;
import net.fabricmc.api.ModInitializer;
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
        // Register S2C payload type so the server can send balance packets
        PayloadTypeRegistry.playS2C()
                .register(SyncBalancePayload.ID, SyncBalancePayload.CODEC);

        // Sync balance to client when a player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                EconomyManager.syncOnJoin(handler.player)
        );

        LOGGER.info("[PocketUICore] Core library loaded — procedural UI, economy, notifications, animations ready.");
    }
}
