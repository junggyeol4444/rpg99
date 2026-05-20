package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** 해양제국: 바다에서만 성장. 육지에서 시도 시 거부. */
public final class OceanGrowth implements GrowthStrategy {
    @Override public WorldKey world() { return WorldKey.OCEAN; }

    private boolean atSea(Player p) {
        Material m = p.getLocation().getBlock().getType();
        return m == Material.WATER || p.isInWater();
    }

    @Override public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        if (!atSea(p)) return;
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.OCEAN_POWER, 1.0, "sea-kill");
    }

    @Override public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.OCEAN_POWER, 5 * weight, "voyage");
    }

    @Override public void onMeditate(Player p, PlayerData d, double quality) {
        if (!atSea(p)) {
            Msg.warn(p, "해양력 수련은 바다 위에서만 가능합니다.");
            return;
        }
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.OCEAN_POWER, 4 * quality, "tide-meditate");
    }
}
