package com.pocketuicore.economy;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PlayerVaultState — Server-side persistent economy storage.
 * <p>
 * Stores a {@code UUID → balance} map that persists across server
 * restarts.  Uses the modern 1.21.11 {@link PersistentStateType} with
 * a DFU {@link Codec} for serialisation (no manual NBT).
 * <p>
 * Retrieve with {@link #getServerState(MinecraftServer)}.
 */
public class PlayerVaultState extends PersistentState {

    // ── Codec-based serialisation ────────────────────────────────────────
    private static final Codec<PlayerVaultState> CODEC =
            Codec.unboundedMap(Uuids.STRING_CODEC, Codec.INT)
                 .xmap(PlayerVaultState::new, state -> state.balances);

    /** The PersistentStateType used by the state manager. */
    public static final PersistentStateType<PlayerVaultState> STATE_TYPE =
            new PersistentStateType<>(
                    "pocketuicore_vault",   // file id → data/pocketuicore_vault.dat
                    PlayerVaultState::new,  // Supplier<T> factory (empty state)
                    CODEC,                  // Codec<T>
                    null                    // no DataFixTypes needed
            );

    // ── Data ─────────────────────────────────────────────────────────────
    private final Map<UUID, Integer> balances;

    /** Create an empty vault (used by the Supplier in STATE_TYPE). */
    public PlayerVaultState() {
        this.balances = new HashMap<>();
    }

    /** Reconstruct from a decoded map (used by the Codec xmap). */
    private PlayerVaultState(Map<UUID, Integer> decoded) {
        this.balances = new HashMap<>(decoded);
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Get the balance for a player.  Defaults to {@code 0} if the player
     * has never been recorded.
     *
     * @param uuid player UUID
     * @return current balance
     */
    public int getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0);
    }

    /**
     * Set the balance for a player and mark the state dirty.
     *
     * @param uuid    player UUID
     * @param balance new balance
     */
    public void setBalance(UUID uuid, int balance) {
        balances.put(uuid, balance);
        markDirty();
    }

    /**
     * Add to a player's balance (clamped at {@link Integer#MAX_VALUE}).
     *
     * @param uuid   player UUID
     * @param amount amount to add (must be positive)
     */
    public void addBalance(UUID uuid, int amount) {
        int current = getBalance(uuid);
        setBalance(uuid, (int) Math.min((long) current + amount, Integer.MAX_VALUE));
    }

    /**
     * Remove from a player's balance (clamped at 0).
     *
     * @param uuid   player UUID
     * @param amount amount to remove (must be positive)
     * @return {@code true} if the player had enough funds
     */
    public boolean removeBalance(UUID uuid, int amount) {
        int current = getBalance(uuid);
        if (current < amount) return false;
        setBalance(uuid, current - amount);
        return true;
    }

    // ── Server state helper ──────────────────────────────────────────────

    /**
     * Retrieve (or create) the vault state from the server's overworld
     * persistent state manager.
     *
     * @param server the running MinecraftServer
     * @return the singleton PlayerVaultState
     */
    public static PlayerVaultState getServerState(MinecraftServer server) {
        return server.getOverworld()
                     .getPersistentStateManager()
                     .getOrCreate(STATE_TYPE);
    }
}
