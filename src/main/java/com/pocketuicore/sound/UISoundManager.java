package com.pocketuicore.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

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

    // =====================================================================
    //  Core playback
    // =====================================================================

    /**
     * Play a {@link SoundEvent} at the given pitch and volume.
     * Handles null-safety on the client player and uses the correct
     * UI sound instance factory.
     *
     * @param event  the sound event to play
     * @param pitch  pitch multiplier (1.0 = normal)
     * @param volume volume multiplier (1.0 = full)
     */
    public static void playCustom(SoundEvent event, float pitch, float volume) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
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
}
