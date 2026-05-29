package kr.reborn.hiddenclass.listener;

import kr.reborn.core.event.RebornWorldChangeEvent;
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
        if (e.getFinalDamage() < p.getHealth()) return; // 죽지 않는 데미지는 무시
        if (plugin.abilities().tryImmortalRevive(p)) {
            e.setCancelled(true);
        }
    }

    private long lastSnap;
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        // throttle: 매 250ms 이내 1회
        long now = System.currentTimeMillis();
        if (now - lastSnap < 250) return;
        lastSnap = now;
        plugin.abilities().snapshotRewind(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.abilities().snapshotRewind(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(RebornWorldChangeEvent e) {
        // 새 월드 진입 — 해당 월드 INITIAL 클래스 굴림 (환생과 유사한 효과)
        try {
            plugin.engine().rollInitial(e.getPlayer(), e.to());
        } catch (Throwable ignored) {}
    }
}
