package kr.reborn.curse.special;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.ActiveEffect;
import kr.reborn.curse.data.EffectDef;
import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * special·동적 효과 적용 엔진.
 *
 * tick마다 호출되며 다음을 처리:
 *   - hp_tick: HP 감소
 *   - out_of_ship_hp_drain: 해상 + 배 없음 검증 후 HP 감소
 *   - stats_tick_day_only: 낮 시간 (월드 시간 < 12000)에만 적용
 *   - lock_skill_school: 활성 캐싱 (외부 listener가 조회)
 *   - train_efficiency: 외부 학습 모듈이 조회
 *   - special: ELEMENT_SKILL_BOOST_50 등 명명된 효과 — 캐싱
 *   - npc_favor_tick: 주변 NPC 호의도 감소 (RebornNPC reflection)
 */
public final class SpecialEffectEngine {

    private final RebornCurse plugin;
    /** uuid → 봉인된 스킬 스쿨 집합 */
    private final Map<UUID, Set<String>> lockedSchools = new ConcurrentHashMap<>();
    /** uuid → 합산된 수련 효율 modifier (예: -0.8 = 80% 감소) */
    private final Map<UUID, Double> trainEfficiency = new ConcurrentHashMap<>();
    /** uuid → 명명된 special 목록 (예: ELEMENT_SKILL_BOOST_50) */
    private final Map<UUID, Set<String>> specials = new ConcurrentHashMap<>();

    public SpecialEffectEngine(RebornCurse plugin) {
        this.plugin = plugin;
    }

    /** 매 effects.tickAll() 안에서 def의 special 필드 처리. */
    public void applyTick(Player p, EffectDef def, ActiveEffect a) {
        UUID id = p.getUniqueId();

        // hp_tick
        if (def.hpTick != 0) {
            try {
                double cur = p.getHealth();
                double next = Math.max(0, Math.min(p.getMaxHealth(), cur + def.hpTick * a.stacks));
                if (next > 0) p.setHealth(next);
                else p.damage(1.0); // 0 이하 = 데미지 트리거
            } catch (Throwable ignored) {}
        }

        // out_of_ship_hp_drain
        if (def.outOfShipHpDrain != 0) {
            try {
                boolean inShip = p.getVehicle() instanceof Boat;
                boolean inWater = p.getLocation().getBlock().isLiquid()
                        || p.getLocation().add(0, -1, 0).getBlock().isLiquid();
                if (!inShip && inWater) {
                    p.damage(def.outOfShipHpDrain * a.stacks);
                }
            } catch (Throwable ignored) {}
        }

        // stats_tick_day_only
        if (!def.tickStatsDayOnly.isEmpty()) {
            long t = p.getWorld().getTime();
            if (t < 12000L) { // 낮
                for (var e : def.tickStatsDayOnly.entrySet()) {
                    try {
                        RebornCore.get().api().addStat(id, e.getKey(),
                                e.getValue() * a.stacks, "TICK_DAY:" + def.id);
                    } catch (Throwable ignored) {}
                }
            }
        }

        // npc_favor_tick — RebornNPC 리플렉션
        if (def.npcFavorTick != 0) {
            try {
                var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
                if (np != null) {
                    np.getClass().getMethod("nudgeNearbyFavor",
                                    Player.class, double.class, double.class)
                            .invoke(np, p, 30.0, def.npcFavorTick * a.stacks);
                }
            } catch (Throwable ignored) {}
        }
    }

    /** 효과가 적용/해제될 때 호출 — lock_skill_school, train_efficiency, special 캐시 갱신. */
    public void onApply(Player p, EffectDef def) {
        UUID id = p.getUniqueId();
        if (def.lockSkillSchool != null && !def.lockSkillSchool.isEmpty()) {
            lockedSchools.computeIfAbsent(id, k -> new HashSet<>()).add(def.lockSkillSchool);
        }
        if (def.trainEfficiency != 0) {
            trainEfficiency.merge(id, def.trainEfficiency, Double::sum);
        }
        if (def.special != null && !def.special.isEmpty()) {
            specials.computeIfAbsent(id, k -> new HashSet<>()).add(def.special);
        }
        // 영구 percent_stats 즉시 적용 외에 percent_stats가 음수인 저주 → MENTAL/CHARM 즉시 페널티 가시화
        if (def.percentStats.containsKey(StatType.SPIRIT_POWER)
                && def.percentStats.get(StatType.SPIRIT_POWER) < 0) {
            // 정령왕 계약 파기: 추가 시각 알림
            try { p.sendTitle("§5계약 파기", "§7정령왕은 더 이상 너를 보지 않는다", 10, 60, 20); }
            catch (Throwable ignored) {}
        }
    }

    public void onRemove(Player p, EffectDef def) {
        UUID id = p.getUniqueId();
        if (def.lockSkillSchool != null && !def.lockSkillSchool.isEmpty()) {
            Set<String> s = lockedSchools.get(id);
            if (s != null) s.remove(def.lockSkillSchool);
        }
        if (def.trainEfficiency != 0) {
            Double cur = trainEfficiency.get(id);
            if (cur != null) trainEfficiency.put(id, cur - def.trainEfficiency);
        }
        if (def.special != null && !def.special.isEmpty()) {
            Set<String> s = specials.get(id);
            if (s != null) s.remove(def.special);
        }
        // percent_stats 영구 효과는 되돌리지 않음 (cure_methods가 명시적으로 풀 때만 부분 복원)
    }

    /** 외부 listener가 조회 — 이 플레이어가 해당 스쿨 스킬을 시전할 수 있는지. */
    public boolean isSchoolLocked(UUID p, String school) {
        Set<String> s = lockedSchools.get(p);
        return s != null && s.contains(school);
    }

    /** 수련 효율 합산 모디파이어. */
    public double trainEfficiencyModifier(UUID p) {
        return trainEfficiency.getOrDefault(p, 0.0);
    }

    /** 명명된 special 보유 여부. */
    public boolean hasSpecial(UUID p, String specialId) {
        Set<String> s = specials.get(p);
        return s != null && s.contains(specialId);
    }

    public Map<UUID, Set<String>> lockedSchoolsView() { return lockedSchools; }
}
