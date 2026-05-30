package kr.reborn.craft.specialty;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.craft.RebornCraft;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 특화 제작 매니저.
 *
 * 12 SpecialtyType별 레시피 카탈로그 + 시도 → 성공률 계산 + 결과 적용.
 *
 * 성공률 = baseRate × (stat 충족 비율) × (숙련도 ÷ 100) × diff penalty
 * 결과:
 *   성공 → 결과 아이템 + 숙련도 +N + 이벤트 fire
 *   부분실패 (chance 30%) → 재료 절반 환불 + 숙련도 +1
 *   완전실패 → 재료 전부 손실 + 숙련도 +0.5
 */
public final class SpecialtyManager {

    private final RebornCraft plugin;
    private final Map<SpecialtyType, List<SpecialtyRecipe>> recipes = new EnumMap<>(SpecialtyType.class);
    /** uuid → SpecialtyType → 숙련도 (0~100) */
    private final Map<UUID, Map<SpecialtyType, Double>> proficiency = new ConcurrentHashMap<>();

    public SpecialtyManager(RebornCraft plugin) {
        this.plugin = plugin;
        seedRecipes();
    }

    private void seedRecipes() {
        // ── 연단 (ELIXIR_BREWING) ──
        add(new SpecialtyRecipe("great_return_pill", SpecialtyType.ELIXIR_BREWING, "대환단",
                "great_return_pill", Material.ENCHANTED_GOLDEN_APPLE, 1,
                5, 800, 500, 80));
        add(new SpecialtyRecipe("spirit_clear_pill", SpecialtyType.ELIXIR_BREWING, "단봉환",
                "spirit_clear_pill", Material.HONEY_BOTTLE, 1,
                3, 300, 200, 40));
        add(new SpecialtyRecipe("ice_heart_pill", SpecialtyType.ELIXIR_BREWING, "빙심단",
                "ice_heart_pill", Material.GHAST_TEAR, 1,
                4, 600, 400, 60));
        ingredients("great_return_pill", Material.NETHERITE_INGOT, 1, Material.GOLDEN_APPLE, 9, Material.GHAST_TEAR, 3);
        ingredients("spirit_clear_pill", Material.HONEY_BOTTLE, 5, Material.AMETHYST_SHARD, 2);
        ingredients("ice_heart_pill", Material.PACKED_ICE, 10, Material.GHAST_TEAR, 1);

        // ── 마도공학 ──
        add(new SpecialtyRecipe("basic_core", SpecialtyType.MAGITECH_FORGE, "기본 코어",
                "basic_core", Material.AMETHYST_SHARD, 1, 1, 100, 50, 10));
        add(new SpecialtyRecipe("magic_drone", SpecialtyType.MAGITECH_FORGE, "마법 드론",
                "magic_drone", Material.PHANTOM_MEMBRANE, 1, 3, 400, 300, 50));
        ingredients("basic_core", Material.AMETHYST_SHARD, 2, Material.REDSTONE, 3);
        ingredients("magic_drone", Material.PHANTOM_MEMBRANE, 2, Material.IRON_INGOT, 5, Material.REDSTONE, 10);

        // ── 사이버해킹 ──
        add(new SpecialtyRecipe("data_chip", SpecialtyType.CYBER_HACK, "데이터 칩",
                "data_chip", Material.QUARTZ, 1, 2, 200, 150, 20));
        add(new SpecialtyRecipe("hack_virus", SpecialtyType.CYBER_HACK, "바이러스",
                "hack_virus", Material.SCULK_SHRIEKER, 1, 4, 500, 400, 60));
        ingredients("data_chip", Material.QUARTZ, 1, Material.REDSTONE, 2);
        ingredients("hack_virus", Material.SCULK, 3, Material.ECHO_SHARD, 1);

        // ── 연금술 ──
        add(new SpecialtyRecipe("healing_potion", SpecialtyType.ALCHEMY, "치유 물약",
                "healing_potion", Material.POTION, 1, 1, 50, 30, 10));
        add(new SpecialtyRecipe("transmute_gold", SpecialtyType.ALCHEMY, "황금 변환",
                "transmute_gold", Material.GOLD_INGOT, 5, 4, 600, 500, 70));
        ingredients("healing_potion", Material.GLISTERING_MELON_SLICE, 1, Material.GLASS_BOTTLE, 1);
        ingredients("transmute_gold", Material.IRON_INGOT, 10, Material.LAPIS_LAZULI, 5);

        // ── 결정학 ──
        add(new SpecialtyRecipe("mana_crystal", SpecialtyType.CRYSTALLURGY, "마정석",
                "mana_crystal", Material.AMETHYST_BLOCK, 1, 2, 150, 200, 30));
        ingredients("mana_crystal", Material.AMETHYST_SHARD, 4, Material.LAPIS_LAZULI, 3);

        // ── 룬각 ──
        add(new SpecialtyRecipe("rune_attack", SpecialtyType.RUNE_INSCRIPTION, "공격 룬",
                "rune_attack", Material.PAPER, 1, 2, 200, 100, 25));
        add(new SpecialtyRecipe("rune_defense", SpecialtyType.RUNE_INSCRIPTION, "방어 룬",
                "rune_defense", Material.PAPER, 1, 2, 100, 200, 25));
        ingredients("rune_attack", Material.PAPER, 1, Material.REDSTONE, 5);
        ingredients("rune_defense", Material.PAPER, 1, Material.IRON_INGOT, 1);

        // ── 영조 ──
        add(new SpecialtyRecipe("soul_blade", SpecialtyType.SPIRIT_FORGING, "영혼 검",
                "soul_blade", Material.NETHERITE_SWORD, 1, 5, 700, 600, 80));
        ingredients("soul_blade", Material.NETHERITE_SWORD, 1, Material.GHAST_TEAR, 5, Material.NETHER_STAR, 1);

        // ── 단조 ──
        add(new SpecialtyRecipe("iron_sword_plus", SpecialtyType.METALWORKING, "강화 철검",
                "iron_sword_plus", Material.IRON_SWORD, 1, 1, 100, 50, 10));
        add(new SpecialtyRecipe("netherite_armor", SpecialtyType.METALWORKING, "네더라이트 갑옷",
                "netherite_armor", Material.NETHERITE_CHESTPLATE, 1, 4, 500, 400, 60));
        ingredients("iron_sword_plus", Material.IRON_INGOT, 3, Material.COAL, 2);
        ingredients("netherite_armor", Material.NETHERITE_INGOT, 4, Material.DIAMOND_CHESTPLATE, 1);

        // ── 직조 ──
        add(new SpecialtyRecipe("mage_robe", SpecialtyType.WEAVING, "마법사 로브",
                "mage_robe", Material.LEATHER_CHESTPLATE, 1, 2, 100, 200, 30));
        ingredients("mage_robe", Material.WHITE_WOOL, 10, Material.LAPIS_LAZULI, 5);

        // ── 보석 세공 ──
        add(new SpecialtyRecipe("dragon_gem", SpecialtyType.GEM_CUTTING, "용의 보석",
                "dragon_gem", Material.DIAMOND, 1, 4, 500, 100, 70));
        ingredients("dragon_gem", Material.DIAMOND, 3, Material.DRAGON_BREATH, 1);

        // ── 향수술 ──
        add(new SpecialtyRecipe("charm_perfume", SpecialtyType.PERFUMERY, "매혹 향수",
                "charm_perfume", Material.HONEY_BOTTLE, 1, 3, 150, 250, 40));
        ingredients("charm_perfume", Material.FLOWERING_AZALEA, 5, Material.HONEY_BOTTLE, 1);

        // ── 약학 ──
        add(new SpecialtyRecipe("anti_radiation", SpecialtyType.PHARMACOLOGY, "방사능 차단제",
                "anti_radiation", Material.MILK_BUCKET, 1, 3, 250, 150, 35));
        ingredients("anti_radiation", Material.MILK_BUCKET, 1, Material.IRON_INGOT, 2, Material.GHAST_TEAR, 1);
    }

