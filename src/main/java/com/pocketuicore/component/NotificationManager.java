package com.pocketuicore.component;

import com.pocketuicore.render.ProceduralRenderer;
import com.pocketuicore.sound.UISoundManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Notification Manager — Typed notification system with queue,
 * positioning, and optional sound integration.
 * <p>
 * Extends the concept of {@link FloatingText} with predefined
 * notification types (SUCCESS, ERROR, WARNING, INFO, MILESTONE)
 * each with distinct colours and optional sounds.
 * <p>
 * Supports a maximum visible count and queues overflow notifications
 * to show when space becomes available.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // Simple typed notification:
 *     NotificationManager.show(NotificationType.SUCCESS, "Harvest complete!");
 *
 *     // With custom duration:
 *     NotificationManager.show(NotificationType.ERROR, "Not enough gold!", 3000);
 *
 *     // With sound:
 *     NotificationManager.show(NotificationType.MILESTONE, "Prestige Level 5!", true);
 *
 *     // In your screen's render():
 *     NotificationManager.renderAll(ctx, screenWidth, screenHeight, delta);
 * }</pre>
 */
public final class NotificationManager {

    private NotificationManager() {}

    // =====================================================================
    //  Notification types
    // =====================================================================

    /** Predefined notification types with distinct colours and icons. */
    public enum NotificationType {
        /** Green — positive outcome. */
        SUCCESS(ProceduralRenderer.COL_SUCCESS, "\u2714 "),   // ✔
        /** Red — error / failure. */
        ERROR(ProceduralRenderer.COL_ERROR, "\u2716 "),       // ✖
        /** Yellow — warning / caution. */
        WARNING(ProceduralRenderer.COL_WARNING, "\u26A0 "),   // ⚠
        /** Teal — informational. */
        INFO(ProceduralRenderer.COL_ACCENT_TEAL, "\u2139 "),  // ℹ
        /** Gold — milestone / achievement. */
        MILESTONE(0xFFFFD700, "\u2605 ");                     // ★

        public final int color;
        public final String prefix;

        NotificationType(int color, String prefix) {
            this.color  = color;
            this.prefix = prefix;
        }
    }

    /** Screen position for notifications. */
    public enum Position {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    // ── State ────────────────────────────────────────────────────────────
    private static final List<Notification> active = java.util.Collections.synchronizedList(new ArrayList<>());
    private static final Deque<Notification> queue = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static int maxVisible = 5;
    private static Position position = Position.TOP_CENTER;

    // ── Notification data ────────────────────────────────────────────────
    private static final class Notification {
        final String message;
        final NotificationType type;
        final long durationMs;
        final long spawnTimeMs;
        final boolean playSound;
        boolean soundPlayed;

        Notification(String message, NotificationType type,
                     long durationMs, boolean playSound) {
            this.message    = message;
            this.type       = type;
            this.durationMs = durationMs;
            this.playSound  = playSound;
            this.spawnTimeMs = System.currentTimeMillis();
        }
    }

    // ── Persistent notification data (v1.13.0) ──────────────────────────
    private static final List<PersistentNotification> persistent =
            java.util.Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger nextPersistentId = new AtomicInteger(1);

    /**
     * A persistent notification that stays on screen until explicitly
     * dismissed via {@link #dismissPersistent(int)}.
     *
     * @since 1.13.0
     */
    public static final class PersistentNotification {
        public final int id;
        public final String message;
        public final NotificationType type;
        public final long spawnTimeMs;
        private final Runnable onDismiss;

        PersistentNotification(int id, String message, NotificationType type,
                               Runnable onDismiss) {
            this.id          = id;
            this.message     = message;
            this.type        = type;
            this.spawnTimeMs = System.currentTimeMillis();
            this.onDismiss   = onDismiss;
        }
    }

    // =====================================================================
    //  Configuration
    // =====================================================================

    /**
     * Set the maximum number of simultaneously visible notifications.
     *
     * @param max maximum visible (1–20)
     */
    public static void setMaxVisible(int max) {
        maxVisible = Math.clamp(max, 1, 20);
    }

    /**
     * Set the screen position for notifications.
     */
    public static void setPosition(Position pos) {
        position = pos;
    }

    // =====================================================================
    //  Show API
    // =====================================================================

    /**
     * Show a typed notification with default duration (2500 ms) and no sound.
     */
    public static void show(NotificationType type, String message) {
        show(type, message, 2500, false);
    }

    /**
     * Show a typed notification with custom duration, no sound.
     */
    public static void show(NotificationType type, String message, long durationMs) {
        show(type, message, durationMs, false);
    }

    /**
     * Show a typed notification with sound option.
     */
    public static void show(NotificationType type, String message, boolean playSound) {
        show(type, message, 2500, playSound);
    }

    /**
     * Show a typed notification with full options.
     *
     * @param type       notification type
     * @param message    the message text
     * @param durationMs display duration in milliseconds
     * @param playSound  whether to play a type-appropriate sound
     */
    public static void show(NotificationType type, String message,
                             long durationMs, boolean playSound) {
        Notification n = new Notification(message, type, durationMs, playSound);
        if (active.size() < maxVisible) {
            active.add(n);
        } else {
            queue.add(n);
        }
    }

