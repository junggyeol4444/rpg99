package kr.reborn.core.data;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerDataListener implements Listener {

    private final DataManager dm;

    public PlayerDataListener(DataManager dm) { this.dm = dm; }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        // 비동기 환경에서 미리 캐싱
        dm.loadSync(e.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerData d = dm.getOrLoad(e.getPlayer().getUniqueId());
        if (d == null) return;
        d.name(e.getPlayer().getName());
        d.lastJoin(System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // 세션 playtime 누적 — 이전엔 어디서도 playtime을 갱신 안 해
        // HiddenClass의 PLAYTIME_MIN 조건이 영영 미달성 상태였음
        PlayerData d = dm.get(e.getPlayer().getUniqueId());
        if (d != null) {
            long session = System.currentTimeMillis() - d.lastJoin();
            if (session > 0) d.playtime(d.playtime() + session);
        }
        dm.unload(e.getPlayer().getUniqueId());
    }
}
