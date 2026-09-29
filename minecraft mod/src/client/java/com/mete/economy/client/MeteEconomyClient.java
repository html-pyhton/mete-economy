package com.mete.economy.client;

import com.mete.economy.client.gui.EconomyMenuScreen;
import com.mete.economy.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class MeteEconomyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register payload codecs on client so C2S sends and S2C receives work
        ModNetworking.registerPayloads();

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OpenMenuPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft.getInstance().setScreen(new EconomyMenuScreen());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SyncDataPayload.TYPE, (payload, context) -> {
            EconomyMenuScreen.cachedItemPrices = payload.itemPrices();
            EconomyMenuScreen.cachedEnchantPrices = payload.enchantPrices();
            EconomyMenuScreen.cachedEconomyEnabled = payload.economyEnabled();
            EconomyMenuScreen.cachedEnchantEnabled = payload.enchantEnabled();
            EconomyMenuScreen.cachedMaxLevel = payload.maxLevel();
        });
    }
}
