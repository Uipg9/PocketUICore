package com.pocketuicore.controller;

import com.pocketuicore.screen.PocketMenuScreen;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;

/**
 * Controlify Compatibility — Optional Integration
 * <p>
 * This class is only loaded when Controlify is present at runtime.
 * It registers a {@link PocketMenuScreenProcessor} so Controlify
 * knows how to handle our custom screens — primarily to disable the
 * virtual mouse (vmouse) and let our {@link ControllerHandler} manage
 * focus navigation natively.
 * <p>
 * Registered via the {@code "controlify"} entrypoint in
 * {@code fabric.mod.json}.
 *
 * @see ControllerHandler
 */
public class PocketControlifyCompat implements ControlifyEntrypoint {

    @Override
    public void onControlifyPreInit(PreInitContext ctx) {
        // Register our custom ScreenProcessor for PocketMenuScreen.
        // This tells Controlify to disable vmouse on our screens
        // and defer input handling to our ControllerHandler + FocusManager.
        ScreenProcessorProvider.registerProvider(
                PocketMenuScreen.class,
                PocketMenuScreenProcessor::new
        );
    }

    @Override
    public void onControlifyInit(InitContext ctx) {
        // No-op — no init-time work needed.
    }

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {
        // No-op — we don't need to query controllers here.
    }
}