    /**
     * Clear all notifications (active and queued).
     */
    public static void clearAll() {
        active.clear();
        queue.clear();
        persistent.clear();
    }

    // =====================================================================
    //  Persistent notification API  (v1.13.0)
    // =====================================================================

    /**
     * Show a persistent notification that stays on screen until dismissed.
     *
     * @param type    notification type
     * @param message the message text
     * @return the notification ID — pass to {@link #dismissPersistent} to remove
     * @since 1.13.0
     */
    public static int showPersistent(NotificationType type, String message) {
        return showPersistent(type, message, null);
    }

    /**
     * Show a persistent notification with a dismiss callback.
     *
     * @param type      notification type
     * @param message   the message text
     * @param onDismiss callback when dismissed (may be {@code null})
     * @return the notification ID
     * @since 1.13.0
     */
    public static int showPersistent(NotificationType type, String message,
                                      Runnable onDismiss) {
        int id = nextPersistentId.getAndIncrement();
        persistent.add(new PersistentNotification(id, message, type, onDismiss));
        return id;
    }

    /**
     * Dismiss a persistent notification by ID.
     *
     * @param id the ID returned by {@link #showPersistent}
     * @return {@code true} if a notification was found and removed
     * @since 1.13.0
     */
    public static boolean dismissPersistent(int id) {
        Iterator<PersistentNotification> it = persistent.iterator();
        while (it.hasNext()) {
            PersistentNotification pn = it.next();
            if (pn.id == id) {
                it.remove();
                if (pn.onDismiss != null) pn.onDismiss.run();
                return true;
            }
        }
        return false;
    }

    /** @return unmodifiable view of active persistent notifications. @since 1.13.0 */
    public static List<PersistentNotification> getPersistent() {
        return java.util.Collections.unmodifiableList(persistent);
    }

    // =====================================================================
    //  Rendering
    // =====================================================================

    /**
     * Render all active notifications. Call at the end of your render method.
     */
    public static void renderAll(DrawContext ctx, int screenW, int screenH, float delta) {
        if (active.isEmpty() && queue.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        long now = System.currentTimeMillis();

        // Remove expired and promote from queue
        Iterator<Notification> it = active.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (now - n.spawnTimeMs >= n.durationMs) {
                it.remove();
            }
        }
        while (active.size() < maxVisible && !queue.isEmpty()) {
            active.add(queue.poll());
        }

        // Render
        int pad = 6;
        int spacing = 4;
        int idx = 0;
        for (Notification n : active) {
            long elapsed = now - n.spawnTimeMs;

            // Play sound on first render
            if (n.playSound && !n.soundPlayed) {
                n.soundPlayed = true;
                playTypeSound(n.type);
            }

            // Fade in/out
            float alpha;
            long fadeInMs  = 200;
            long fadeOutMs = 400;
            if (elapsed < fadeInMs) {
                alpha = (float) elapsed / fadeInMs;
            } else if (elapsed > n.durationMs - fadeOutMs) {
                alpha = (float) (n.durationMs - elapsed) / fadeOutMs;
            } else {
                alpha = 1.0f;
            }
            alpha = Math.clamp(alpha, 0f, 1f);

            String fullText = n.type.prefix + n.message;
            int textW = tr.getWidth(fullText);
            int boxW  = textW + pad * 2;
            int boxH  = tr.fontHeight + pad * 2;

            // Position
            int bx, by;
            switch (position) {
                case TOP_LEFT      -> { bx = 10;                              by = 10; }
                case TOP_CENTER    -> { bx = (screenW - boxW) / 2;           by = 10; }
                case TOP_RIGHT     -> { bx = screenW - boxW - 10;            by = 10; }
                case BOTTOM_LEFT   -> { bx = 10;                              by = screenH - boxH - 10; }
                case BOTTOM_CENTER -> { bx = (screenW - boxW) / 2;           by = screenH - boxH - 10; }
                case BOTTOM_RIGHT  -> { bx = screenW - boxW - 10;            by = screenH - boxH - 10; }
                default            -> { bx = (screenW - boxW) / 2;           by = 10; }
            }

            // Stack vertically
            boolean topAnchored = position == Position.TOP_LEFT
                    || position == Position.TOP_CENTER
                    || position == Position.TOP_RIGHT;
            if (topAnchored) {
                by += idx * (boxH + spacing);
            } else {
                by -= idx * (boxH + spacing);
            }

            // Draw
            int bgAlpha = (int) (alpha * 220);
            int bgColor = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_BG_ELEVATED, bgAlpha);
            int borderAlpha = (int) (alpha * 180);
            int border  = ProceduralRenderer.withAlpha(n.type.color, borderAlpha);
            int txtCol  = ProceduralRenderer.withAlpha(n.type.color, (int) (alpha * 255));

            ProceduralRenderer.fillRoundedRect(ctx, bx, by, boxW, boxH, 4, bgColor);
            ProceduralRenderer.drawRoundedBorder(ctx, bx, by, boxW, boxH, 4, border);

            // Accent stripe on left
            int stripeColor = ProceduralRenderer.withAlpha(n.type.color, (int) (alpha * 200));
            ctx.fill(bx + 1, by + 3, bx + 3, by + boxH - 3, stripeColor);

            ctx.drawTextWithShadow(tr, Text.literal(fullText),
                    bx + pad, by + pad, txtCol);

            idx++;
        }

