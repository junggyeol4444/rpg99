package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class HeavenGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.HEAVEN; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        // 천계는 신앙·임무·기도 중심, 사냥은 효율 매우 낮음
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.HEAVEN_KI, 0.1, "kill");
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.HEAVEN_KI, 6 * weight, "duty");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.HEAVEN_KI, 4 * quality, "prayer");
    }
}
