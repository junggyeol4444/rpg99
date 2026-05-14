package kr.reborn.core.api;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.scheduler.RebornScheduler;
import kr.reborn.core.tier.Tier;
import kr.reborn.core.tier.TierManager;

import java.util.Set;
import java.util.UUID;

/**
 * 다른 모든 Reborn 플러그인이 호출하는 공용 API.
 * RebornCore.get().api()로 접근.
 */
public final class RebornAPI {

    private final RebornCore core;

    public RebornAPI(RebornCore core) { this.core = core; }

    public PlayerData getPlayerData(UUID uuid) {
        return core.dataManager().getOrLoad(uuid);
    }

    public double getStat(UUID uuid, StatType t) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        return d == null ? 0 : d.getStat(t);
    }

    public void setStat(UUID uuid, StatType t, double value) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        if (d != null) d.setStat(t, value);
    }

    public void addStat(UUID uuid, StatType t, double delta) {
        addStat(uuid, t, delta, "API");
    }

    public void addStat(UUID uuid, StatType t, double delta, String source) {
        core.dataManager().addStat(uuid, t, delta, source);
    }

    public double getTotalStats(UUID uuid) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        if (d == null) return 0;
        double sum = 0;
        for (StatType s : StatType.COMMON_8) sum += d.getStat(s);
        return sum;
    }

    public double getEffectiveTotalStats(UUID uuid) {
        // 축복/저주 보정은 RebornCurse가 hook으로 적용하므로 기본값은 동일.
        // RebornCurse 활성 시 PlayerData.statsView()가 이미 보정값.
        return getTotalStats(uuid);
    }

    public Tier getTier(UUID uuid) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        if (d == null) return null;
        TierManager tm = core.tierManager();
        return tm.resolveTier(getTotalStats(uuid), tm.defaultTable(d.worldKey()), d.dragonAge());
    }

    public boolean checkTierUp(UUID uuid) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        if (d == null) return false;
        Tier t = getTier(uuid);
        return t != null && !t.name.equals(d.tier());
    }

    public WorldKey getCurrentWorld(UUID uuid) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        return d == null ? WorldKey.LOBBY : d.worldKey();
    }

    public Set<WorldKey> getVisitedWorlds(UUID uuid) {
        PlayerData d = core.dataManager().getOrLoad(uuid);
        return d == null ? java.util.Collections.emptySet() : d.visited();
    }

    public RebornScheduler getScheduler() {
        return core.scheduler();
    }
}
