package com.pocketuicore.notification;

import com.pocketuicore.component.NotificationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

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
    private static final Component PREFIX_TEXT = Component.literal(PREFIX)
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

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
    public static void sendActionBar(ServerPlayer player, Component message) {
        if (player == null) return;
        player.sendOverlayMessage(message);
    }

    /**
     * Convenience overload accepting a plain string.
     */
    public static void sendActionBar(ServerPlayer player, String message) {
        sendActionBar(player, Component.literal(message));
    }

    /**
     * Formatted action-bar message with a colour.
     */
    public static void sendActionBar(ServerPlayer player, String message, ChatFormatting color) {
        sendActionBar(player, Component.literal(message).withStyle(color));
    }

    /**
     * Action-bar message showing a numeric progress.
     * Output example: {@code "Growing… 73 %"}
     */
    public static void sendActionBarProgress(ServerPlayer player,
                                              String prefix, float progress01) {
        int pct = Math.round(Math.clamp(progress01, 0f, 1f) * 100);
        Component msg = Component.literal(prefix + " " + pct + "%")
            .withStyle(ChatFormatting.AQUA);
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
     * @param message body Component (will be appended after the prefix)
     */
    public static void sendChatReminder(ServerPlayer player, Component message) {
        if (player == null) return;
        Component full = Component.empty()
                .append(PREFIX_TEXT)
                .append(message);
        player.sendSystemMessage(full);
    }

    /**
     * Convenience overload accepting a plain string.
     */
    public static void sendChatReminder(ServerPlayer player, String message) {
        sendChatReminder(player, Component.literal(message).withStyle(ChatFormatting.YELLOW));
    }

    /**
     * Chat reminder with explicit formatting.
     */
    public static void sendChatReminder(ServerPlayer player, String message,
                                         ChatFormatting... formats) {
        sendChatReminder(player, Component.literal(message).withStyle(formats));
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
    public static void sendDurabilityAlert(ServerPlayer player,
                                            int remaining, int max, String itemName) {
        float ratio = (max > 0) ? (float) remaining / max : 0f;
        ChatFormatting colour = ratio <= 0.1f ? ChatFormatting.RED : ChatFormatting.YELLOW;
        Component body = Component.literal("⚠ " + itemName + " durability: " + remaining + "/" + max)
            .withStyle(colour);
        sendChatReminder(player, body);
    }

    /**
     * Tier upgrade notification (e.g. tool upgraded to Diamond tier).
     * Sent as both an action-bar flash and a chat reminder.
     */
        public static void sendTierUpgrade(ServerPlayer player,
                                        String itemName, String newTier) {
        Component actionBar = Component.literal("⬆ " + itemName + " → " + newTier)
                .withStyle(ChatFormatting.GREEN);
        sendActionBar(player, actionBar);

        Component chat = Component.literal(itemName + " upgraded to ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(newTier).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        sendChatReminder(player, chat);
    }

    /**
     * Generic milestone celebration.
     */
        public static void sendMilestone(ServerPlayer player, String milestone) {
        Component body = Component.literal("✦ " + milestone)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        sendChatReminder(player, body);
        sendActionBar(player, Component.literal("✦ " + milestone)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================
    //  Client-side bridge  (v1.13.0)
    // =====================================================================

    /**
     * Show a client-side UI notification via {@link NotificationManager}.
     * This is a convenience bridge so server-aware code that already uses
     * PocketNotifier can also trigger the rich on-screen notification
     * system on the client.
     * <p>
     * <b>Must be called from the client (render) thread.</b>
     *
     * @param type    notification type (colour + icon)
     * @param message the message to display
     * @since 1.13.0
     */
    public static void showClient(NotificationManager.NotificationType type,
                                   String message) {
        NotificationManager.show(type, message);
    }

    /**
     * Show a client-side notification with custom duration.
     *
     * @param type       notification type
     * @param message    the message
     * @param durationMs display duration in milliseconds
     * @since 1.13.0
     */
    public static void showClient(NotificationManager.NotificationType type,
                                   String message, long durationMs) {
        NotificationManager.show(type, message, durationMs);
    }
}
