package com.mete.economy.network;

import com.mete.economy.MeteEconomy;
import com.mete.economy.data.EconomyState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class ModNetworking {
    public static final Identifier OPEN_MENU = Identifier.fromNamespaceAndPath(MeteEconomy.MOD_ID, "open_menu");
    public static final Identifier UPDATE_ITEM_PRICE = Identifier.fromNamespaceAndPath(MeteEconomy.MOD_ID, "update_item_price");
    public static final Identifier UPDATE_ENCHANT_PRICE = Identifier.fromNamespaceAndPath(MeteEconomy.MOD_ID, "update_enchant_price");
    public static final Identifier UPDATE_SETTING = Identifier.fromNamespaceAndPath(MeteEconomy.MOD_ID, "update_setting");
    public static final Identifier SYNC_DATA = Identifier.fromNamespaceAndPath(MeteEconomy.MOD_ID, "sync_data");

    private static boolean payloadsRegistered = false;

    public record OpenMenuPayload() implements CustomPacketPayload {
        public static final Type<OpenMenuPayload> TYPE = new Type<>(OPEN_MENU);
        public static final StreamCodec<FriendlyByteBuf, OpenMenuPayload> CODEC = StreamCodec.unit(new OpenMenuPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UpdateItemPricePayload(String itemId, long buy, long sell) implements CustomPacketPayload {
        public static final Type<UpdateItemPricePayload> TYPE = new Type<>(UPDATE_ITEM_PRICE);
        public static final StreamCodec<FriendlyByteBuf, UpdateItemPricePayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.itemId); buf.writeLong(p.buy); buf.writeLong(p.sell); },
            buf -> new UpdateItemPricePayload(buf.readUtf(), buf.readLong(), buf.readLong())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UpdateEnchantPricePayload(String enchantId, long pricePerLevel) implements CustomPacketPayload {
        public static final Type<UpdateEnchantPricePayload> TYPE = new Type<>(UPDATE_ENCHANT_PRICE);
        public static final StreamCodec<FriendlyByteBuf, UpdateEnchantPricePayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.enchantId); buf.writeLong(p.pricePerLevel); },
            buf -> new UpdateEnchantPricePayload(buf.readUtf(), buf.readLong())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UpdateSettingPayload(String key, String value) implements CustomPacketPayload {
        public static final Type<UpdateSettingPayload> TYPE = new Type<>(UPDATE_SETTING);
        public static final StreamCodec<FriendlyByteBuf, UpdateSettingPayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.key); buf.writeUtf(p.value); },
            buf -> new UpdateSettingPayload(buf.readUtf(), buf.readUtf())
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncDataPayload(
        Map<String, long[]> itemPrices,
        Map<String, Long> enchantPrices,
        boolean economyEnabled,
        boolean enchantEnabled,
        int maxLevel
    ) implements CustomPacketPayload {
        public static final Type<SyncDataPayload> TYPE = new Type<>(SYNC_DATA);
        public static final StreamCodec<FriendlyByteBuf, SyncDataPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.itemPrices.size());
                p.itemPrices.forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeLong(v[0]);
                    buf.writeLong(v[1]);
                });
                buf.writeVarInt(p.enchantPrices.size());
                p.enchantPrices.forEach((k, v) -> { buf.writeUtf(k); buf.writeLong(v); });
                buf.writeBoolean(p.economyEnabled);
                buf.writeBoolean(p.enchantEnabled);
                buf.writeInt(p.maxLevel);
            },
            buf -> {
                Map<String, long[]> items = new HashMap<>();
                int is = buf.readVarInt();
                for (int i = 0; i < is; i++) {
                    items.put(buf.readUtf(), new long[]{buf.readLong(), buf.readLong()});
                }
                Map<String, Long> enchants = new HashMap<>();
                int es = buf.readVarInt();
                for (int i = 0; i < es; i++) enchants.put(buf.readUtf(), buf.readLong());
                return new SyncDataPayload(items, enchants, buf.readBoolean(), buf.readBoolean(), buf.readInt());
            }
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Idempotent payload codec registration (safe to call from client + server). */
    public static synchronized void registerPayloads() {
        if (payloadsRegistered) return;
        PayloadTypeRegistry.playS2C().register(OpenMenuPayload.TYPE, OpenMenuPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDataPayload.TYPE, SyncDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateItemPricePayload.TYPE, UpdateItemPricePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateEnchantPricePayload.TYPE, UpdateEnchantPricePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateSettingPayload.TYPE, UpdateSettingPayload.CODEC);
        payloadsRegistered = true;
    }

    private static boolean isOp(ServerPlayer player) {
        MinecraftServer server = MeteEconomy.getServer();
        if (server == null) return false;
        return server.getPlayerList().isOp(player.nameAndId());
    }

    public static void registerServer() {
        registerPayloads();

        ServerPlayNetworking.registerGlobalReceiver(UpdateItemPricePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!isOp(player)) return;
            MinecraftServer server = MeteEconomy.getServer();
            if (server == null) return;
            EconomyState state = EconomyState.get(server);
            state.setItemPrice(payload.itemId(), payload.buy(), payload.sell());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a§lMete Economy §7» §f" + payload.itemId() +
                " → Alış: §e" + payload.buy() + " TL §7| Satış: §e" + payload.sell() + " TL"));
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdateEnchantPricePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!isOp(player)) return;
            MinecraftServer server = MeteEconomy.getServer();
            if (server == null) return;
            EconomyState state = EconomyState.get(server);
            state.setEnchantPricePerLevel(payload.enchantId(), payload.pricePerLevel());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a§lMete Economy §7» §f" + payload.enchantId() + " seviye başı §e" + payload.pricePerLevel() + " TL"));
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdateSettingPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!isOp(player)) return;
            MinecraftServer server = MeteEconomy.getServer();
            if (server == null) return;
            EconomyState state = EconomyState.get(server);
            switch (payload.key()) {
                case "economyEnabled" -> state.setEconomyEnabled(Boolean.parseBoolean(payload.value()));
                case "enchantEnabled" -> state.setEnchantSystemEnabled(Boolean.parseBoolean(payload.value()));
                case "maxEnchantLevel" -> {
                    try { state.setMaxEnchantLevel(Integer.parseInt(payload.value())); }
                    catch (NumberFormatException ignored) {}
                }
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a§lMete Economy §7» §fAyar: " + payload.key() + " = " + payload.value()));
        });
    }

    public static void openEconomyMenu(ServerPlayer player) {
        MinecraftServer server = MeteEconomy.getServer();
        if (server == null) return;
        EconomyState state = EconomyState.get(server);

        Map<String, long[]> items = new HashMap<>();
        state.getAllItemPrices().forEach((k, v) -> items.put(k, new long[]{v.buy, v.sell}));

        ServerPlayNetworking.send(player, new SyncDataPayload(
            items,
            state.getAllEnchantPrices(),
            state.isEconomyEnabled(),
            state.isEnchantSystemEnabled(),
            state.getMaxEnchantLevel()
        ));
        ServerPlayNetworking.send(player, new OpenMenuPayload());
    }
}
