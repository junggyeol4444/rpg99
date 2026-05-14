package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

/** 무협계: 몬스터 사냥 스탯 0. 운기조식·퀘스트·깨달음만. */
public final class MartialGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.MARTIAL; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        // 의도적으로 0. (혈마공 등 금기 무공 보유자만 흡성 콜백으로 별도 처리)
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INNER_KI, 3 * weight, "quest");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INNER_KI, 5 * quality, "meditate");
        if (quality > 0.9) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 0.5, "meditate-perfect");
        }
    }
}
