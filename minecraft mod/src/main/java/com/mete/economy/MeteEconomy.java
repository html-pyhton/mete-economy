package com.mete.economy;

import com.mete.economy.command.EconomyCommands;
import com.mete.economy.data.EconomyState;
import com.mete.economy.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteEconomy implements ModInitializer {
    public static final String MOD_ID = "mete-economy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        LOGGER.info("Mete Economy v1.1 initializing...");

        ModNetworking.registerServer();
        EconomyCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            EconomyState.get(s); // load
            LOGGER.info("Economy state loaded.");
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            if (server != null) {
                EconomyState state = EconomyState.get(server);
                state.save();
            }
            EconomyState.reset();
            server = null;
        });

        LOGGER.info("Mete Economy ready!");
    }

    public static MinecraftServer getServer() {
        return server;
    }
}
