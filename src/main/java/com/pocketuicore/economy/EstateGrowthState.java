package com.pocketuicore.economy;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * EstateGrowthState — Persistent storage for estate growth percentages.
 * <p>
 * Stores a {@code UUID → Float} map so that partially-completed estate
 * growth is preserved across player disconnects and server restarts.
 * Uses {@link SavedDataType} with a DFU
 * {@link Codec} for serialisation (no manual NBT).
 *
 * @see EstateManager
 */
public class EstateGrowthState extends SavedData {

    // ── Codec-based serialisation ────────────────────────────────────────

    private static final Codec<EstateGrowthState> CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.FLOAT)
                 .xmap(EstateGrowthState::new, state -> state.growthMap);

        /** The SavedDataType used by the state manager. */
        public static final SavedDataType<EstateGrowthState> STATE_TYPE =
            new SavedDataType<>(
                Identifier.fromNamespaceAndPath("pocketuicore", "estate_growth"),
                EstateGrowthState::new,
                CODEC,
                null
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
        setDirty();
    }

    /**
     * Remove a player's growth entry (e.g. after full payout reset).
     *
     * @param uuid player UUID
     */
    public void removeGrowth(UUID uuid) {
        growthMap.remove(uuid);
        setDirty();
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
        return server.overworld()
                     .getDataStorage()
                     .computeIfAbsent(STATE_TYPE);
    }
}
