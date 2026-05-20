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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CraftingManager {

    private final RebornCraft plugin;
    private final Set<UUID> casting = new HashSet<>();

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
        // 재료 체크 + 차감
        for (Recipe.Mat m : r.materials) {
            if (!p.getInventory().contains(m.material, m.amount)) {
                Msg.error(p, "재료 부족: " + m.material + " x" + m.amount);
                return;
            }
        }
        for (Recipe.Mat m : r.materials) p.getInventory().removeItem(new ItemStack(m.material, m.amount));

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
            if (out != null) p.getInventory().addItem(plugin.items().render(out));
            plugin.proficiency().grantExp(p, r.profession, r.expGain);
            Bukkit.getPluginManager().callEvent(new RebornCraftSuccessEvent(p, r));
            Msg.send(p, "&a제작 성공!");
            // 상위 등급 확률
            if (Rand.chance(r.higherGradeChance) && out != null) {
                Msg.send(p, "&6&l[행운] 상위 등급 결과!");
                // TODO: 결과를 한 단계 위로 변환
            }
        } else {
            plugin.proficiency().grantExp(p, r.profession, r.expGain / 4);
            Bukkit.getPluginManager().callEvent(new RebornCraftFailEvent(p, r));
            Msg.error(p, "제작 실패. 부산물을 회수했다.");
        }
    }
}
