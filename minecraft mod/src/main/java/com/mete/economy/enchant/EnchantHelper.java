package com.mete.economy.enchant;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

public class EnchantHelper {
    public static boolean applyEnchant(ServerPlayer player, String enchantId, int level) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }

        try {
            Optional<Holder.Reference<Enchantment>> opt = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Identifier.parse(enchantId));

            if (opt.isEmpty()) {
                return false;
            }

            Holder<Enchantment> holder = opt.get();

            ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
            mutable.set(holder, level);
            EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
