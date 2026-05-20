package kr.reborn.hiddenclass.listener;

import kr.reborn.core.event.RebornStatChangeEvent;
import kr.reborn.core.event.RebornTierUpEvent;
import kr.reborn.core.event.RebornWorldChangeEvent;
import kr.reborn.core.event.RebornQuestCompleteEvent;
import kr.reborn.hiddenclass.RebornHiddenClass;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 히든 클래스 해금 조건을 트리거하는 이벤트 리스너.
 * 성능을 위해 관련 이벤트가 발생할 때만 체크.
 */
public final class ConditionListener implements Listener {

    private final RebornHiddenClass plugin;

    public ConditionListener(RebornHiddenClass p) { this.plugin = p; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // 로그인 시 풀 체크 (누락 방지)
        plugin.engine().fullCheck(e.getPlayer());
    }

    @EventHandler
    public void onStatChange(RebornStatChangeEvent e) {
        // 스탯 변동 → STAT_MIN 조건 클래스 체크
        plugin.engine().fullCheck(e.getPlayer());
    }

    @EventHandler
    public void onTierUp(RebornTierUpEvent e) {
        // 경지 승급 → TIER_REACHED 체크
        plugin.engine().fullCheck(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(RebornWorldChangeEvent e) {
        // 세계 이동 → WORLDS_VISITED 체크
        plugin.engine().fullCheck(e.getPlayer());
    }

    @EventHandler
    public void onQuestComplete(RebornQuestCompleteEvent e) {
        plugin.progress().markQuestComplete(e.getPlayer().getUniqueId(), e.questId());
        plugin.engine().fullCheck(e.getPlayer());
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null) {
            plugin.progress().incKills(e.getEntity().getKiller().getUniqueId());
        }
    }
}
