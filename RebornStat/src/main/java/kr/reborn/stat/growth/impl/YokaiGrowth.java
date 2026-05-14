package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

/** 요계: 밤 3배, 낮 0.5배, 보름달 10배. */
public final class YokaiGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.YOKAI; }

    private double timeMult(Player p) {
        long t = p.getWorld().getTime();
        boolean night = t >= 13000 && t <= 23000;
        boolean fullMoon = p.getWorld().getFullTime() / 24000L % 8 == 0; // 간이 보름달
        var c = RebornStat.get().getConfig();
        if (fullMoon && night) return c.getDouble("growth.yokai.full-moon-mult", 10.0);
        return night ? c.getDouble("growth.yokai.night-mult", 3.0)
                     : c.getDouble("growth.yokai.day-mult", 0.5);
    }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.YOKAI_KI, 1.0 * timeMult(p), "yokai-kill");
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.YOKAI_KI, 5 * weight * timeMult(p), "quest");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.YOKAI_KI, 3 * quality * timeMult(p), "meditate");
    }
}
