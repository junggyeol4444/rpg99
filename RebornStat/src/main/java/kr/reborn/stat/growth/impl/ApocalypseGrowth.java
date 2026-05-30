package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 아포칼립스 성장: 사냥보다 생존이 핵심.
 *
 * - 방사능 누적: 외부 호출 onRadiationExposure(player, dose)
 *     누적 100 초과 → 방사능 오염 저주 (RebornCurse hook)
 * - 식수/식량 카운트 (자급자족 = 보상): 외부 onConsumeWater/Food
 *     14일치 비축 = MENTAL +5, 음식 +3
 * - 변종 처치 = 보통 사냥 보상 + 음식 드롭 시뮬레이션
 * - 명상은 폐허에서 위험 — 30% 확률로 변종 출현 (PHANTOM 스폰)
 * - 의약품(MED_BAY) 위치 → 방사능 정화
 */
public final class ApocalypseGrowth implements GrowthStrategy {

    /** uuid → 방사능 누적량 (0~∞) */
    private final Map<UUID, Double> radiation = new ConcurrentHashMap<>();
    /** uuid → 비축 식수일 */
    private final Map<UUID, Integer> waterDays = new ConcurrentHashMap<>();
    /** uuid → 비축 식량일 */
    private final Map<UUID, Integer> foodDays = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.APOCALYPSE; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        StatType st = StatType.COMMON_8[Rand.range(0, 7)];
        RebornCore.get().api().addStat(p.getUniqueId(), st, 0.4, "survive-kill");
        // 변종 처치 시 방사능 약간 증가
        radiation.merge(p.getUniqueId(), 0.5, Double::sum);
        // 음식 드롭 (즉시 보충)
        if (Rand.chance(0.3)) {
            consumeFood(p, 1);
        }
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        StatType st = StatType.COMMON_8[Rand.range(0, 7)];
        RebornCore.get().api().addStat(p.getUniqueId(), st, 2 * weight, "quest");
        // 생존 퀘스트 보상 = 비축물자
        consumeFood(p, (int)(2 * weight));
        consumeWater(p, (int)(2 * weight));
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        // 폐허 세계 명상은 위험
        if (Rand.chance(0.3)) {
            spawnMutantAmbush(p);
        } else {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, quality, "rest");
        }
    }

    /** 외부 호출 — 방사능 피폭. */
    public void onRadiationExposure(Player p, double dose) {
        double cur = radiation.merge(p.getUniqueId(), dose, Double::sum);
        // 매 50마다 알림
        if (cur >= 50 && cur - dose < 50) {
            Msg.warn(p, "&2방사능 누적 50 — 방호복 권장.");
        }
        if (cur >= 100) {
            try {
                var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                if (cp != null) {
                    Object effects = cp.getClass().getMethod("effects").invoke(cp);
                    effects.getClass().getMethod("apply", Player.class, String.class)
                            .invoke(effects, p, "radiation");
                }
            } catch (Throwable ignored) {}
            // 50 차감 (저주 부여 후)
            radiation.put(p.getUniqueId(), cur - 50);
        }
    }

    /** 외부 호출 — 식수 비축. */
    public void consumeWater(Player p, int days) {
        int cur = waterDays.merge(p.getUniqueId(), days, Integer::sum);
        if (cur >= 14 && cur - days < 14) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 5, "14d-water");
            Msg.send(p, "&b식수 2주 비축 — 정신 +5");
        }
    }

    /** 외부 호출 — 식량 비축. */
    public void consumeFood(Player p, int days) {
        int cur = foodDays.merge(p.getUniqueId(), days, Integer::sum);
        if (cur >= 14 && cur - days < 14) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.ENDURANCE, 3, "14d-food");
            Msg.send(p, "&6식량 2주 비축 — 체력 +3");
        }
        try {
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + days));
        } catch (Throwable ignored) {}
    }

    /** MED_BAY 도달 시 정화. */
    public void onMedBayEnter(Player p) {
        double cur = radiation.getOrDefault(p.getUniqueId(), 0.0);
        if (cur > 0) {
            radiation.put(p.getUniqueId(), 0.0);
            Msg.send(p, "&a메드베이 — 방사능 " + (int)cur + " 제거.");
        }
        try { p.setHealth(p.getMaxHealth()); } catch (Throwable ignored) {}
    }

    private void spawnMutantAmbush(Player p) {
        try {
            p.getWorld().spawnEntity(p.getLocation().clone().add(5, 0, 5), EntityType.ZOMBIE);
            p.getWorld().spawnEntity(p.getLocation().clone().add(-5, 0, -5), EntityType.HUSK);
            p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation(), 50, 3, 1, 3);
        } catch (Throwable ignored) {}
        Msg.warn(p, "&c&l변종 습격! 잠든 사이 발견되었다.");
    }

    public double radiationOf(UUID p) { return radiation.getOrDefault(p, 0.0); }
    public int waterStockpileOf(UUID p) { return waterDays.getOrDefault(p, 0); }
    public int foodStockpileOf(UUID p) { return foodDays.getOrDefault(p, 0); }
}
