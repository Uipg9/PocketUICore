package com.pocketuicore.economy;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * SyncEstatePayload — S2C network payload for estate growth sync.
 * <p>
 * Sent from the server to a specific client at a throttled rate
 * (every {@code SYNC_INTERVAL} ticks in {@link EstateManager}) so the
 * client can display a smoothly-filling progress bar in the
 * {@code /pocket} menu without desyncing.
 * <p>
 * Uses the modern 1.21.11 {@link CustomPayload} + {@link PacketCodec}
 * pattern (no raw PacketByteBuf registration).
 *
 * @param percentage the player's current estate growth (0.0–100.0)
 */
public record SyncEstatePayload(float percentage) implements CustomPayload {

    // ── Payload ID ───────────────────────────────────────────────────────
    public static final CustomPayload.Id<SyncEstatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("pocketuicore", "sync_estate"));

    // ── Packet Codec ─────────────────────────────────────────────────────
    public static final PacketCodec<io.netty.buffer.ByteBuf, SyncEstatePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, SyncEstatePayload::percentage,
                    SyncEstatePayload::new
            );

    // ── CustomPayload implementation ─────────────────────────────────────
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
