package kr.reborn.craft.enchant;

import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.craft.RebornCraft;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * 강화 시스템.
 *
 * 강화 레벨: 0 → 10
 * 성공률 (베이스, 각 레벨별):
 *   1~3: 90/80/70
 *   4~6: 60/50/40
 *   7~9: 30/20/10
 *   10:  5%
 *
 * 강화석 (config: enchant.stones)으로 성공률 가산.
 * 룬 (config: enchant.runes)으로 강화 시 추가 스탯 부여.
 *
 * 실패:
 *   1~6: 레벨 -1 (또는 유지)
 *   7~10: 아이템 파괴 위험 (등급별 fail-break-chance config)
 *
 * 강화 보너스: 레벨 N → 영구 STR/INT/ETC +N×3 (해당 아이템 장착 시 적용 — 외부 listener 필요)
 */
public final class EnchantSystem {

    private final RebornCraft plugin;
    private final NamespacedKey LEVEL_KEY;
    private final NamespacedKey RUNE_KEY;

    public EnchantSystem(RebornCraft plugin) {
        this.plugin = plugin;
        this.LEVEL_KEY = new NamespacedKey(plugin, "enchant_level");
        this.RUNE_KEY = new NamespacedKey(plugin, "rune_id");
    }

    /** 아이템의 현재 강화 레벨. 0 = 강화 안됨. */
    public int levelOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Integer lv = pdc.get(LEVEL_KEY, PersistentDataType.INTEGER);
        return lv == null ? 0 : lv;
    }

    public String runeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(RUNE_KEY, PersistentDataType.STRING);
    }

    /** 강화 시도. */
    public boolean tryEnchant(Player p, ItemStack item, String stoneId, String runeId) {
        if (item == null || item.getType() == Material.AIR) {
            Msg.error(p, "강화할 아이템 없음.");
            return false;
        }
        int curLv = levelOf(item);
        if (curLv >= 10) {
            Msg.error(p, "이미 +10 강화 (최대).");
            return false;
        }
        // 강화석은 필수 — 이전엔 stoneId null이면 비용 없이 시도 가능했음
        if (stoneId == null || stoneId.isEmpty()) {
            Msg.error(p, "강화석이 필요합니다 — /enchant try <stoneId> [rune]");
            return false;
        }
        double base = baseRate(curLv + 1);
        double stoneBonus = stoneBonus(stoneId);
        double total = Math.min(0.95, base + stoneBonus);

        // 강화석 소비
        if (!consumeStone(p, stoneId)) {
            Msg.error(p, "강화석 부족: " + stoneId);
            return false;
        }
        // 룬 소비 (성공 시만 적용)
        boolean haveRune = runeId != null;
        if (haveRune && countItem(p, Material.PAPER) < 1) {
            Msg.warn(p, "&7룬 새기기에 종이 1장 필요.");
            haveRune = false;
        }

        if (Rand.chance(total)) {
            applyLevel(item, curLv + 1);
            if (haveRune) {
                applyRune(item, runeId);
                removeItem(p, Material.PAPER, 1);
            }
            Msg.send(p, "&a&l[+ " + (curLv + 1) + "] §a강화 성공! §7(성공률 "
                    + (int)(total * 100) + "%)");
            updateLore(item);
            return true;
        }
        // 실패 처리
        return handleFailure(p, item, curLv);
    }

    private boolean handleFailure(Player p, ItemStack item, int curLv) {
        int nextLv = curLv + 1;
        if (nextLv <= 6) {
            // 단순 실패 — 레벨 유지
            Msg.warn(p, "&7강화 실패. 레벨 유지.");
        } else if (nextLv <= 9) {
            // 레벨 -1 또는 파괴
            double breakChance = 0.15;
            if (Rand.chance(breakChance)) {
                p.getInventory().setItemInMainHand(null);
                Msg.error(p, "&c&l강화 실패 — 아이템 파괴!");
                return false;
            }
            applyLevel(item, Math.max(0, curLv - 1));
            Msg.error(p, "&c강화 실패 — 레벨 -1 (현재 +" + Math.max(0, curLv - 1) + ")");
        } else {
            // +10 도전 실패 → 50% 파괴
            if (Rand.chance(0.5)) {
                p.getInventory().setItemInMainHand(null);
                Msg.error(p, "&4&l+10 실패 — 아이템 파괴!");
                return false;
            }
            applyLevel(item, 0);
            Msg.error(p, "&c+10 실패 — 강화 초기화 (+0)");
        }
        updateLore(item);
        return false;
    }

    private double baseRate(int targetLevel) {
        if (targetLevel <= 3) return 0.9 - (targetLevel - 1) * 0.10;
        if (targetLevel <= 6) return 0.6 - (targetLevel - 4) * 0.10;
        if (targetLevel <= 9) return 0.3 - (targetLevel - 7) * 0.10;
        return 0.05;
    }

    private double stoneBonus(String stoneId) {
        if (stoneId == null) return 0;
        var sec = plugin.getConfig().getConfigurationSection("enchant.stones." + stoneId);
        if (sec == null) return 0;
        // 둘 다 지원 — rate-bonus (0~1) 또는 bonus-percent (0~100 → /100)
        if (sec.contains("rate-bonus")) return sec.getDouble("rate-bonus");
        if (sec.contains("bonus-percent")) return sec.getDouble("bonus-percent") / 100.0;
        return 0;
    }

    private boolean consumeStone(Player p, String stoneId) {
        // stoneId → Material 매핑:
        //   1순위: config의 명시적 material
        //   2순위: stoneId 자체가 Material (예: "DIAMOND")
        //   3순위: grade별 기본 (config grade 필드 활용)
        Material mat = null;
        var sec = plugin.getConfig().getConfigurationSection("enchant.stones." + stoneId);
        if (sec != null && sec.isString("material")) {
            mat = Material.matchMaterial(sec.getString("material"));
        }
        if (mat == null) mat = Material.matchMaterial(stoneId.toUpperCase());
        if (mat == null && sec != null) {
            // grade로 fallback (시드된 stones와 일치)
            mat = stoneByGrade(sec.getString("grade", "COMMON"));
        }
        if (mat == null) mat = Material.PAPER;
        int amount = sec != null ? sec.getInt("amount", 1) : 1;
        if (countItem(p, mat) < amount) return false;
        removeItem(p, mat, amount);
        return true;
    }

    private Material stoneByGrade(String grade) {
        switch (grade.toUpperCase()) {
            case "COMMON": return Material.IRON_INGOT;
            case "UNCOMMON": return Material.IRON_BLOCK;
            case "RARE": return Material.GOLD_INGOT;
            case "HEROIC": return Material.DIAMOND;
            case "LEGENDARY": return Material.NETHERITE_INGOT;
            case "MYTHIC": return Material.NETHER_STAR;
            case "GENESIS": return Material.BEACON;
            default: return Material.PAPER;
        }
    }

    private void applyLevel(ItemStack item, int lv) {
        ItemMeta m = item.getItemMeta();
        if (m == null) return;
        m.getPersistentDataContainer().set(LEVEL_KEY, PersistentDataType.INTEGER, lv);
        item.setItemMeta(m);
    }

    private void applyRune(ItemStack item, String runeId) {
        ItemMeta m = item.getItemMeta();
        if (m == null) return;
        m.getPersistentDataContainer().set(RUNE_KEY, PersistentDataType.STRING, runeId);
        item.setItemMeta(m);
    }

    private void updateLore(ItemStack item) {
        ItemMeta m = item.getItemMeta();
        if (m == null) return;
        // 기존 lore 보존 — 강화/룬 prefix 줄만 제거하고 나머지(등급·스탯 등) 유지.
        // 이전 구현은 setLore(new ArrayList)로 통째 덮어써 CustomItem.render의
        // 등급·스탯 lore가 강화 시 영구 소실됐음.
        List<String> existing = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
        existing.removeIf(line -> {
            String stripped = ChatColor.stripColor(line);
            return stripped != null
                    && (stripped.startsWith("강화 +") || stripped.startsWith("장착 보너스 ")
                        || stripped.startsWith("룬: "));
        });
        int lv = levelOf(item);
        if (lv > 0) {
            String color = lv >= 10 ? "§6§l" : lv >= 7 ? "§5§l" : lv >= 4 ? "§a" : "§7";
            existing.add(color + "강화 +" + lv);
            existing.add("§7장착 보너스 §f+" + (lv * 3));
        }
        String rune = runeOf(item);
        if (rune != null) {
            existing.add("§b룬: §f" + rune);
        }
        m.setLore(existing);
        item.setItemMeta(m);
    }

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
