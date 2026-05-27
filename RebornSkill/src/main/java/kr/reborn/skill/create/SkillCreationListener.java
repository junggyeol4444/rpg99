package kr.reborn.skill.create;

import kr.reborn.skill.RebornSkill;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 플레이어 행동 패턴을 SkillCreator에 공급.
 * 무기 종류별 반복 사냥·은신 처치 등을 누적하여 스킬 창조의 재료로 삼는다.
 */
public final class SkillCreationListener implements Listener {

    private final RebornSkill plugin;

    public SkillCreationListener(RebornSkill p) { this.plugin = p; }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        String key = killer.isSneaking()
                ? "stealth_kill"
                : "weapon_" + weaponCat(killer.getInventory().getItemInMainHand());
        plugin.creator().log(killer, key);
    }

    private String weaponCat(ItemStack item) {
        if (item == null || item.getType().isAir()) return "fist";
        String n = item.getType().name();
        if (n.endsWith("_SWORD")) return "sword";
        if (n.endsWith("_AXE")) return "axe";
        if (n.equals("BOW") || n.equals("CROSSBOW")) return "bow";
        if (n.equals("TRIDENT")) return "trident";
        return "sword";  // 기타 무기는 검 계열
    }
}
