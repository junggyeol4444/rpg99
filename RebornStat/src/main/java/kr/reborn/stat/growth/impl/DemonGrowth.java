package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

/** 마계: 마기·영혼 흡수. 마기 30%+제어 필요. */
public final class DemonGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.DEMON; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DEMON_KI, 1.0, "soul-absorb");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, 0.3, "kill");
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DEMON_KI, 4 * weight, "quest");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DEMON_KI, 3 * quality, "meditate");
    }
}
