package com.pocketuicore.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * UI Sound Manager — Centralised sound playback for PocketUICore screens.
 * <p>
 * Removes the boilerplate of null-checking the player and correctly
 * distinguishing between {@code RegistryEntry<SoundEvent>} (needs
 * {@code .value()}) and plain {@code SoundEvent} instances.
 * <p>
 * Provides preset methods for common UI feedback sounds and a
 * {@link #playCustom} method for arbitrary sound events.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     UISoundManager.playClick();                // default click
 *     UISoundManager.playClick(1.4f, 0.5f);     // custom pitch + volume
 *     UISoundManager.playSuccess();              // bright ding
 *     UISoundManager.playError();                // low warning
 * }</pre>
 */
public final class UISoundManager {

    private UISoundManager() { /* static utility */ }

    // ── Master volume control (v1.12.0) ─────────────────────────────────
    private static float masterVolume = 1.0f;
    private static boolean muted = false;

    /**
     * Set the master volume for all PocketUICore UI sounds.
     *
     * @param volume 0.0 (silent) to 1.0 (full)
     * @since 1.12.0
     */
    public static void setMasterVolume(float volume) {
        masterVolume = Math.clamp(volume, 0f, 1f);
    }

    /** @return the current master volume (0.0–1.0). @since 1.12.0 */
    public static float getMasterVolume() { return masterVolume; }

    /**
     * Mute or unmute all PocketUICore UI sounds.
     *
     * @param muted {@code true} to mute
     * @since 1.12.0
     */
    public static void setMuted(boolean muted) {
        UISoundManager.muted = muted;
    }

    /** @return {@code true} if muted. @since 1.12.0 */
    public static boolean isMuted() { return muted; }

    // =====================================================================
    //  Core playback
    // =====================================================================

