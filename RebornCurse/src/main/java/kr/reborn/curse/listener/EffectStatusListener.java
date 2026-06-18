package kr.reborn.curse.listener;

import kr.reborn.core.event.RebornNPCInteractEvent;
import kr.reborn.core.event.RebornQuestCompleteEvent;
import kr.reborn.core.event.RebornSkillLearnEvent;
import kr.reborn.curse.RebornCurse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 효과 상태 리스너 — 저주 해제 트리거 + 광폭화 PvP 면책 + 위치 기반 정화.
 */
public final class EffectStatusListener implements Listener {

    private final RebornCurse plugin;
    /** uuid → 마지막 위치 검사 시각 (1초당 1회로 throttle) */
    private final Map<UUID, Long> lastLocationCheck = new ConcurrentHashMap<>();

    public EffectStatusListener(RebornCurse plugin) { this.plugin = plugin; }

    /** 우클릭 아이템 — 약 또는 정화 도구 사용 시 cure 시도. */
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = e.getItem();
        if (hand == null || hand.getType() == Material.AIR) return;
        plugin.cure().tryCureWithItem(e.getPlayer(), hand);
    }

    /** NPC 상호작용 — 치료 NPC 또는 시전자가 dispel 발동. */
    @EventHandler
    public void onNpc(RebornNPCInteractEvent e) {
        plugin.cure().tryCureWithNpc(e.getPlayer(), e.npcId());
    }

    /** 퀘스트 완료 — 회개·정화 퀘스트. */
    @EventHandler
    public void onQuest(RebornQuestCompleteEvent e) {
        plugin.cure().tryCureWithQuest(e.getPlayer(), e.questId());
    }

    /** 스킬 학습/시전 — GREATER_SAGE_DISPEL 등. (Learn 이벤트를 cast 대용으로 사용) */
    @EventHandler
    public void onSkill(RebornSkillLearnEvent e) {
        plugin.cure().tryCureWithSkill(e.getPlayer(), e.skillId());
    }

    /** 위치 기반 cure — 1초당 1회만 체크. */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        long now = System.currentTimeMillis();
        long last = lastLocationCheck.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < 1000) return;
        lastLocationCheck.put(p.getUniqueId(), now);

        // 블록 종류 기반 cure 트리거 (단순화)
        Material below = p.getLocation().add(0, -1, 0).getBlock().getType();
        if (below == Material.GOLD_BLOCK || below == Material.EMERALD_BLOCK) {
            plugin.cure().tryCureAtLocation(p, "harmony_fountain");
        } else if (below == Material.IRON_BLOCK || below == Material.DIAMOND_BLOCK) {
            plugin.cure().tryCureAtLocation(p, "med_bay");
        } else if (below == Material.GLOWSTONE || below == Material.SEA_LANTERN) {
            plugin.cure().tryCureAtLocation(p, "light_altar");
        }
    }

    /** 광폭화 중인 공격자: 데미지 강화 + 표적이 같은 클랜원도 공격. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (plugin.berserk().isBerserk(attacker.getUniqueId())) {
            // PK 책임 감소 (외부 RebornStat이 조회할 수 있는 hint)
            // 실제 PK 시스템이 berserkEngine.pkReductionFor() 조회.
            e.setDamage(e.getDamage() * 1.5);
        }
    }
}