    private void add(SpecialtyRecipe r) {
        recipes.computeIfAbsent(r.type, k -> new ArrayList<>()).add(r);
    }

    /** ingredients(id, mat1, amt1, mat2, amt2 …) — variadic helper. */
    private void ingredients(String recipeId, Object... pairs) {
        SpecialtyRecipe r = byId(recipeId);
        if (r == null) return;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            r.ingredients.put((Material) pairs[i], (Integer) pairs[i + 1]);
        }
    }

    private SpecialtyRecipe byId(String id) {
        for (var list : recipes.values()) {
            for (var r : list) if (r.id.equals(id)) return r;
        }
        return null;
    }

    public boolean tryCraft(Player p, String recipeId) {
        SpecialtyRecipe r = byId(recipeId);
        if (r == null) { Msg.error(p, "레시피 없음: " + recipeId); return false; }
        // 재료 검사
        for (var e : r.ingredients.entrySet()) {
            int have = countItem(p, e.getKey());
            if (have < e.getValue()) {
                Msg.error(p, "재료 부족: " + e.getKey() + " (필요 " + e.getValue() + ", 보유 " + have + ")");
                return false;
            }
        }
        double primarStat = RebornCore.get().api().getStat(p.getUniqueId(), r.type.primaryStat);
        double secondaryStat = RebornCore.get().api().getStat(p.getUniqueId(), r.type.secondaryStat);
        double prof = proficiency.getOrDefault(p.getUniqueId(), java.util.Collections.emptyMap())
                .getOrDefault(r.type, 0.0);

        double statRatio = Math.min(1.0,
                (primarStat / Math.max(1, r.requiredPrimaryStat) +
                 secondaryStat / Math.max(1, r.requiredSecondaryStat)) / 2);
        double profRatio = Math.min(1.5, prof / Math.max(1, r.requiredProficiency));
        double diffPenalty = 1.0 / (1 + (r.difficultyTier - 1) * 0.15);
        double rate = r.type.baseSuccessRate * statRatio * profRatio * diffPenalty;
        rate = Math.max(0.05, Math.min(0.95, rate));

        // 재료 소비 (성공/부분실패 처리)
        if (Rand.chance(rate)) {
            // 성공
            for (var e : r.ingredients.entrySet()) removeItem(p, e.getKey(), e.getValue());
            // 결과물 지급
            if (r.resultMaterial != null) {
                p.getInventory().addItem(new ItemStack(r.resultMaterial, r.resultAmount));
            }
            addProf(p, r.type, 2 + r.difficultyTier);
            Msg.send(p, "&a[" + r.type.koreanName + "] §6" + r.name + " §a제작 성공!");
            Bukkit.broadcastMessage("§e§l[" + r.type.koreanName + "] §f" + p.getName()
                    + " §7가 " + r.name + " 제작 성공! (Tier " + r.difficultyTier + ")");
            return true;
        }
        // 부분실패 (50%) — 재료 절반만 소비
        if (Rand.chance(0.5)) {
            for (var e : r.ingredients.entrySet()) {
                removeItem(p, e.getKey(), e.getValue() / 2);
            }
            addProf(p, r.type, 1);
            Msg.warn(p, "&7부분 실패 — 재료 절반만 손실.");
        } else {
            // 완전실패
            for (var e : r.ingredients.entrySet()) removeItem(p, e.getKey(), e.getValue());
            addProf(p, r.type, 0.5);
            Msg.error(p, "&c제작 실패 — 재료 손실. 숙련 미세 상승.");
        }
        return false;
    }

    public void addProf(Player p, SpecialtyType t, double v) {
        Map<SpecialtyType, Double> map = proficiency.computeIfAbsent(p.getUniqueId(),
                k -> new EnumMap<>(SpecialtyType.class));
        double cur = Math.min(100, map.getOrDefault(t, 0.0) + v);
        Double prev = map.put(t, cur);
        if (prev == null || (int) cur > (int) (double) prev) {
            Msg.send(p, "&e숙련 +1 (" + t.koreanName + ") §7Lv." + (int) cur);
        }
    }

    public double profOf(UUID p, SpecialtyType t) {
        return proficiency.getOrDefault(p, java.util.Collections.emptyMap()).getOrDefault(t, 0.0);
    }

    public List<SpecialtyRecipe> recipesOf(SpecialtyType t) {
        return recipes.getOrDefault(t, java.util.Collections.emptyList());
    }

    public Map<SpecialtyType, List<SpecialtyRecipe>> all() { return recipes; }

    private int countItem(Player p, Material m) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == m) n += it.getAmount();
        }
        return n;
    }

    private void removeItem(Player p, Material m, int n) {
        if (n <= 0) return;
        p.getInventory().removeItem(new ItemStack(m, n));
    }
}
