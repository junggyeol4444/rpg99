package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class ImmortalGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.IMMORTAL; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 0.5, "kill");
    }
    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 5 * weight, "quest");
    }
    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 6 * quality, "meditate");
    }
}
