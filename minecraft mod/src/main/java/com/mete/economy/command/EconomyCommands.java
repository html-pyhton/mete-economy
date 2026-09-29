package com.mete.economy.command;

import com.mete.economy.MeteEconomy;
import com.mete.economy.data.EconomyState;
import com.mete.economy.network.ModNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EconomyCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /para
        dispatcher.register(Commands.literal("para")
            .executes(ctx -> showBalance(ctx, ctx.getSource().getPlayerOrException()))
            .then(Commands.literal("ver")
                .requires(src -> {
                    var pl = src.getPlayer();
                    if (pl == null) return true;
                    return src.getServer().getPlayerList().isOp(pl.nameAndId());
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("miktar", LongArgumentType.longArg(0))
                        .executes(ctx -> giveMoney(ctx,
                            EntityArgument.getPlayer(ctx, "player"),
                            LongArgumentType.getLong(ctx, "miktar"))))))
            .then(Commands.literal("al")
                .requires(src -> {
                    var pl = src.getPlayer();
                    if (pl == null) return true;
                    return src.getServer().getPlayerList().isOp(pl.nameAndId());
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("miktar", LongArgumentType.longArg(0))
                        .executes(ctx -> takeMoney(ctx,
                            EntityArgument.getPlayer(ctx, "player"),
                            LongArgumentType.getLong(ctx, "miktar"))))))
            .then(Commands.literal("set")
                .requires(src -> {
                    var pl = src.getPlayer();
                    if (pl == null) return true;
                    return src.getServer().getPlayerList().isOp(pl.nameAndId());
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("miktar", LongArgumentType.longArg(0))
                        .executes(ctx -> setMoney(ctx,
                            EntityArgument.getPlayer(ctx, "player"),
                            LongArgumentType.getLong(ctx, "miktar")))))));

        // /ekonomimenu
        dispatcher.register(Commands.literal("ekonomimenu")
            .requires(src -> {
                    var pl = src.getPlayer();
                    if (pl == null) return true;
                    return src.getServer().getPlayerList().isOp(pl.nameAndId());
                })
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ModNetworking.openEconomyMenu(player);
                return 1;
            }));

        // /buyenchant
        dispatcher.register(Commands.literal("buyenchant")
            .then(Commands.argument("enchant", StringArgumentType.string())
                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                    .executes(ctx -> buyEnchant(ctx,
                        StringArgumentType.getString(ctx, "enchant"),
                        IntegerArgumentType.getInteger(ctx, "level"))))));

        // /al <item> [adet]
        dispatcher.register(Commands.literal("al")
            .then(Commands.argument("item", StringArgumentType.string())
                .executes(ctx -> buyItem(ctx, StringArgumentType.getString(ctx, "item"), 1))
                .then(Commands.argument("adet", IntegerArgumentType.integer(1, 64))
                    .executes(ctx -> buyItem(ctx,
                        StringArgumentType.getString(ctx, "item"),
                        IntegerArgumentType.getInteger(ctx, "adet"))))));

        // /sat [adet]
        dispatcher.register(Commands.literal("sat")
            .executes(ctx -> sellItem(ctx, -1))
            .then(Commands.argument("adet", IntegerArgumentType.integer(1, 64))
                .executes(ctx -> sellItem(ctx, IntegerArgumentType.getInteger(ctx, "adet")))));

        // /fiyat [item]
        dispatcher.register(Commands.literal("fiyat")
            .executes(ctx -> showPriceHeld(ctx))
            .then(Commands.argument("item", StringArgumentType.string())
                .executes(ctx -> showPrice(ctx, StringArgumentType.getString(ctx, "item")))));
    }

    private static MinecraftServer server() {
        return MeteEconomy.getServer();
    }

    private static String normalizeItemId(String raw) {
        if (!raw.contains(":")) {
            return "minecraft:" + raw.toLowerCase();
        }
        return raw.toLowerCase();
    }

    private static int showBalance(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);
        long balance = state.getBalance(player.getUUID());
        player.sendSystemMessage(Component.literal("§a§lMete Economy §7» §fBakiyen: §e" + balance + " TL"));
        return 1;
    }

    private static int giveMoney(CommandContext<CommandSourceStack> ctx, ServerPlayer target, long amount) {
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);
        state.addBalance(target.getUUID(), amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§a§lMete Economy §7» §e" + amount + " TL §fverildi → " + target.getName().getString()), true);
        target.sendSystemMessage(Component.literal("§a§lMete Economy §7» §e" + amount + " TL §fkazandın! Bakiye: §e" + state.getBalance(target.getUUID()) + " TL"));
        return 1;
    }

    private static int takeMoney(CommandContext<CommandSourceStack> ctx, ServerPlayer target, long amount) {
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);
        if (!state.removeBalance(target.getUUID(), amount)) {
            ctx.getSource().sendFailure(Component.literal("§cYeterli para yok!"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§a§lMete Economy §7» §e" + amount + " TL §falınd → " + target.getName().getString()), true);
        target.sendSystemMessage(Component.literal("§c§lMete Economy §7» §e" + amount + " TL §fkaybettin! Bakiye: §e" + state.getBalance(target.getUUID()) + " TL"));
        return 1;
    }

    private static int setMoney(CommandContext<CommandSourceStack> ctx, ServerPlayer target, long amount) {
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);
        state.setBalance(target.getUUID(), amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§a§lMete Economy §7» §fBakiye ayarlandı: §e" + amount + " TL"), true);
        target.sendSystemMessage(Component.literal("§a§lMete Economy §7» §fBakiyen §e" + amount + " TL §foldu."));
        return 1;
    }

    private static int buyItem(CommandContext<CommandSourceStack> ctx, String itemKey, int amount) {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);

        if (!state.isEconomyEnabled()) {
            player.sendSystemMessage(Component.literal("§cPara sistemi kapalı!"));
            return 0;
        }

        String itemId = normalizeItemId(itemKey);
        long unitPrice = state.getBuyPrice(itemId);
        if (unitPrice <= 0) {
            player.sendSystemMessage(Component.literal("§cBu eşya satılmıyor veya fiyat 0!"));
            return 0;
        }

        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            player.sendSystemMessage(Component.literal("§cGeçersiz eşya: " + itemId));
            return 0;
        }

        Item item = BuiltInRegistries.ITEM.getValue(id);
        long total = unitPrice * amount;
        if (!state.removeBalance(player.getUUID(), total)) {
            player.sendSystemMessage(Component.literal("§cYeterli paran yok! Gerekli: §e" + total + " TL §7(Bakiyen: §e" + state.getBalance(player.getUUID()) + " TL§7)"));
            return 0;
        }

        ItemStack stack = new ItemStack(item, amount);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        player.sendSystemMessage(Component.literal(
            "§a§lMete Economy §7» §f" + amount + "x " + itemId.replace("minecraft:", "") +
            " satın aldın (§e" + total + " TL§f). Bakiye: §e" + state.getBalance(player.getUUID()) + " TL"));
        return 1;
    }

    private static int sellItem(CommandContext<CommandSourceStack> ctx, int amount) {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);

        if (!state.isEconomyEnabled()) {
            player.sendSystemMessage(Component.literal("§cPara sistemi kapalı!"));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cElinde eşya yok!"));
            return 0;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (itemId == null) {
            player.sendSystemMessage(Component.literal("§cGeçersiz eşya!"));
            return 0;
        }

        String idStr = itemId.toString();
        long unitPrice = state.getSellPrice(idStr);
        if (unitPrice <= 0) {
            player.sendSystemMessage(Component.literal("§cBu eşya mağazaya satılamıyor!"));
            return 0;
        }

        int sellCount = amount < 0 ? held.getCount() : Math.min(amount, held.getCount());
        long total = unitPrice * sellCount;

        held.shrink(sellCount);
        state.addBalance(player.getUUID(), total);

        player.sendSystemMessage(Component.literal(
            "§a§lMete Economy §7» §f" + sellCount + "x " + idStr.replace("minecraft:", "") +
            " sattın (§e" + total + " TL§f). Bakiye: §e" + state.getBalance(player.getUUID()) + " TL"));
        return 1;
    }

    private static int showPriceHeld(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cElinde eşya yok! Kullanım: /fiyat <item>"));
            return 0;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (itemId == null) return 0;
        return showPrice(ctx, itemId.toString());
    }

    private static int showPrice(CommandContext<CommandSourceStack> ctx, String itemKey) {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);

        String itemId = normalizeItemId(itemKey);
        long buy = state.getBuyPrice(itemId);
        long sell = state.getSellPrice(itemId);

        player.sendSystemMessage(Component.literal(
            "§a§lMete Economy §7» §f" + itemId.replace("minecraft:", "") +
            " → Alış: §e" + buy + " TL §7| Satış: §e" + sell + " TL"));
        return 1;
    }

    private static int buyEnchant(CommandContext<CommandSourceStack> ctx, String enchantKey, int level) {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer s = server();
        if (s == null) return 0;
        EconomyState state = EconomyState.get(s);

        if (!state.isEnchantSystemEnabled()) {
            player.sendSystemMessage(Component.literal("§cBüyü sistemi kapalı!"));
            return 0;
        }
        if (level > state.getMaxEnchantLevel()) {
            player.sendSystemMessage(Component.literal("§cMaks seviye: " + state.getMaxEnchantLevel()));
            return 0;
        }
        if (!enchantKey.contains(":")) {
            enchantKey = "minecraft:" + enchantKey;
        }

        long price = state.getEnchantPrice(enchantKey, level);
        if (!state.removeBalance(player.getUUID(), price)) {
            player.sendSystemMessage(Component.literal("§cYeterli paran yok! Gerekli: §e" + price + " TL"));
            return 0;
        }

        boolean success = com.mete.economy.enchant.EnchantHelper.applyEnchant(player, enchantKey, level);
        if (!success) {
            state.addBalance(player.getUUID(), price);
            player.sendSystemMessage(Component.literal("§cBüyü uygulanamadı (elinde uygun eşya yok?), para iade edildi."));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§a§lMete Economy §7» §f" + enchantKey + " " + level + " uygulandı (§e" + price + " TL§f)"));
        return 1;
    }
}
