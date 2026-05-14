package kr.reborn.title.listener;

import kr.reborn.core.event.RebornTierUpEvent;
import kr.reborn.core.event.RebornWorldChangeEvent;
import kr.reborn.title.RebornTitle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class TitleProgressListener implements Listener {

    private final RebornTitle plugin;
    public TitleProgressListener(RebornTitle plugin) { this.plugin = plugin; }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        plugin.titles().incrementKill(killer);
    }

    @EventHandler
    public void onTier(RebornTierUpEvent e) {
        plugin.titles().onTier(e.getPlayer(), e.current());
    }

    @EventHandler
    public void onWorldChange(RebornWorldChangeEvent e) {
        plugin.titles().onWorldVisit(e.getPlayer());
    }
}
