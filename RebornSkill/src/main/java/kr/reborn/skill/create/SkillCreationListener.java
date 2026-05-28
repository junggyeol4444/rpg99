package kr.reborn.skill.create;

import kr.reborn.skill.RebornSkill;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 플레이어 행동 패턴을 SkillCreator에 공급.
 * 사냥·은신·채굴·제작·사선 회피 등 다양한 반복/특이 행동을 추적해 스킬 창조 재료로.
 */
public final class SkillCreationListener implements Listener {

    private final RebornSkill plugin;

    public SkillCreationListener(RebornSkill p) { this.plugin = p; }

    /** 사냥 — 무기 종류별 + 은신 기습. */
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        String key = killer.isSneaking()
                ? "stealth_kill"
                : "weapon_" + weaponCat(killer.getInventory().getItemInMainHand());
        plugin.creator().log(killer, key);
    }

    /** 채굴 — 도구 종류별. */
    @EventHandler
    public void onMine(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir()) return;
        String t = tool.getType().name();
        String cat;
        if (t.endsWith("_PICKAXE")) cat = "pickaxe";
        else if (t.endsWith("_AXE")) cat = "axe_mine";
        else if (t.endsWith("_SHOVEL")) cat = "shovel";
        else return;  // 손/기타는 추적 안 함
        plugin.creator().log(p, "mine_" + cat);
    }

    /** 제작 — 결과 카테고리별. */
    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack result = e.getRecipe().getResult();
        if (result == null) return;
        String n = result.getType().name();
        String cat;
        if (n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE")
                || n.endsWith("_SHOVEL") || n.endsWith("_HOE") || n.equals("BOW")) cat = "weapon";
        else if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") || n.endsWith("_SHIELD")) cat = "armor";
        else if (n.endsWith("_POTION") || n.equals("POTION") || n.equals("SPLASH_POTION")) cat = "potion";
        else return;  // 기타는 추적 안 함
        plugin.creator().log(p, "craft_" + cat);
    }

    /** 사선 생존 — 저HP에서 큰 피해를 견디면 방어 스킬 단서. */
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getFinalDamage() < 6) return;
        // 큰 피해를 맞고도 1HP 이상 남으면 "사선 회피" 단서
        if (p.getHealth() - e.getFinalDamage() < 2 && p.getHealth() - e.getFinalDamage() > 0) {
            plugin.creator().log(p, "survive_brink");
        }
    }

    private String weaponCat(ItemStack item) {
        if (item == null || item.getType().isAir()) return "fist";
        String n = item.getType().name();
        if (n.endsWith("_SWORD")) return "sword";
        if (n.endsWith("_AXE")) return "axe";
        if (n.equals("BOW") || n.equals("CROSSBOW")) return "bow";
        if (n.equals("TRIDENT")) return "trident";
        return "sword";
    }
}
