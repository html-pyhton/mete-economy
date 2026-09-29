package com.mete.economy.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent economy data stored as JSON in the world folder.
 * Works reliably on 1.21.11 without relying on SavedData API changes.
 */
public class EconomyState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EconomyState INSTANCE;

    public static class ItemPrice {
        public long buy;
        public long sell;
        public ItemPrice() {}
        public ItemPrice(long buy, long sell) {
            this.buy = Math.max(0, buy);
            this.sell = Math.max(0, sell);
        }
    }

    private final Map<String, Long> balances = new ConcurrentHashMap<>(); // uuid string -> amount
    private final Map<String, ItemPrice> itemPrices = new ConcurrentHashMap<>();
    private final Map<String, Long> enchantPricesPerLevel = new ConcurrentHashMap<>();
    private boolean economyEnabled = true;
    private boolean enchantSystemEnabled = true;
    private int maxEnchantLevel = 1000;

    private transient Path savePath;

    public EconomyState() {
        // defaults
        putItem("minecraft:dirt", 1, 1);
        putItem("minecraft:cobblestone", 2, 1);
        putItem("minecraft:stone", 2, 1);
        putItem("minecraft:iron_ingot", 25, 15);
        putItem("minecraft:diamond", 500, 350);
        putItem("minecraft:gold_ingot", 40, 25);
        putItem("minecraft:emerald", 100, 70);
        putItem("minecraft:netherite_ingot", 2000, 1400);
        putItem("minecraft:oak_log", 5, 3);
        putItem("minecraft:coal", 8, 5);
        putItem("minecraft:wheat", 3, 2);
        putItem("minecraft:carrot", 3, 2);
        putItem("minecraft:potato", 3, 2);
        putItem("minecraft:bread", 6, 4);
        putItem("minecraft:cooked_beef", 12, 8);
        putItem("minecraft:arrow", 2, 1);
        putItem("minecraft:string", 4, 2);
        putItem("minecraft:leather", 10, 6);
        putItem("minecraft:redstone", 15, 10);
        putItem("minecraft:lapis_lazuli", 12, 8);
        putItem("minecraft:quartz", 20, 12);
        putItem("minecraft:obsidian", 50, 30);
        putItem("minecraft:ender_pearl", 80, 50);
        putItem("minecraft:blaze_rod", 60, 40);
        putItem("minecraft:ghast_tear", 150, 100);
        putItem("minecraft:nether_star", 5000, 3500);

        enchantPricesPerLevel.put("minecraft:efficiency", 10L);
        enchantPricesPerLevel.put("minecraft:fortune", 25L);
        enchantPricesPerLevel.put("minecraft:sharpness", 15L);
        enchantPricesPerLevel.put("minecraft:protection", 12L);
        enchantPricesPerLevel.put("minecraft:unbreaking", 20L);
        enchantPricesPerLevel.put("minecraft:silk_touch", 100L);
        enchantPricesPerLevel.put("minecraft:mending", 150L);
        enchantPricesPerLevel.put("minecraft:power", 12L);
        enchantPricesPerLevel.put("minecraft:infinity", 200L);
    }

    private void putItem(String id, long buy, long sell) {
        itemPrices.put(id, new ItemPrice(buy, sell));
    }

    public static EconomyState get(MinecraftServer server) {
        if (INSTANCE == null) {
            INSTANCE = load(server);
        }
        return INSTANCE;
    }

    public static void reset() {
        INSTANCE = null;
    }

    private static Path getPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("mete_economy.json");
    }

    private static EconomyState load(MinecraftServer server) {
        Path path = getPath(server);
        EconomyState state = new EconomyState();
        state.savePath = path;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Type type = new TypeToken<EconomyState>(){}.getType();
                EconomyState loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    if (loaded.balances != null) state.balances.putAll(loaded.balances);
                    if (loaded.itemPrices != null) {
                        state.itemPrices.clear();
                        state.itemPrices.putAll(loaded.itemPrices);
                    }
                    if (loaded.enchantPricesPerLevel != null) {
                        state.enchantPricesPerLevel.clear();
                        state.enchantPricesPerLevel.putAll(loaded.enchantPricesPerLevel);
                    }
                    state.economyEnabled = loaded.economyEnabled;
                    state.enchantSystemEnabled = loaded.enchantSystemEnabled;
                    state.maxEnchantLevel = loaded.maxEnchantLevel;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    public void save() {
        if (savePath == null) return;
        try {
            Files.createDirectories(savePath.getParent());
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dirty() {
        save();
    }

    // ===== Balance =====
    public long getBalance(UUID uuid) {
        return balances.getOrDefault(uuid.toString(), 0L);
    }

    public void setBalance(UUID uuid, long amount) {
        balances.put(uuid.toString(), Math.max(0, amount));
        dirty();
    }

    public void addBalance(UUID uuid, long amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public boolean removeBalance(UUID uuid, long amount) {
        long cur = getBalance(uuid);
        if (cur < amount) return false;
        setBalance(uuid, cur - amount);
        return true;
    }

    // ===== Item prices =====
    public ItemPrice getItemPrice(String itemId) {
        return itemPrices.getOrDefault(itemId, new ItemPrice(0, 0));
    }

    public long getBuyPrice(String itemId) { return getItemPrice(itemId).buy; }
    public long getSellPrice(String itemId) { return getItemPrice(itemId).sell; }

    public void setItemPrice(String itemId, long buy, long sell) {
        itemPrices.put(itemId, new ItemPrice(buy, sell));
        dirty();
    }

    public Map<String, ItemPrice> getAllItemPrices() {
        return new HashMap<>(itemPrices);
    }

    // ===== Enchant =====
    public long getEnchantPricePerLevel(String id) {
        return enchantPricesPerLevel.getOrDefault(id, 10L);
    }

    public long getEnchantPrice(String id, int level) {
        return getEnchantPricePerLevel(id) * Math.max(1, level);
    }

    public void setEnchantPricePerLevel(String id, long price) {
        enchantPricesPerLevel.put(id, Math.max(0, price));
        dirty();
    }

    public Map<String, Long> getAllEnchantPrices() {
        return new HashMap<>(enchantPricesPerLevel);
    }

    // ===== Settings =====
    public boolean isEconomyEnabled() { return economyEnabled; }
    public void setEconomyEnabled(boolean v) { economyEnabled = v; dirty(); }
    public boolean isEnchantSystemEnabled() { return enchantSystemEnabled; }
    public void setEnchantSystemEnabled(boolean v) { enchantSystemEnabled = v; dirty(); }
    public int getMaxEnchantLevel() { return maxEnchantLevel; }
    public void setMaxEnchantLevel(int v) { maxEnchantLevel = Math.max(1, v); dirty(); }
}
