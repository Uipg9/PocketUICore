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
 * EstateGrowthState — Persistent storage for estate growth percentages.
 * <p>
 * Stores a {@code UUID → Float} map so that partially-completed estate
 * growth is preserved across player disconnects and server restarts.
 * Uses the modern 1.21.11 {@link PersistentStateType} with a DFU
 * {@link Codec} for serialisation (no manual NBT).
 *
 * @see EstateManager
 */
public class EstateGrowthState extends PersistentState {

    // ── Codec-based serialisation ────────────────────────────────────────

    private static final Codec<EstateGrowthState> CODEC =
            Codec.unboundedMap(Uuids.STRING_CODEC, Codec.FLOAT)
                 .xmap(EstateGrowthState::new, state -> state.growthMap);

    /** The PersistentStateType used by the state manager. */
    public static final PersistentStateType<EstateGrowthState> STATE_TYPE =
            new PersistentStateType<>(
                    "pocketuicore_estate_growth",   // → data/pocketuicore_estate_growth.dat
                    EstateGrowthState::new,         // Supplier<T>
                    CODEC,                          // Codec<T>
                    null                            // no DataFixTypes
            );

    // ── Data ─────────────────────────────────────────────────────────────

    private final Map<UUID, Float> growthMap;

    /** Create an empty state (factory for STATE_TYPE). */
    public EstateGrowthState() {
        this.growthMap = new HashMap<>();
    }

    /** Reconstruct from a decoded map (Codec xmap). */
    private EstateGrowthState(Map<UUID, Float> decoded) {
        this.growthMap = new HashMap<>(decoded);
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Get the stored growth percentage for a player.
     *
     * @param uuid player UUID
     * @return growth percentage (0.0f–100.0f), defaults to 0.0f
     */
    public float getGrowth(UUID uuid) {
        return growthMap.getOrDefault(uuid, 0.0f);
    }

    /**
     * Set the growth percentage for a player and mark dirty.
     *
     * @param uuid   player UUID
     * @param growth new growth value (0.0f–100.0f)
     */
    public void setGrowth(UUID uuid, float growth) {
        growthMap.put(uuid, growth);
        markDirty();
    }

    /**
     * Remove a player's growth entry (e.g. after full payout reset).
     *
     * @param uuid player UUID
     */
    public void removeGrowth(UUID uuid) {
        growthMap.remove(uuid);
        markDirty();
    }

    // ── Server state helper ──────────────────────────────────────────────

    /**
     * Retrieve (or create) the estate growth state from the server's
     * overworld persistent state manager.
     *
     * @param server the running MinecraftServer
     * @return the singleton EstateGrowthState
     */
    public static EstateGrowthState getServerState(MinecraftServer server) {
        return server.getOverworld()
                     .getPersistentStateManager()
                     .getOrCreate(STATE_TYPE);
    }
}
