package com.pocketuicore.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * SyncBalancePayload — S2C network payload for balance synchronisation.
 * <p>
 * Sent from the server to a specific client whenever that player's
 * balance changes.  The client receiver updates the global
 * {@link com.pocketuicore.data.ObservableState} so all bound UI
 * components refresh automatically.
 * <p>
 * Uses {@link CustomPacketPayload.Type} + {@link StreamCodec}.
 *
 * @param balance the player's current balance after the change
 */
public record SyncBalancePayload(int balance) implements CustomPacketPayload {

    // ── Payload type ─────────────────────────────────────────────────────
    public static final CustomPacketPayload.Type<SyncBalancePayload> TYPE =
            CustomPacketPayload.createType("pocketuicore:sync_balance");

    // ── Stream codec ─────────────────────────────────────────────────────
    public static final StreamCodec<ByteBuf, SyncBalancePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SyncBalancePayload::balance,
                    SyncBalancePayload::new
            );

    // ── CustomPacketPayload implementation ───────────────────────────────
    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
