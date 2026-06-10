package kr.reborn.hiddenclass.listener;

import kr.reborn.hiddenclass.RebornHiddenClass;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 능력 트리거 리스너.
 *
 * - IMMORTAL_REVIVE: 사망 직전 데미지를 가로채 부활 시도.
 * - TIME_REWIND: 5초마다 위치·체력 스냅샷 (PlayerMoveEvent throttling).
 * - 환생 이벤트: INITIAL 클래스 굴림.
 */
public final class AbilityListener implements Listener {

    private final RebornHiddenClass plugin;

    public AbilityListener(RebornHiddenClass plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        // 치명타만 — finalDamage가 현재 HP 이상이면 사망
        if (e.getFinalDamage() < p.getHealth()) return;
        if (plugin.abilities().tryImmortalRevive(p)) {
            e.setCancelled(true);
        }
    }

    /** 플레이어별 throttle — 이전엔 단일 lastSnap 필드라
     *  한 명이 움직이면 모든 플레이어의 snapshot이 250ms 동안 차단됐음. */
    private final java.util.Map<java.util.UUID, Long> lastSnap = new java.util.concurrent.ConcurrentHashMap<>();
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        long now = System.currentTimeMillis();
        java.util.UUID id = e.getPlayer().getUniqueId();
        Long last = lastSnap.get(id);
        if (last != null && now - last < 250) return;
        lastSnap.put(id, now);
        plugin.abilities().snapshotRewind(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.abilities().snapshotRewind(e.getPlayer());
    }
}
