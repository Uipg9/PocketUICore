package com.pocketuicore.economy;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * SyncBalancePayload — S2C network payload for balance synchronisation.
 * <p>
 * Sent from the server to a specific client whenever that player's
 * balance changes.  The client receiver updates the global
 * {@link com.pocketuicore.data.ObservableState} so all bound UI
 * components refresh automatically.
 * <p>
 * Uses the modern 1.21.11 {@link CustomPayload} + {@link PacketCodec}
 * pattern (no raw PacketByteBuf registration).
 *
 * @param balance the player's current balance after the change
 */
public record SyncBalancePayload(int balance) implements CustomPayload {

    // ── Payload ID ───────────────────────────────────────────────────────
    public static final CustomPayload.Id<SyncBalancePayload> ID =
            CustomPayload.id("pocketuicore:sync_balance");

    // ── Packet Codec ─────────────────────────────────────────────────────
    public static final PacketCodec<io.netty.buffer.ByteBuf, SyncBalancePayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, SyncBalancePayload::balance,
                    SyncBalancePayload::new
            );

    // ── CustomPayload implementation ─────────────────────────────────────
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
