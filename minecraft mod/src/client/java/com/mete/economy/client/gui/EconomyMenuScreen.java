package com.mete.economy.client.gui;

import com.mete.economy.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EconomyMenuScreen extends Screen {
    // itemId -> [buy, sell]
    public static Map<String, long[]> cachedItemPrices = new HashMap<>();
    public static Map<String, Long> cachedEnchantPrices = new HashMap<>();
    public static boolean cachedEconomyEnabled = true;
    public static boolean cachedEnchantEnabled = true;
    public static int cachedMaxLevel = 1000;

    private enum Tab { MAIN, ITEMS, ENCHANTS, SETTINGS, EDIT_ITEM, EDIT_ENCHANT }
    private Tab currentTab = Tab.MAIN;
    private String editingKey = null;
    private EditBox buyBox;
    private EditBox sellBox;
    private EditBox priceBox;
    private final List<Button> dynamicButtons = new ArrayList<>();

    public EconomyMenuScreen() {
        super(Component.literal("Mete Economy Yönetim"));
    }

    @Override
    protected void init() {
        clearWidgets();
        dynamicButtons.clear();
        buyBox = null;
        sellBox = null;
        priceBox = null;

        int centerX = this.width / 2;
        int startY = 40;

        if (currentTab == Tab.MAIN) {
            addRenderableWidget(Button.builder(Component.literal("💰 Eşya Fiyatları (Alış / Satış)"), b -> {
                currentTab = Tab.ITEMS;
                init();
            }).bounds(centerX - 120, startY, 240, 20).build());

            addRenderableWidget(Button.builder(Component.literal("✨ Büyü Fiyatları"), b -> {
                currentTab = Tab.ENCHANTS;
                init();
            }).bounds(centerX - 100, startY + 30, 200, 20).build());

            addRenderableWidget(Button.builder(Component.literal("⚙️ Genel Ayarlar"), b -> {
                currentTab = Tab.SETTINGS;
                init();
            }).bounds(centerX - 100, startY + 60, 200, 20).build());

            addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
                .bounds(centerX - 50, startY + 100, 100, 20).build());
        } else if (currentTab == Tab.ITEMS) {
            addRenderableWidget(Button.builder(Component.literal("← Geri"), b -> {
                currentTab = Tab.MAIN;
                init();
            }).bounds(10, 10, 60, 20).build());

            int y = 45;
            int i = 0;
            for (Map.Entry<String, long[]> e : cachedItemPrices.entrySet()) {
                String key = e.getKey();
                long buy = e.getValue()[0];
                long sell = e.getValue()[1];
                String shortName = key.replace("minecraft:", "");
                String display = shortName + "  |  Alış: " + buy + " TL  |  Satış: " + sell + " TL";
                Button btn = Button.builder(Component.literal(display), b -> {
                    editingKey = key;
                    currentTab = Tab.EDIT_ITEM;
                    init();
                }).bounds(centerX - 160, y + (i * 22), 320, 20).build();
                addRenderableWidget(btn);
                dynamicButtons.add(btn);
                i++;
                if (i > 12) break;
            }
        } else if (currentTab == Tab.ENCHANTS) {
            addRenderableWidget(Button.builder(Component.literal("← Geri"), b -> {
                currentTab = Tab.MAIN;
                init();
            }).bounds(10, 10, 60, 20).build());

            int y = 50;
            int i = 0;
            for (Map.Entry<String, Long> e : cachedEnchantPrices.entrySet()) {
                String key = e.getKey();
                long price = e.getValue();
                String display = key.replace("minecraft:", "") + " (seviye başı): " + price + " TL";
                Button btn = Button.builder(Component.literal(display), b -> {
                    editingKey = key;
                    currentTab = Tab.EDIT_ENCHANT;
                    init();
                }).bounds(centerX - 140, y + (i * 24), 280, 20).build();
                addRenderableWidget(btn);
                dynamicButtons.add(btn);
                i++;
                if (i > 10) break;
            }
        } else if (currentTab == Tab.SETTINGS) {
            addRenderableWidget(Button.builder(Component.literal("← Geri"), b -> {
                currentTab = Tab.MAIN;
                init();
            }).bounds(10, 10, 60, 20).build());

            addRenderableWidget(Button.builder(
                Component.literal("Para Sistemi: " + (cachedEconomyEnabled ? "§aAÇIK" : "§cKAPALI")),
                b -> {
                    cachedEconomyEnabled = !cachedEconomyEnabled;
                    ClientPlayNetworking.send(new ModNetworking.UpdateSettingPayload("economyEnabled", String.valueOf(cachedEconomyEnabled)));
                    init();
                }).bounds(centerX - 100, 50, 200, 20).build());

            addRenderableWidget(Button.builder(
                Component.literal("Büyü Sistemi: " + (cachedEnchantEnabled ? "§aAÇIK" : "§cKAPALI")),
                b -> {
                    cachedEnchantEnabled = !cachedEnchantEnabled;
                    ClientPlayNetworking.send(new ModNetworking.UpdateSettingPayload("enchantEnabled", String.valueOf(cachedEnchantEnabled)));
                    init();
                }).bounds(centerX - 100, 80, 200, 20).build());

            addRenderableWidget(Button.builder(
                Component.literal("Max Büyü Seviyesi: " + cachedMaxLevel + " (tıkla +100)"),
                b -> {
                    cachedMaxLevel += 100;
                    if (cachedMaxLevel > 10000) cachedMaxLevel = 100;
                    ClientPlayNetworking.send(new ModNetworking.UpdateSettingPayload("maxEnchantLevel", String.valueOf(cachedMaxLevel)));
                    init();
                }).bounds(centerX - 120, 110, 240, 20).build());
        } else if (currentTab == Tab.EDIT_ITEM && editingKey != null) {
            addRenderableWidget(Button.builder(Component.literal("← Geri"), b -> {
                currentTab = Tab.ITEMS;
                editingKey = null;
                init();
            }).bounds(10, 10, 60, 20).build());

            long[] current = cachedItemPrices.getOrDefault(editingKey, new long[]{0, 0});

            buyBox = new EditBox(this.font, centerX - 50, 75, 100, 20, Component.literal("Alış"));
            buyBox.setValue(String.valueOf(current[0]));
            buyBox.setMaxLength(12);
            buyBox.setFilter(s -> s.matches("\\d*"));
            addRenderableWidget(buyBox);

            sellBox = new EditBox(this.font, centerX - 50, 120, 100, 20, Component.literal("Satış"));
            sellBox.setValue(String.valueOf(current[1]));
            sellBox.setMaxLength(12);
            sellBox.setFilter(s -> s.matches("\\d*"));
            addRenderableWidget(sellBox);

            addRenderableWidget(Button.builder(Component.literal("Kaydet"), b -> {
                try {
                    long newBuy = Long.parseLong(buyBox.getValue().isEmpty() ? "0" : buyBox.getValue());
                    long newSell = Long.parseLong(sellBox.getValue().isEmpty() ? "0" : sellBox.getValue());
                    cachedItemPrices.put(editingKey, new long[]{newBuy, newSell});
                    ClientPlayNetworking.send(new ModNetworking.UpdateItemPricePayload(editingKey, newBuy, newSell));
                    currentTab = Tab.ITEMS;
                    editingKey = null;
                    init();
                } catch (NumberFormatException ignored) {}
            }).bounds(centerX - 50, 155, 100, 20).build());
        } else if (currentTab == Tab.EDIT_ENCHANT && editingKey != null) {
            addRenderableWidget(Button.builder(Component.literal("← Geri"), b -> {
                currentTab = Tab.ENCHANTS;
                editingKey = null;
                init();
            }).bounds(10, 10, 60, 20).build());

            long current = cachedEnchantPrices.getOrDefault(editingKey, 0L);

            priceBox = new EditBox(this.font, centerX - 50, 80, 100, 20, Component.literal("Fiyat"));
            priceBox.setValue(String.valueOf(current));
            priceBox.setMaxLength(12);
            priceBox.setFilter(s -> s.matches("\\d*"));
            addRenderableWidget(priceBox);

            addRenderableWidget(Button.builder(Component.literal("Kaydet"), b -> {
                try {
                    long newPrice = Long.parseLong(priceBox.getValue().isEmpty() ? "0" : priceBox.getValue());
                    cachedEnchantPrices.put(editingKey, newPrice);
                    ClientPlayNetworking.send(new ModNetworking.UpdateEnchantPricePayload(editingKey, newPrice));
                    currentTab = Tab.ENCHANTS;
                    editingKey = null;
                    init();
                } catch (NumberFormatException ignored) {}
            }).bounds(centerX - 50, 110, 100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, "§6§lMete Economy Yönetim Menüsü", centerX, 15, 0xFFFFFF);

        if (currentTab == Tab.EDIT_ITEM && editingKey != null) {
            String name = editingKey.replace("minecraft:", "");
            graphics.drawCenteredString(this.font, "§e" + name, centerX, 45, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§aAlış Fiyatı (oyuncu satın alır):", centerX, 62, 0xAAAAAA);
            graphics.drawCenteredString(this.font, "§cSatış Fiyatı (oyuncu satar):", centerX, 107, 0xAAAAAA);
        }

        if (currentTab == Tab.EDIT_ENCHANT && editingKey != null) {
            graphics.drawCenteredString(this.font, "§eBüyü Seviye Başı Fiyat: §f" + editingKey.replace("minecraft:", ""), centerX, 55, 0xFFFFFF);
            graphics.drawCenteredString(this.font, "§7Yeni fiyatı girin (TL):", centerX, 70, 0xAAAAAA);
        }

        if (currentTab == Tab.MAIN) {
            graphics.drawCenteredString(this.font, "§7Sadece OP oyuncular bu menüyü kullanabilir.", centerX, height - 30, 0xAAAAAA);
        }

        if (currentTab == Tab.ITEMS) {
            graphics.drawCenteredString(this.font, "§7Bir eşyaya tıklayarak Alış / Satış fiyatını düzenle", centerX, height - 25, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
