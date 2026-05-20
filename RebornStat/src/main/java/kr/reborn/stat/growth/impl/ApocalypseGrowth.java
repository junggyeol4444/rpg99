package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

public final class ApocalypseGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.APOCALYPSE; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        StatType st = StatType.COMMON_8[Rand.range(0, 7)];
        RebornCore.get().api().addStat(p.getUniqueId(), st, 0.4, "survive-kill");
    }
    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        StatType st = StatType.COMMON_8[Rand.range(0, 7)];
        RebornCore.get().api().addStat(p.getUniqueId(), st, 2 * weight, "quest");
    }
    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        // 폐허 세계 — 명상 따위 없다. 잠 들면 변종 습격
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, quality, "rest");
    }
}
