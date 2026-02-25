package com.pocketuicore.economy;

import com.pocketuicore.PocketUICore;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EstateManager — Server-side passive income system.
 * <p>
 * Every server tick, each online player's "estate growth" percentage
 * increments by a configurable rate (see {@link EconomyConfig}).
 * When that percentage reaches {@code 100.0f} the player automatically
 * receives a passive-income payout via {@link EconomyManager#addMoney}
 * and the cycle resets.
 * <p>
 * <b>Persistence:</b> Estate growth is saved into the player's vault
 * state on disconnect and restored on join, so progress is never lost
 * across sessions.
 * <p>
 * The current growth percentage is synced to the client via
 * {@link SyncEstatePayload} so the {@code /pocket} menu can render a
 * smoothly-filling progress bar without desyncing.
 *
 * <b>Integration:</b>
 * <pre>{@code
 *     // Server entrypoint:
 *     ServerTickEvents.END_SERVER_TICK.register(EstateManager::tick);
 *
 *     // Query from anywhere server-side:
 *     float pct = EstateManager.getGrowth(player.getUuid());
 * }</pre>
 */
public final class EstateManager {

    private EstateManager() { /* utility class */ }

    // =====================================================================
    //  Configuration — now externalised to EconomyConfig
    // =====================================================================

    // =====================================================================
    //  State
    // =====================================================================

    /** Per-player growth percentage, 0.0–100.0. */
    private static final Map<UUID, Float> growthMap = new ConcurrentHashMap<>();

    /** Tick counter for throttled sync. */
    private static int tickCounter = 0;

    // =====================================================================
    //  Tick — called from ServerTickEvents.END_SERVER_TICK
    // =====================================================================

    /**
     * Advance estate growth for every online player.
     * Must be called once per server tick.
     *
     * @param server the current MinecraftServer instance
     */
    public static void tick(MinecraftServer server) {
        tickCounter++;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            float current = growthMap.getOrDefault(uuid, 0.0f);
            current += EconomyConfig.ESTATE_GROWTH_PER_TICK;

            if (current >= 100.0f) {
                current = 0.0f;
                EconomyManager.addMoney(player, EconomyConfig.ESTATE_PAYOUT_AMOUNT);
                PocketUICore.LOGGER.debug("[Estate] Payout ${} → {}",
                        EconomyConfig.ESTATE_PAYOUT_AMOUNT, player.getName().getString());
            }

            growthMap.put(uuid, current);

            // Sync to client at throttled rate
            if (tickCounter % EconomyConfig.ESTATE_SYNC_INTERVAL == 0) {
                syncToClient(player, current);
            }
        }
    }

    // =====================================================================
    //  Queries
    // =====================================================================

    /**
     * Get a player's current estate growth percentage.
     *
     * @param uuid the player's UUID
     * @return growth percentage 0.0f–100.0f (0.0f if unknown)
     */
    public static float getGrowth(UUID uuid) {
        return growthMap.getOrDefault(uuid, 0.0f);
    }

    /**
     * Reset a player's growth (e.g. on disconnect cleanup).
     * Also persists the current value before removing from memory.
     *
     * @param uuid   the player's UUID
     * @param server the MinecraftServer (for persistence)
     */
    public static void resetGrowth(UUID uuid, MinecraftServer server) {
        float current = growthMap.getOrDefault(uuid, 0.0f);
        if (server != null && current > 0.0f) {
            EstateGrowthState state = EstateGrowthState.getServerState(server);
            state.setGrowth(uuid, current);
        }
        growthMap.remove(uuid);
    }

    /**
     * Reset a player's growth without saving (legacy/simple cleanup).
     *
     * @param uuid the player's UUID
     */
    public static void resetGrowth(UUID uuid) {
        growthMap.remove(uuid);
    }

    // =====================================================================
    //  Persistence
    // =====================================================================

    /**
     * Load a player's saved estate growth from disk into the in-memory
     * map.  Call on player join.
     *
     * @param player the joining player
     * @param server the MinecraftServer
     */
    public static void loadGrowth(ServerPlayerEntity player, MinecraftServer server) {
        EstateGrowthState state = EstateGrowthState.getServerState(server);
        float saved = state.getGrowth(player.getUuid());
        if (saved > 0.0f) {
            growthMap.put(player.getUuid(), saved);
        }
    }

    /**
     * Save a player's current estate growth to disk.
     * Call on player disconnect.
     *
     * @param player the disconnecting player
     * @param server the MinecraftServer
     */
    public static void saveGrowth(ServerPlayerEntity player, MinecraftServer server) {
        UUID uuid = player.getUuid();
        float current = growthMap.getOrDefault(uuid, 0.0f);
        EstateGrowthState state = EstateGrowthState.getServerState(server);
        state.setGrowth(uuid, current);
        growthMap.remove(uuid);
    }

    // =====================================================================
    //  Sync
    // =====================================================================

    /**
     * Send the current estate growth percentage to the player's client.
     *
     * @param player     the target player
     * @param percentage current growth (0.0–100.0)
     */
    public static void syncToClient(ServerPlayerEntity player, float percentage) {
        ServerPlayNetworking.send(player, new SyncEstatePayload(percentage));
    }

    /**
     * Force-sync the current estate growth to a player (e.g. on join).
     *
     * @param player the target player
     */
    public static void syncOnJoin(ServerPlayerEntity player) {
        float pct = growthMap.getOrDefault(player.getUuid(), 0.0f);
        syncToClient(player, pct);
    }
}
