package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class FantasyGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.FANTASY; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        StatType stat = StatType.COMMON_8[Rand.range(0, 7)];
        RebornCore.get().api().addStat(p.getUniqueId(), stat, 0.5 + Math.min(2.0, mobLevel / 20.0), "mob");
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INTELLIGENCE, 1.5 * weight, "quest");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MANA, 5 * weight, "quest");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MANA, 2 * quality, "meditate");
    }
}
