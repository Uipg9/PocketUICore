package com.pocketuicore.economy;

import com.pocketuicore.PocketUICore;
import com.pocketuicore.data.ObservableState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * ClientNetworkHandler — Client-side receiver for economy payloads.
 * <p>
 * Registers a global receiver for {@link SyncBalancePayload}.
 * When a balance update arrives from the server, the global
 * {@link #CLIENT_BALANCE} observable is updated so that any bound
 * PocketUICore menu (e.g. {@code PocketMenuScreen}) refreshes
 * automatically.
 * <p>
 * <b>Usage in UI code:</b>
 * <pre>{@code
 *     ObservableState<String> label =
 *         ClientNetworkHandler.CLIENT_BALANCE
 *             .map(b -> "Balance: $" + b);
 *     ObservableState.bindText(label, myTextLabel);
 * }</pre>
 */
public final class ClientNetworkHandler {

    private ClientNetworkHandler() { /* utility class */ }

    // ── Global observable that UI components can bind to ─────────────────
    /**
     * The player's current balance as received from the server.
     * Starts at {@code 0} and is updated each time a
     * {@link SyncBalancePayload} arrives.
     */
    public static final ObservableState<Integer> CLIENT_BALANCE =
            new ObservableState<>(0);

    // ── Registration ─────────────────────────────────────────────────────

    /**
     * Register the S2C payload receiver.  Must be called once from the
     * client entrypoint ({@link com.pocketuicore.PocketUICoreClient}).
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                SyncBalancePayload.ID,
                (payload, context) -> {
                    int newBalance = payload.balance();
                    // Schedule UI update on the render thread
                    context.client().execute(() -> {
                        CLIENT_BALANCE.set(newBalance);
                        PocketUICore.LOGGER.debug(
                                "[Economy] Client balance synced: ${}",
                                newBalance);
                    });
                }
        );
    }
}
