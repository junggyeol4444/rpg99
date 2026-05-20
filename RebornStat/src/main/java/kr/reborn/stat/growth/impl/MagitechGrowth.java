package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class MagitechGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.MAGITECH; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MAGITECH_ENERGY, 0.4, "kill");
    }
    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MAGITECH_ENERGY, 4 * weight, "research");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INTELLIGENCE, 1, "research");
    }
    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MAGITECH_ENERGY, 2 * quality, "tinker");
    }
}
