package com.pocketuicore.notification;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Module 3 — Notification Manager
 * <p>
 * Thin, server-side utility for sending formatted messages to players.
 * Two channels are provided:
 * <ul>
 *   <li><b>Action-bar</b> — for rapid, transient updates (tool tier, grow
 *       progress, cooldowns) that should not pollute chat.</li>
 *   <li><b>Chat reminder</b> — for durable alerts (low-durability warnings,
 *       milestone notifications) that persist in chat history.</li>
 * </ul>
 * <p>
 * All methods are static, thread-safe (they call player methods that
 * internally dispatch to the network thread), and null-safe on the
 * player argument.
 */
public final class PocketNotifier {

    private PocketNotifier() { /* static utility */ }

    // ── Branding ─────────────────────────────────────────────────────────
    private static final String PREFIX  = "[Pocket] ";
    /** Gold-coloured prefix prepended to chat reminders. */
    private static final Text PREFIX_TEXT = Text.literal(PREFIX)
            .formatted(Formatting.GOLD, Formatting.BOLD);

    // =====================================================================
    //  Action-bar (overlay) messages
    // =====================================================================

    /**
     * Display a message in the action-bar slot (above the hotbar).
     * Ideal for ephemeral info that refreshes frequently (e.g. every tick
     * or every few ticks).
     *
     * @param player  target player (no-op if null)
     * @param message the message to display
     */
    public static void sendActionBar(ServerPlayerEntity player, Text message) {
        if (player == null) return;
        player.sendMessage(message, true); // overlay = true → action bar
    }

    /**
     * Convenience overload accepting a plain string.
     */
    public static void sendActionBar(ServerPlayerEntity player, String message) {
        sendActionBar(player, Text.literal(message));
    }

    /**
     * Formatted action-bar message with a colour.
     */
    public static void sendActionBar(ServerPlayerEntity player, String message, Formatting color) {
        sendActionBar(player, Text.literal(message).formatted(color));
    }

    /**
     * Action-bar message showing a numeric progress.
     * Output example: {@code "Growing… 73 %"}
     */
    public static void sendActionBarProgress(ServerPlayerEntity player,
                                              String prefix, float progress01) {
        int pct = Math.round(Math.clamp(progress01, 0f, 1f) * 100);
        Text msg = Text.literal(prefix + " " + pct + "%")
                .formatted(Formatting.AQUA);
        sendActionBar(player, msg);
    }

    // =====================================================================
    //  Chat reminders
    // =====================================================================

    /**
     * Send a branded chat message that stays in the player's chat history.
     * Automatically prepends the golden {@code [Pocket]} prefix.
     *
     * @param player  target player (no-op if null)
     * @param message body text (will be appended after the prefix)
     */
    public static void sendChatReminder(ServerPlayerEntity player, Text message) {
        if (player == null) return;
        Text full = Text.empty()
                .append(PREFIX_TEXT)
                .append(message);
        player.sendMessage(full, false); // overlay = false → normal chat
    }

    /**
     * Convenience overload accepting a plain string.
     */
    public static void sendChatReminder(ServerPlayerEntity player, String message) {
        sendChatReminder(player, Text.literal(message).formatted(Formatting.YELLOW));
    }

    /**
     * Chat reminder with explicit formatting.
     */
    public static void sendChatReminder(ServerPlayerEntity player, String message,
                                         Formatting... formats) {
        sendChatReminder(player, Text.literal(message).formatted(formats));
    }

    // =====================================================================
    //  Specialised helpers
    // =====================================================================

    /**
     * Durability warning — uses a red/yellow colour depending on severity.
     *
     * @param remaining durability points left
     * @param max       maximum durability
     * @param itemName  display name of the item
     */
    public static void sendDurabilityAlert(ServerPlayerEntity player,
                                            int remaining, int max, String itemName) {
        float ratio = (max > 0) ? (float) remaining / max : 0f;
        Formatting colour = ratio <= 0.1f ? Formatting.RED : Formatting.YELLOW;
        Text body = Text.literal("⚠ " + itemName + " durability: " + remaining + "/" + max)
                .formatted(colour);
        sendChatReminder(player, body);
    }

    /**
     * Tier upgrade notification (e.g. tool upgraded to Diamond tier).
     * Sent as both an action-bar flash and a chat reminder.
     */
    public static void sendTierUpgrade(ServerPlayerEntity player,
                                        String itemName, String newTier) {
        Text actionBar = Text.literal("⬆ " + itemName + " → " + newTier)
                .formatted(Formatting.GREEN);
        sendActionBar(player, actionBar);

        Text chat = Text.literal(itemName + " upgraded to ")
                .formatted(Formatting.WHITE)
                .append(Text.literal(newTier).formatted(Formatting.GREEN, Formatting.BOLD));
        sendChatReminder(player, chat);
    }

    /**
     * Generic milestone celebration.
     */
    public static void sendMilestone(ServerPlayerEntity player, String milestone) {
        Text body = Text.literal("✦ " + milestone)
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);
        sendChatReminder(player, body);
        sendActionBar(player, Text.literal("✦ " + milestone)
                .formatted(Formatting.LIGHT_PURPLE));
    }
}
