package com.pocketuicore;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PocketUICore — Server-side entrypoint.
 * <p>
 * Registers server-side systems (notification manager, etc.).
 * All rendering and animation lives in the client entrypoint.
 */
public class PocketUICore implements ModInitializer {

    public static final String MOD_ID = "pocketuicore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[PocketUICore] Core library loaded — procedural UI, notifications, animations ready.");
    }
}
