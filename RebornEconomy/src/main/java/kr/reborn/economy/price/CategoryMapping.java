package kr.reborn.economy.price;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

/**
 * 아이템 → 경제 카테고리 매핑. MarketSimulator의 Category와 매칭되어
 * 세계별 가격 변동이 어느 아이템에 적용될지 결정.
 *
 * 카테고리: FOOD, METAL, MAGIC, WEAPON, ARMOR, RARE, INFO, MEDICINE
 */
public final class CategoryMapping {

    private static final Map<Material, String> M = new EnumMap<>(Material.class);

    static {
        // FOOD
        for (Material m : new Material[]{
                Material.BREAD, Material.APPLE, Material.GOLDEN_APPLE, Material.COOKED_BEEF,
                Material.COOKED_CHICKEN, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
                Material.COOKED_SALMON, Material.COOKED_COD, Material.COOKIE, Material.MELON_SLICE,
                Material.MUSHROOM_STEW, Material.PUMPKIN_PIE, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW,
                Material.WHEAT, Material.CARROT, Material.POTATO, Material.BAKED_POTATO, Material.BEETROOT,
                Material.HONEY_BOTTLE, Material.MILK_BUCKET, Material.CAKE
        }) M.put(m, "FOOD");

        // METAL
        for (Material m : new Material[]{
                Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT, Material.NETHERITE_INGOT,
                Material.IRON_NUGGET, Material.GOLD_NUGGET, Material.IRON_BLOCK, Material.GOLD_BLOCK,
                Material.NETHERITE_BLOCK, Material.RAW_IRON, Material.RAW_GOLD, Material.RAW_COPPER
        }) M.put(m, "METAL");

        // MAGIC
        for (Material m : new Material[]{
                Material.BLAZE_POWDER, Material.BLAZE_ROD, Material.ENDER_PEARL, Material.ENDER_EYE,
                Material.GHAST_TEAR, Material.NETHER_STAR, Material.DRAGON_BREATH, Material.DRAGON_EGG,
                Material.AMETHYST_SHARD, Material.GLOWSTONE_DUST, Material.REDSTONE,
                Material.LAPIS_LAZULI, Material.ENCHANTED_BOOK, Material.EXPERIENCE_BOTTLE,
                Material.PHANTOM_MEMBRANE, Material.PRISMARINE_CRYSTALS, Material.NAUTILUS_SHELL
        }) M.put(m, "MAGIC");

        // WEAPON
        for (Material m : new Material[]{
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE,
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
                Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW
        }) M.put(m, "WEAPON");

        // ARMOR
        for (Material m : new Material[]{
                Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
                Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
                Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
                Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
                Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
                Material.SHIELD, Material.ELYTRA, Material.TURTLE_HELMET
        }) M.put(m, "ARMOR");

        // RARE
        for (Material m : new Material[]{
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT,
                Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING, Material.NETHER_STAR,
                Material.DRAGON_EGG, Material.HEART_OF_THE_SEA, Material.BEACON,
                Material.CONDUIT
        }) M.put(m, "RARE");

        // MEDICINE
        for (Material m : new Material[]{
                Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
                Material.GLISTERING_MELON_SLICE
        }) M.put(m, "MEDICINE");

        // INFO
        for (Material m : new Material[]{
                Material.WRITTEN_BOOK, Material.BOOK, Material.WRITABLE_BOOK,
                Material.MAP, Material.FILLED_MAP, Material.PAPER,
                Material.RECOVERY_COMPASS, Material.COMPASS, Material.CLOCK
        }) M.put(m, "INFO");
    }

    public static String of(Material m) {
        return M.getOrDefault(m, "INFO"); // 분류되지 않은 아이템은 INFO로
    }
}
