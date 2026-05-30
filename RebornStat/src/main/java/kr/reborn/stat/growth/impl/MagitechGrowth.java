package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 마도공학 성장: 에너지 코어 채집·조합·과부하 위험.
 *
 * 코어 등급: BASIC(1) → STANDARD(3) → ADVANCED(10) → PRIME(30) → CHAOS(100)
 * 보유 코어 합산 = MAGITECH_ENERGY 영구 보너스
 * 과부하: 100 초과 시 매 분 5% magitech_overload 저주 위험
 * 조합: 같은 등급 3개 → 다음 등급 1개 (외부 craftCore 호출)
 */
public final class MagitechGrowth implements GrowthStrategy {

    public enum CoreTier {
        BASIC(1), STANDARD(3), ADVANCED(10), PRIME(30), CHAOS(100);
        public final int weight;
        CoreTier(int w) { this.weight = w; }
    }

    /** uuid → tier → count */
    private final Map<UUID, Map<CoreTier, Integer>> cores = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.MAGITECH; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.MAGITECH_ENERGY, 0.4, "kill");
        if (Rand.chance(0.10)) harvest(p, CoreTier.BASIC, 1);
        if (Rand.chance(0.02)) harvest(p, CoreTier.STANDARD, 1);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.MAGITECH_ENERGY, 4 * weight, "research");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.INTELLIGENCE, 1.0 * weight, "research");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.MAGITECH_ENERGY, 2 * quality, "tinker");
        checkOverload(p);
    }

    /** 외부 호출 — 코어 채집. */
    public void harvest(Player p, CoreTier tier, int amount) {
        Map<CoreTier, Integer> map = cores.computeIfAbsent(p.getUniqueId(),
                k -> new java.util.EnumMap<>(CoreTier.class));
        int cur = map.merge(tier, amount, Integer::sum);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.MAGITECH_ENERGY, tier.weight * amount, "core:" + tier);
        Msg.send(p, "&b" + tier + " 코어 +" + amount + " §7(총 " + cur + ")");
        checkOverload(p);
        if (tier == CoreTier.CHAOS && amount > 0) {
            Bukkit.broadcastMessage("§5§l[CHAOS 코어] §f" + p.getName()
                    + " §7가 카오스 코어를 얻었다 — 최고 등급!");
        }
    }

    /** 외부 호출 — 같은 등급 3개 → 다음 등급 1개. */
    public boolean craftCore(Player p, CoreTier tier) {
        if (tier == CoreTier.CHAOS) {
            Msg.error(p, "CHAOS 코어는 더 이상 합성 불가.");
            return false;
        }
        Map<CoreTier, Integer> map = cores.get(p.getUniqueId());
        if (map == null) return false;
        int cur = map.getOrDefault(tier, 0);
        if (cur < 3) {
            Msg.error(p, tier + " 코어 부족 (필요 3, 보유 " + cur + ")");
            return false;
        }
        map.put(tier, cur - 3);
        CoreTier next = CoreTier.values()[tier.ordinal() + 1];
        harvest(p, next, 1);
        return true;
    }

    private void checkOverload(Player p) {
        if (totalCoreWeight(p.getUniqueId()) > 100 && Rand.chance(0.05)) {
            try {
                var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                if (cp != null) {
                    Object effects = cp.getClass().getMethod("effects").invoke(cp);
                    effects.getClass().getMethod("apply", Player.class, String.class)
                            .invoke(effects, p, "magitech_overload");
                }
            } catch (Throwable ignored) {}
        }
    }

    public Map<CoreTier, Integer> coresOf(UUID p) {
        return cores.getOrDefault(p, java.util.Collections.emptyMap());
    }

    public int totalCoreWeight(UUID p) {
        Map<CoreTier, Integer> map = cores.get(p);
        if (map == null) return 0;
        int total = 0;
        for (var e : map.entrySet()) total += e.getKey().weight * e.getValue();
        return total;
    }
}