    /**
     * Play a {@link SoundEvent} at the given pitch and volume.
     * Handles null-safety on the client player and uses the correct
     * UI sound instance factory.
     * <p>
     * Volume is scaled by the master volume and respects the mute flag.
     *
     * @param event  the sound event to play
     * @param pitch  pitch multiplier (1.0 = normal)
     * @param volume volume multiplier (1.0 = full)
     */
    public static void playCustom(SoundEvent event, float pitch, float volume) {
        if (muted || masterVolume <= 0f) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume * masterVolume));
    }

    /**
     * Play a {@link RegistryEntry}{@code <SoundEvent>} at the given pitch
     * and volume.  Unwraps the entry via {@code .value()} automatically,
     * so callers can pass vanilla sounds like
     * {@code SoundEvents.UI_BUTTON_CLICK} directly.
     *
     * @param entry  the registry entry wrapping the sound event
     * @param pitch  pitch multiplier (1.0 = normal)
     * @param volume volume multiplier (1.0 = full)
     */
    public static void playCustom(RegistryEntry<SoundEvent> entry,
                                   float pitch, float volume) {
        playCustom(entry.value(), pitch, volume);
    }

    // =====================================================================
    //  Preset sounds — UI Button Click (RegistryEntry → .value())
    // =====================================================================

    /**
     * Standard UI button click (default pitch 1.0, volume 0.4).
     */
    public static void playClick() {
        playClick(1.0f, 0.4f);
    }

    /**
     * UI button click with custom pitch and volume.
     */
    public static void playClick(float pitch, float volume) {
        playCustom(SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume);
    }

    // =====================================================================
    //  Preset sounds — Semantic feedback
    // =====================================================================

    /**
     * Bright "success" ding (high-pitched click).
     * Suitable for confirmations, completed actions, rewards.
     */
    public static void playSuccess() {
        playClick(1.5f, 0.6f);
    }

    /**
     * Deep "error" thud (low-pitched click).
     * Suitable for invalid actions, insufficient resources.
     */
    public static void playError() {
        playClick(0.3f, 0.5f);
    }

    /**
     * Soft selection tick (quiet, subtle).
     * Suitable for focus changes, hover acknowledgements.
     */
    public static void playSelect() {
        playClick(1.0f, 0.15f);
    }

    /**
     * Double-note "plant" / "create" sound.
     */
    public static void playCreate() {
        playClick(1.4f, 0.5f);
        playClick(1.8f, 0.3f);
    }

    /**
     * Three-note ascending cascade.
     * Suitable for harvests, celebrations, level-ups.
     */
    public static void playCelebration() {
        playClick(1.5f, 0.6f);
        playClick(1.8f, 0.4f);
        playClick(2.0f, 0.3f);
    }

    /**
     * Low gong / notification.
     * Suitable for season changes, important alerts.
     */
    public static void playGong() {
        playClick(0.4f, 0.6f);
        playClick(0.6f, 0.4f);
    }

    /**
     * Bright ascending ding.
     * Suitable for "ready" notifications, item completion.
     */
    public static void playReady() {
        playClick(1.6f, 0.4f);
        playClick(2.0f, 0.2f);
    }

    /**
     * Low boundary thud — for navigation edges.
     */
    public static void playBoundary() {
        playClick(0.3f, 0.2f);
    }

    // =====================================================================
    //  v1.9 — Additional semantic presets
    // =====================================================================

    /**
     * Harvest / collect sound — satisfying ascending double-note.
     * Suitable for crop harvest, resource collection.
     */
    public static void playHarvest() {
        playClick(1.3f, 0.5f);
        playClick(1.7f, 0.3f);
    }

    /**
     * Plant / place sound — soft descending note.
     * Suitable for planting crops, placing items.
     */
    public static void playPlant() {
        playClick(1.2f, 0.35f);
        playClick(0.9f, 0.2f);
    }

    /**
     * Upgrade / level-up sound — bright ascending triple-note fanfare.
     * Suitable for upgrades, prestige, tier-ups.
     */
    public static void playUpgrade() {
        playClick(1.4f, 0.5f);
        playClick(1.8f, 0.4f);
        playClick(2.0f, 0.5f);
    }

    /**
     * Warning sound — low double-thud.
     * Suitable for low resources, expiring timers, danger.
     */
    public static void playWarning() {
        playClick(0.5f, 0.5f);
        playClick(0.4f, 0.3f);
    }

    /**
     * Transition / whoosh sound — brief mid-pitch sweep.
     * Suitable for screen transitions, tab switches, page flips.
     */
    public static void playTransition() {
        playClick(1.1f, 0.25f);
    }

    /**
     * Milestone achieved — triumphant ascending cascade with emphasis.
     * More dramatic than {@link #playCelebration()}.
     */
    public static void playMilestone() {
        playClick(1.2f, 0.5f);
        playClick(1.6f, 0.5f);
        playClick(2.0f, 0.6f);
    }

    /**
     * Notification ping — single bright note.
     * Suitable for toast notifications, alerts.
     */
    public static void playNotification() {
        playClick(1.8f, 0.4f);
    }

    // =====================================================================
    //  Scheduled playback  (v1.13.0)
    // =====================================================================

    private record ScheduledSound(SoundEvent event, float pitch, float volume,
                                   long playAtMs) {}

    private static final List<ScheduledSound> scheduled = new ArrayList<>();

    /**
     * Schedule a sound to play after a delay.  Useful for multi-note
     * sequences that should be spaced out over time (e.g. musical chords).
     * <p>
     * Call {@link #tickScheduled()} from your client tick handler to
     * service the queue.
     *
     * @param event   the sound event
     * @param pitch   pitch multiplier
     * @param volume  volume multiplier
     * @param delayMs delay before playback (milliseconds)
     * @since 1.13.0
     */
    public static void schedulePlay(SoundEvent event, float pitch,
                                     float volume, long delayMs) {
        scheduled.add(new ScheduledSound(event, pitch, volume,
                System.currentTimeMillis() + delayMs));
    }

    /** Overload accepting a {@link RegistryEntry}. @since 1.13.0 */
    public static void schedulePlay(RegistryEntry<SoundEvent> entry,
                                     float pitch, float volume, long delayMs) {
        schedulePlay(entry.value(), pitch, volume, delayMs);
    }

    /**
     * Tick the scheduled-sound queue — plays any sounds whose delay has
     * elapsed.  Should be called once per client tick
     * (e.g. from {@code ClientTickEvents.END_CLIENT_TICK}).
     *
     * @since 1.13.0
     */
    public static void tickScheduled() {
        if (scheduled.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<ScheduledSound> it = scheduled.iterator();
        while (it.hasNext()) {
            ScheduledSound s = it.next();
            if (now >= s.playAtMs()) {
                playCustom(s.event(), s.pitch(), s.volume());
                it.remove();
            }
        }
    }
}
