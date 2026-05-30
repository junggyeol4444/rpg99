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
        double base = baseRate(curLv + 1);
        double stoneBonus = stoneBonus(stoneId);
        double total = Math.min(0.95, base + stoneBonus);

        // 강화석 소비 (있을 때만)
        if (stoneId != null && !consumeStone(p, stoneId)) {
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
        return sec.getDouble("rate-bonus", 0);
    }

    private boolean consumeStone(Player p, String stoneId) {
        // 강화석은 NAME_TAG로 가정 (간단화) — 정확한 매핑은 RebornCraft.ItemRegistry hook 필요.
        // 우선 PAPER 1개로 fallback (PoC)
        Material proxy = Material.PAPER;
        if (countItem(p, proxy) < 1) return false;
        removeItem(p, proxy, 1);
        return true;
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
        List<String> lore = new ArrayList<>();
        int lv = levelOf(item);
        if (lv > 0) {
            String color = lv >= 10 ? "§6§l" : lv >= 7 ? "§5§l" : lv >= 4 ? "§a" : "§7";
            lore.add(color + "강화 +" + lv);
            lore.add("§7장착 보너스 §f+" + (lv * 3));
        }
        String rune = runeOf(item);
        if (rune != null) {
            lore.add("§b룬: §f" + rune);
        }
        m.setLore(lore);
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
