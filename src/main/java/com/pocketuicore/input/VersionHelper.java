package com.pocketuicore.input;

import net.minecraft.SharedConstants;

/**
 * Version-Aware Abstraction Layer — detects the running Minecraft version
 * and exposes version checks so downstream mods can branch on API differences.
 * <p>
 * Combined with {@link InputHelper} (which normalises Click/KeyInput across
 * versions) this provides the "multi-version abstraction" layer of PocketUICore.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 *     if (VersionHelper.isAtLeast(1, 21, 11)) {
 *         // Use new matrix stack API
 *     }
 * }</pre>
 *
 * @since 1.13.0
 */
public final class VersionHelper {

    private VersionHelper() {}

    /** The full Minecraft version string (e.g. "1.21.11"). */
    public static final String MC_VERSION = SharedConstants.getGameVersion().name();

    private static final int[] PARSED = parseVersion(MC_VERSION);

    /** Major version number (e.g. 1). */
    public static int major() { return PARSED[0]; }
    /** Minor version number (e.g. 21). */
    public static int minor() { return PARSED[1]; }
    /** Patch version number (e.g. 11). */
    public static int patch() { return PARSED[2]; }

    /**
     * Check if the running MC version is at least the given version.
     *
     * @return {@code true} if running version &ge; specified version
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (PARSED[0] != major) return PARSED[0] > major;
        if (PARSED[1] != minor) return PARSED[1] > minor;
        return PARSED[2] >= patch;
    }

    /**
     * Check if the running MC version is exactly the given version.
     */
    public static boolean isExactly(int major, int minor, int patch) {
        return PARSED[0] == major && PARSED[1] == minor && PARSED[2] == patch;
    }

    private static int[] parseVersion(String ver) {
        int[] result = {0, 0, 0};
        // Strip anything after a space or dash (e.g. "1.21.11-pre1")
        int end = ver.indexOf('-');
        if (end < 0) end = ver.indexOf(' ');
        if (end > 0) ver = ver.substring(0, end);

        String[] parts = ver.split("\\.");
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try { result[i] = Integer.parseInt(parts[i]); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
