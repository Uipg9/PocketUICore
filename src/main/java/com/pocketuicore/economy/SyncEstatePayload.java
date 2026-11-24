package com.pocketuicore.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * SyncEstatePayload — S2C network payload for estate growth sync.
 * <p>
 * Sent from the server to a specific client at a throttled rate
 * (every {@code SYNC_INTERVAL} ticks in {@link EstateManager}) so the
 * client can display a smoothly-filling progress bar in the
 * {@code /pocket} menu without desyncing.
 * <p>
 * Uses {@link CustomPacketPayload.Type} + {@link StreamCodec}.
 *
 * @param percentage the player's current estate growth (0.0-100.0)
 */
public record SyncEstatePayload(float percentage) implements CustomPacketPayload {

    // ── Payload type ─────────────────────────────────────────────────────
    public static final CustomPacketPayload.Type<SyncEstatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("pocketuicore", "sync_estate"));

    // ── Stream codec ─────────────────────────────────────────────────────
    public static final StreamCodec<ByteBuf, SyncEstatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, SyncEstatePayload::percentage,
                    SyncEstatePayload::new
            );

    // ── CustomPacketPayload implementation ───────────────────────────────
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
