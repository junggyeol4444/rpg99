package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 정령계: 정수 흡수·원소 수련. 정신력 0이면 소멸. */
public final class SpiritGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.SPIRIT; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.SPIRIT_POWER, 0.3, "spirit-kill");
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.SPIRIT_POWER, 4 * weight, "quest");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.SPIRIT_POWER, 5 * quality, "meditate");
        // 정신력 0 체크
        if (d.getStat(StatType.MENTAL) <= 0) {
            Bukkit.getPluginManager().callEvent(
                    new RebornDeathEvent(p, p.getLocation(), null, "SPIRIT_VANISH"));
        }
    }
}
