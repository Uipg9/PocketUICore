package com.pocketuicore.economy;

import com.pocketuicore.PocketUICore;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * EconomyManager — Server-side utility for modifying player balances.
 * <p>
 * Every mutation automatically persists (via {@link PlayerVaultState#markDirty()})
 * and syncs the new balance to the affected client with a
 * {@link SyncBalancePayload}.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     EconomyManager.addMoney(player, 250);
 *     EconomyManager.removeMoney(player, 100);
 *     int bal = EconomyManager.getBalance(player);
 * }</pre>
 */
public final class EconomyManager {

    private EconomyManager() { /* utility class */ }

    // =====================================================================
    //  Balance queries
    // =====================================================================

    /**
     * Get a player's current balance.
     *
     * @param player the server-side player
     * @return current balance (0 if unset)
     */
    public static int getBalance(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        PlayerVaultState vault = PlayerVaultState.getServerState(server);
        return vault.getBalance(player.getUUID());
    }

    // =====================================================================
    //  Balance mutations
    // =====================================================================

    /**
     * Add money to a player's balance, persist, and sync to client.
     *
     * @param player the server-side player
     * @param amount positive amount to add
     */
    public static void addMoney(ServerPlayer player, int amount) {
        if (amount <= 0) return;

        MinecraftServer server = player.level().getServer();
        PlayerVaultState vault = PlayerVaultState.getServerState(server);

        vault.addBalance(player.getUUID(), amount);
        syncToClient(player, vault.getBalance(player.getUUID()));

        PocketUICore.LOGGER.debug("[Economy] +{} → {} (total: {})",
                amount, player.getName().getString(),
                vault.getBalance(player.getUUID()));
    }

    /**
     * Remove money from a player's balance, persist, and sync to client.
     * <p>
     * If the player does not have sufficient funds the operation is
     * silently skipped and {@code false} is returned.
     *
     * @param player the server-side player
     * @param amount positive amount to remove
     * @return {@code true} if funds were sufficient and removed
     */
    public static boolean removeMoney(ServerPlayer player, int amount) {
        if (amount <= 0) return false;

        MinecraftServer server = player.level().getServer();
        PlayerVaultState vault = PlayerVaultState.getServerState(server);

        boolean success = vault.removeBalance(player.getUUID(), amount);
        if (success) {
            syncToClient(player, vault.getBalance(player.getUUID()));
            PocketUICore.LOGGER.debug("[Economy] -{} → {} (total: {})",
                    amount, player.getName().getString(),
                    vault.getBalance(player.getUUID()));
        }
        return success;
    }

    /**
     * Set a player's balance to an exact value, persist, and sync.
     *
     * @param player  the server-side player
     * @param balance the new balance (clamped to >= 0)
     */
    public static void setBalance(ServerPlayer player, int balance) {
        MinecraftServer server = player.level().getServer();
        PlayerVaultState vault = PlayerVaultState.getServerState(server);

        vault.setBalance(player.getUUID(), Math.max(0, balance));
        syncToClient(player, vault.getBalance(player.getUUID()));
    }

    // =====================================================================
    //  Sync helper
    // =====================================================================

    /**
     * Send the current balance to the player's client via S2C payload.
     *
     * @param player  the target player
     * @param balance the balance to send
     */
    public static void syncToClient(ServerPlayer player, int balance) {
        ServerPlayNetworking.send(player, new SyncBalancePayload(balance));
    }

    /**
     * Force-sync a player's balance (e.g. on join).
     *
     * @param player the server-side player
     */
    public static void syncOnJoin(ServerPlayer player) {
        syncToClient(player, getBalance(player));
    }
}
