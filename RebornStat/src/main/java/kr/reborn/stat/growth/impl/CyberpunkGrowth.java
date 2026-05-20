package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class CyberpunkGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.CYBERPUNK; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, 0.3, "kill");
    }
    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.CYBER_ADAPTATION, 2 * weight, "augment");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INTELLIGENCE, 1.5 * weight, "hack");
    }
    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.CYBER_ADAPTATION, quality, "calibrate");
    }
}
