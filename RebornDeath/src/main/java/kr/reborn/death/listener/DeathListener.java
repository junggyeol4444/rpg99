package kr.reborn.death.listener;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class DeathListener implements Listener {

    private final RebornDeath plugin;

    public DeathListener(RebornDeath p) { this.plugin = p; }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Player killer = e.getEntity().getKiller();
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());

        Bukkit.getPluginManager().callEvent(
                new RebornDeathEvent(p, p.getLocation(), killer, killer == null ? "PVE" : "PVP"));

        d.deaths(d.deaths() + 1);
        // 아이템은 사망 지점에 자연스럽게 드랍됨 (바닐라 동작 유지)

        // 명계 이동
        plugin.underworld().sendToUnderworld(p);

        // PvP면 범죄 처리 + 현상금 지급
        if (killer != null) {
            plugin.crime().onPvpKill(killer, p);
            plugin.bounty().onKilled(killer, p.getUniqueId());
        }
        // 보험금 자동 지급 (RebornEconomy 리플렉션)
        try {
            var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep != null) {
                Object ins = ep.getClass().getMethod("insurance").invoke(ep);
                if (ins != null) {
                    ins.getClass().getMethod("payoutOnDeath", Player.class)
                            .invoke(ins, p);
                }
            }
        } catch (Throwable ignored) {}
    }
}
