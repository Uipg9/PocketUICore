package com.pocketuicore.economy;

/**
 * Economy Config — Externalised tuning constants for the economy system.
 * <p>
 * All values that were previously hardcoded in {@link EstateManager} and
 * {@link EconomyManager} are now accessible here for easy server-side
 * customisation.
 * <p>
 * Modify these values before the first server tick to change behaviour.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     // In your mod's server initializer:
 *     EconomyConfig.ESTATE_GROWTH_PER_TICK = 0.10f; // faster estates
 *     EconomyConfig.ESTATE_PAYOUT_AMOUNT   = 100;   // bigger payouts
 * }</pre>
 */
public final class EconomyConfig {

    private EconomyConfig() { /* static config holder */ }

    // ── Estate settings ──────────────────────────────────────────────────

    /**
     * Percentage points added per server tick (20 tps).
     * Default: 0.05 per tick → full cycle ≈ 100 seconds (2000 ticks).
     */
    public static float ESTATE_GROWTH_PER_TICK = 0.05f;

    /**
     * Money deposited when the estate growth bar completes a full cycle.
     * Default: 50.
     */
    public static int ESTATE_PAYOUT_AMOUNT = 50;

    /**
     * How often (in ticks) to sync the estate percentage to clients.
     * Default: 10 (2 Hz — smooth for progress bars, low bandwidth).
     */
    public static int ESTATE_SYNC_INTERVAL = 10;

    // ── Economy general ──────────────────────────────────────────────────

    /**
     * Starting balance for new players.
     * Default: 0.
     */
    public static int STARTING_BALANCE = 0;

    /**
     * Maximum possible balance (cap to avoid overflow).
     * Default: {@link Integer#MAX_VALUE}.
     */
    public static int MAX_BALANCE = Integer.MAX_VALUE;
}
