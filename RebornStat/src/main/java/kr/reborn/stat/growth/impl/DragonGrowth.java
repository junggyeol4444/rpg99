package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class DragonGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.DRAGON; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, 0.8, "dragon-kill");
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, 6 * weight, "quest");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        // 잠 = 회복 + 성장 (타임 시스템에서 호출)
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, 4 * quality, "sleep");
    }
}
