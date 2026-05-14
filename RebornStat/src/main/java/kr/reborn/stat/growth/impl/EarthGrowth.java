package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.entity.Player;

/** 지구: 레벨업 = +3 자유 스탯 (간이 구현, 1킬당 경험치 1로 누적) */
public final class EarthGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.EARTH; }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        double oldLv = d.getStat(StatType.LEVEL);
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.LEVEL, Math.max(0.5, mobLevel / 10.0), "kill-exp");
        double newLv = d.getStat(StatType.LEVEL);
        if (Math.floor(newLv / 10.0) > Math.floor(oldLv / 10.0)) {
            // 레벨 업 (10포인트당 1레벨)
            for (int i = 0; i < 3; i++) {
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, 1, "earth-levelup");
            }
            Msg.send(p, "&6레벨 업! +3 스탯");
        }
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.LEVEL, 5 * weight, "quest-exp");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        Msg.warn(p, "지구에서는 운기조식이 통하지 않는다.");
    }
}