        // ── Persistent notifications (below/above timed ones) ────────────
        for (PersistentNotification pn : persistent) {
            long elapsed = now - pn.spawnTimeMs;
            float alpha = Math.min(1f, elapsed / 200f); // 200 ms fade-in

            String fullText = pn.type.prefix + pn.message;
            int textW = tr.getWidth(fullText);
            int boxW  = textW + pad * 2 + 12; // extra for "×" dismiss hint
            int boxH  = tr.fontHeight + pad * 2;

            int bx, by;
            switch (position) {
                case TOP_LEFT      -> { bx = 10;                    by = 10; }
                case TOP_CENTER    -> { bx = (screenW - boxW) / 2; by = 10; }
                case TOP_RIGHT     -> { bx = screenW - boxW - 10;  by = 10; }
                case BOTTOM_LEFT   -> { bx = 10;                    by = screenH - boxH - 10; }
                case BOTTOM_CENTER -> { bx = (screenW - boxW) / 2; by = screenH - boxH - 10; }
                case BOTTOM_RIGHT  -> { bx = screenW - boxW - 10;  by = screenH - boxH - 10; }
                default            -> { bx = (screenW - boxW) / 2; by = 10; }
            }

            boolean topAnchored = position == Position.TOP_LEFT
                    || position == Position.TOP_CENTER
                    || position == Position.TOP_RIGHT;
            if (topAnchored) {
                by += idx * (boxH + spacing);
            } else {
                by -= idx * (boxH + spacing);
            }

            int bgAlpha = (int) (alpha * 220);
            int bgColor = ProceduralRenderer.withAlpha(ProceduralRenderer.COL_BG_ELEVATED, bgAlpha);
            int borderAlpha = (int) (alpha * 220);
            int border  = ProceduralRenderer.withAlpha(pn.type.color, borderAlpha);
            int txtCol  = ProceduralRenderer.withAlpha(pn.type.color, (int) (alpha * 255));

            ProceduralRenderer.fillRoundedRect(ctx, bx, by, boxW, boxH, 4, bgColor);
            ProceduralRenderer.drawRoundedBorder(ctx, bx, by, boxW, boxH, 4, border);

            // Accent stripe + pulsing left edge
            int stripeAlpha = (int) (alpha * (160 + 40 * Math.sin(elapsed / 400.0)));
            int stripeColor = ProceduralRenderer.withAlpha(pn.type.color, Math.clamp(stripeAlpha, 0, 255));
            ctx.fill(bx + 1, by + 3, bx + 3, by + boxH - 3, stripeColor);

            ctx.drawTextWithShadow(tr, Text.literal(fullText),
                    bx + pad, by + pad, txtCol);

            idx++;
        }
    }

    private static void playTypeSound(NotificationType type) {
        switch (type) {
            case SUCCESS   -> UISoundManager.playSuccess();
            case ERROR     -> UISoundManager.playError();
            case WARNING   -> UISoundManager.playWarning();
            case MILESTONE -> UISoundManager.playMilestone();
            case INFO      -> UISoundManager.playNotification();
        }
    }

    // =====================================================================
    //  Queries
    // =====================================================================

    /** @return number of active (visible) notifications. */
    public static int getActiveCount() { return active.size(); }

    /** @return number of queued notifications waiting to display. */
    public static int getQueuedCount() { return queue.size(); }

    // =====================================================================
    //  FloatingText-compatible toast API  (v1.13.0)
    // =====================================================================

    /**
     * Show a lightweight toast at any {@link FloatingText.Anchor} position.
     * This replaces the old {@link FloatingText#show} API; new code should
     * use this method instead.
     *
     * @param message    the text to display
     * @param anchor     screen anchor/position
     * @param durationMs how long the toast stays visible (ms)
     * @since 1.13.0
     */
    public static void showToast(String message, FloatingText.Anchor anchor, long durationMs) {
        FloatingText.show(message, anchor, ProceduralRenderer.COL_TEXT_PRIMARY, durationMs);
    }

    /**
     * Show a coloured toast at any {@link FloatingText.Anchor} position.
     *
     * @param message    the text to display
     * @param anchor     screen anchor/position
     * @param color      ARGB text colour
     * @param durationMs how long the toast stays visible (ms)
     * @since 1.13.0
     */
    public static void showToast(String message, FloatingText.Anchor anchor,
                                  int color, long durationMs) {
        FloatingText.show(message, anchor, color, durationMs);
    }
}
