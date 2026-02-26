package com.pocketuicore.render;

/**
 * Static utility methods for formatting numbers, durations, and
 * percentages into human-readable display strings.
 * <p>
 * All methods are pure functions with no side effects — safe to call
 * from any thread.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     String gold   = UIFormatUtils.formatGold(12500);    // "12,500g"
 *     String compact = UIFormatUtils.formatCompact(1_500_000); // "1.5M"
 *     String time   = UIFormatUtils.formatTime(3661);     // "1h 1m 1s"
 *     String pct    = UIFormatUtils.formatPercent(0.753f); // "75%"
 * }</pre>
 */
public final class UIFormatUtils {

    private UIFormatUtils() { /* static utility */ }

    // =====================================================================
    //  Gold / currency formatting
    // =====================================================================

    /**
     * Format a gold amount with comma-separated thousands and a trailing
     * 'g' suffix.
     *
     * @param amount the gold amount (can be negative)
     * @return formatted string, e.g. "12,500g"
     */
    public static String formatGold(int amount) {
        return String.format("%,dg", amount);
    }

    /**
     * Format a gold amount with comma separation but no suffix.
     *
     * @param amount the gold amount
     * @return formatted string, e.g. "12,500"
     */
    public static String formatNumber(int amount) {
        return String.format("%,d", amount);
    }

    /**
     * Format a long value with comma separation.
     *
     * @param amount the value
     * @return formatted string
     */
    public static String formatNumber(long amount) {
        return String.format("%,d", amount);
    }

    // =====================================================================
    //  Compact notation
    // =====================================================================

    /**
     * Format a number in compact notation (K, M, B).
     * <p>
     * Examples: 999 → "999", 1500 → "1.5K", 1_500_000 → "1.5M",
     * 2_500_000_000 → "2.5B"
     *
     * @param value the number
     * @return compact string representation
     */
    public static String formatCompact(long value) {
        boolean negative = value < 0;
        long abs = Math.abs(value);
        String prefix = negative ? "-" : "";

        if (abs >= 1_000_000_000L) {
            return prefix + formatDecimal(abs, 1_000_000_000L) + "B";
        } else if (abs >= 1_000_000L) {
            return prefix + formatDecimal(abs, 1_000_000L) + "M";
        } else if (abs >= 1_000L) {
            return prefix + formatDecimal(abs, 1_000L) + "K";
        } else {
            return prefix + abs;
        }
    }

    /** Convenience overload for int values. */
    public static String formatCompact(int value) {
        return formatCompact((long) value);
    }

    private static String formatDecimal(long value, long divisor) {
        long whole = value / divisor;
        long remainder = (value % divisor) * 10 / divisor;
        if (remainder == 0) {
            return String.valueOf(whole);
        }
        return whole + "." + remainder;
    }

    // =====================================================================
    //  Time / duration formatting
    // =====================================================================

    /**
     * Format a duration in seconds into a human-readable string.
     * <p>
     * Examples: 45 → "45s", 125 → "2m 5s", 3661 → "1h 1m 1s",
     * 90061 → "1d 1h 1m 1s"
     *
     * @param totalSeconds duration in seconds (must be non-negative)
     * @return formatted duration string
     */
    public static String formatTime(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        if (totalSeconds == 0) return "0s";

        int days    = totalSeconds / 86400;
        int hours   = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    /**
     * Format a duration in ticks (20 ticks = 1 second) into a
     * human-readable string.
     *
     * @param ticks duration in game ticks
     * @return formatted duration string
     */
    public static String formatTicks(int ticks) {
        return formatTime(ticks / 20);
    }

    /**
     * Format a duration as a compact countdown (mm:ss or h:mm:ss).
     * <p>
     * Examples: 65 → "1:05", 3661 → "1:01:01"
     *
     * @param totalSeconds duration in seconds
     * @return formatted countdown string
     */
    public static String formatCountdown(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int hours   = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    // =====================================================================
    //  Percentage formatting
    // =====================================================================

    /**
     * Format a float ratio as a percentage (rounded, no decimal).
     * <p>
     * Examples: 0.753f → "75%", 1.0f → "100%", 0.0f → "0%"
     *
     * @param ratio 0.0 to 1.0
     * @return percentage string
     */
    public static String formatPercent(float ratio) {
        return Math.round(ratio * 100) + "%";
    }

    /**
     * Format a float ratio as a percentage with one decimal.
     * <p>
     * Examples: 0.753f → "75.3%", 1.0f → "100.0%"
     *
     * @param ratio 0.0 to 1.0
     * @return percentage string with one decimal
     */
    public static String formatPercentDecimal(float ratio) {
        return String.format("%.1f%%", ratio * 100);
    }

    /**
     * Format a fraction as "current / max".
     *
     * @param current current value
     * @param max     maximum value
     * @return fraction string, e.g. "50 / 100"
     */
    public static String formatFraction(int current, int max) {
        return formatNumber(current) + " / " + formatNumber(max);
    }

    // =====================================================================
    //  Miscellaneous
    // =====================================================================

    /**
     * Pluralise a word based on count.
     * <p>
     * Examples: pluralise(1, "field") → "1 field",
     *           pluralise(3, "field") → "3 fields"
     *
     * @param count the count
     * @param singular the singular form
     * @return formatted count + word string
     */
    public static String pluralise(int count, String singular) {
        return formatNumber(count) + " " + singular + (count == 1 ? "" : "s");
    }

    /**
     * Pluralise with a custom plural form.
     *
     * @param count    the count
     * @param singular the singular form
     * @param plural   the plural form
     * @return formatted count + word string
     */
    public static String pluralise(int count, String singular, String plural) {
        return formatNumber(count) + " " + (count == 1 ? singular : plural);
    }
}
