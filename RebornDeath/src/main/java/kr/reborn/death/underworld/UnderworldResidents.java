package kr.reborn.death.underworld;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 명계 체류자 명기(冥氣) 성장 + 명병/명수 처치 보상.
 * 명계 안에서만 동작.
 */
public final class UnderworldResidents implements Listener {

    private final RebornDeath plugin;

    public UnderworldResidents(RebornDeath p) {
        this.plugin = p;
        // 매 분 명계 체류자에게 명기 +1
        RebornCore.get().scheduler().runTimer(this::tickResidents, 1200L, 1200L);
    }

    private void tickResidents() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) continue;
            if (d.worldKey() != WorldKey.UNDERWORLD) continue;
            d.addStat(StatType.UNDERWORLD_KI, 1);
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (!killer.getWorld().getName().equalsIgnoreCase("underworld")) return;
        RebornCore.get().api().addStat(killer.getUniqueId(), StatType.UNDERWORLD_KI, 1, "underworld-kill");
        // 의뢰 진행 (collect_bone)
        try { plugin.underworldQuests().progress(killer, "collect_bone", 1); }
        catch (Throwable ignored) {}
    }
}
