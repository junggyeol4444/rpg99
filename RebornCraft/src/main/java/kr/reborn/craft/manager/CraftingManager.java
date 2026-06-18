package kr.reborn.craft.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import kr.reborn.craft.data.Recipe;
import kr.reborn.craft.event.RebornCraftFailEvent;
import kr.reborn.craft.event.RebornCraftSuccessEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CraftingManager {

    private final RebornCraft plugin;
    /** 제작 중 플레이어 — 다중 플레이어 동시 이벤트 안전. */
    private final Set<UUID> casting = ConcurrentHashMap.newKeySet();

    public CraftingManager(RebornCraft plugin) { this.plugin = plugin; }

    public void open(Player p) {
        var b = plugin.gui().builder("&6제작 GUI", 6);
        int slot = 0;
        for (Recipe r : plugin.recipes().all()) {
            if (slot >= 54) break;
            CustomItem result = plugin.items().get(r.resultItemId);
            ItemStack icon = result != null ? plugin.items().render(result)
                    : Items.of(Material.STONE, "&7" + r.resultItemId);
            String prof = r.profession;
            int min = r.minProficiency;
            b.set(slot++, icon, e -> tryCraft(p, r));
        }
        b.open(p);
    }

    public void tryCraft(Player p, Recipe r) {
        if (casting.contains(p.getUniqueId())) {
            Msg.warn(p, "이미 제작 중입니다.");
            return;
        }
        // 직업·숙련도 체크
        if (!plugin.proficiency().learn(p.getUniqueId(), r.profession)) {
            Msg.error(p, "다른 직업으로 가득 찼습니다.");
            return;
        }
        int exp = plugin.proficiency().exp(p.getUniqueId(), r.profession);
        if (exp < r.minProficiency) {
            Msg.error(p, "숙련도 부족 (필요: " + r.minProficiency + ", 보유: " + exp + ")");
            return;
        }
        // 재료 체크 + 차감 — ALL_RECIPES_30_PCT_DISCOUNT passive 시 30% 감액 (최소 1개).
        boolean discount = hasHiddenPassive(p, "ALL_RECIPES_30_PCT_DISCOUNT");
        java.util.List<Recipe.Mat> effective = new java.util.ArrayList<>();
        for (Recipe.Mat m : r.materials) {
            int needed = discount ? Math.max(1, (int) Math.ceil(m.amount * 0.70)) : m.amount;
            effective.add(new Recipe.Mat(m.material, needed));
            if (!p.getInventory().contains(m.material, needed)) {
                Msg.error(p, "재료 부족: " + m.material + " x" + needed
                        + (discount ? " §7(30% 할인 적용)" : ""));
                return;
            }
        }
        for (Recipe.Mat m : effective) p.getInventory().removeItem(new ItemStack(m.material, m.amount));

        casting.add(p.getUniqueId());
        Msg.send(p, "&e제작 시작... (" + r.castSeconds + "초)");
        p.closeInventory();

        RebornCore.get().scheduler().runTaskLater(() -> finalizeCraft(p, r), r.castSeconds * 20L);
    }

    private void finalizeCraft(Player p, Recipe r) {
        casting.remove(p.getUniqueId());
        if (!p.isOnline()) return;
        // 숙련도 보정
        int exp = plugin.proficiency().exp(p.getUniqueId(), r.profession);
        double rate = Math.min(0.99, r.successRate + Math.min(0.3, exp / 50000.0));
        if (Rand.chance(rate)) {
            CustomItem out = plugin.items().get(r.resultItemId);
            if (out != null) addOrDrop(p, plugin.items().render(out));
            plugin.proficiency().grantExp(p, r.profession, r.expGain);
            Bukkit.getPluginManager().callEvent(new RebornCraftSuccessEvent(p, r));
            Msg.send(p, "&a제작 성공!");
            // 상위 등급 확률 — 보너스 1개 추가 (상위 변형 id 명명 컨벤션 없으므로 동일 아이템 +1)
            if (Rand.chance(r.higherGradeChance) && out != null) {
                Msg.send(p, "&6&l[행운] 상위 등급 결과 — 추가 1개!");
                addOrDrop(p, plugin.items().render(out));
            }
        } else {
            plugin.proficiency().grantExp(p, r.profession, r.expGain / 4);
            Bukkit.getPluginManager().callEvent(new RebornCraftFailEvent(p, r));
            Msg.error(p, "제작 실패. 부산물을 회수했다.");
        }
    }

    /** 인벤이 가득 차서 addItem 실패하면 발 밑에 드롭 — 결과물 분실 방지. */
    private void addOrDrop(Player p, ItemStack item) {
        var leftover = p.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack it : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), it);
            }
            Msg.warn(p, "&7인벤 가득 — 발 밑에 떨궈 두었다.");
        }
    }

    /** RebornHiddenClass.passives().has(uuid, flag) 리플렉션. 미존재 plugin이면 false. */
    private boolean hasHiddenPassive(Player p, String flag) {
        try {
            var hc = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornHiddenClass");
            if (hc == null) return false;
            Object pe = hc.getClass().getMethod("passives").invoke(hc);
            if (pe == null) return false;
            Object res = pe.getClass().getMethod("has",
                    java.util.UUID.class, String.class).invoke(pe, p.getUniqueId(), flag);
            return Boolean.TRUE.equals(res);
        } catch (Throwable ignored) {}
        return false;
    }
}
