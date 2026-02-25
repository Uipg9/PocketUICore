package com.pocketuicore.command;

import com.pocketuicore.screen.PocketMenuScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

/**
 * Client-side command registration for the Pocket menu.
 * <p>
 * Registers {@code /pocket} and {@code /p} as client commands.
 * Both open the {@link PocketMenuScreen} on the render thread.
 * <p>
 * Called once from {@link com.pocketuicore.PocketUICoreClient#onInitializeClient()}.
 */
public final class PocketCommandRegister {

    private PocketCommandRegister() { /* static utility */ }

    /**
     * Register all Pocket client commands.
     */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // /pocket
            dispatcher.register(ClientCommandManager.literal("pocket")
                    .executes(context -> {
                        openPocketScreen();
                        return 1;
                    }));

            // /p  (alias)
            dispatcher.register(ClientCommandManager.literal("p")
                    .executes(context -> {
                        openPocketScreen();
                        return 1;
                    }));
        });
    }

    /**
     * Schedule the screen opening on the render thread so it is
     * safe to call from the network-thread command dispatcher.
     */
    private static void openPocketScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new PocketMenuScreen()));
    }
}
