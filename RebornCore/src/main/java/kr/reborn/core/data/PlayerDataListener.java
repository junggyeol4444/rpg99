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
        d.name(e.getPlayer().getName());
        d.lastJoin(System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dm.unload(e.getPlayer().getUniqueId());
    }
}
