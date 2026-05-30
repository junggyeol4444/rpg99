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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사이버펑크 성장: 임플란트(개조)가 핵심.
 *
 * 임플란트 슬롯 8개 (HEAD, EYES, NEURAL, ARM_L, ARM_R, TORSO, LEGS, INTERNAL):
 *   - 슬롯당 1개 장착
 *   - 임플란트 수 1~3: 안정. 적응도/지능 보너스
 *   - 4개 이상: cybernetic_dependency 저주 자동 부여 (RebornCurse hook)
 *   - 7개 이상: cyber_psychosis 위험 → 매 분 5% 광폭화
 *
 * 장착 시: 영구 스탯 보정 + 슬롯 만료 시간 (수명).
 * 수명 다하면 자동 분리 + 위험 효과.
 *
 * 해킹 quality (onMeditate): 적응도 누적 + 데이터 칩 발견 확률.
 */
public final class CyberpunkGrowth implements GrowthStrategy {

    public enum Slot {
        HEAD, EYES, NEURAL, ARM_L, ARM_R, TORSO, LEGS, INTERNAL
    }

    public static final class Implant {
        public final Slot slot;
        public final String id;
        public final String name;
        public final long installedAt;
        public final long lifespanMs;
        public final Map<StatType, Double> bonus;

        public Implant(Slot slot, String id, String name, long lifespanMs, Map<StatType, Double> bonus) {
            this.slot = slot; this.id = id; this.name = name;
            this.installedAt = System.currentTimeMillis();
            this.lifespanMs = lifespanMs;
            this.bonus = bonus;
        }

        public boolean expired() {
            return lifespanMs > 0 && System.currentTimeMillis() - installedAt > lifespanMs;
        }
    }

    private final Map<UUID, Map<Slot, Implant>> implants = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.CYBERPUNK; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, 0.3, "kill");
        // 사이버 골렘 처치 시 임플란트 부품 드롭 (외부 mob plugin이 인벤 처리)
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.CYBER_ADAPTATION, 2 * weight, "augment");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.INTELLIGENCE, 1.5 * weight, "hack");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        // 해킹 자기교정
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.CYBER_ADAPTATION, quality * 2, "calibrate");
        // 데이터 칩 발견
        if (Rand.chance(0.05 * quality)) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.INTELLIGENCE, 5, "data-chip");
            Msg.send(p, "&b데이터 칩 발견 — 지능 +5");
        }
        // 만료 임플란트 자동 분리
        checkExpired(p);
    }

    /** 임플란트 장착. */
    public boolean install(Player p, Slot slot, String id, String name,
                           long lifespanMs, Map<StatType, Double> bonus) {
        Map<Slot, Implant> map = implants.computeIfAbsent(p.getUniqueId(), k -> new java.util.EnumMap<>(Slot.class));
        if (map.containsKey(slot)) {
            Msg.error(p, "이미 " + slot + " 슬롯에 임플란트 장착됨.");
            return false;
        }
        Implant imp = new Implant(slot, id, name, lifespanMs, bonus);
        map.put(slot, imp);
        // 보너스 즉시 적용
        for (var e : bonus.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(),
                    e.getValue(), "implant:" + id);
        }
        Msg.send(p, "&b임플란트 장착: §f" + name + " §7(" + slot + ")");
        checkDependency(p, map.size());
        return true;
    }

    /** 임플란트 분리. */
    public boolean remove(Player p, Slot slot) {
        Map<Slot, Implant> map = implants.get(p.getUniqueId());
        if (map == null) return false;
        Implant imp = map.remove(slot);
        if (imp == null) return false;
        // 보너스 회수
        for (var e : imp.bonus.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(),
                    -e.getValue(), "implant-remove:" + imp.id);
        }
        Msg.send(p, "&7임플란트 분리: " + imp.name);
        checkDependency(p, map.size());
        // 3개 이하로 줄면 cybernetic_dependency 해제 트리거
        if (map.size() <= 3) {
            try {
                var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                if (cp != null) {
                    Object cure = cp.getClass().getMethod("cure").invoke(cp);
                    cure.getClass().getMethod("tryCureMechanic", Player.class, String.class)
                            .invoke(cure, p, "cybernetics_count_max_3");
                }
            } catch (Throwable ignored) {}
        }
        return true;
    }

    private void checkExpired(Player p) {
        Map<Slot, Implant> map = implants.get(p.getUniqueId());
        if (map == null) return;
        List<Slot> toRemove = new ArrayList<>();
        for (var e : map.entrySet()) {
            if (e.getValue().expired()) toRemove.add(e.getKey());
        }
        for (Slot s : toRemove) {
            Implant imp = map.get(s);
            remove(p, s);
            Msg.warn(p, "&c임플란트 수명 만료: " + imp.name);
            // 만료 페널티: HP -5
            try { p.damage(5); } catch (Throwable ignored) {}
        }
    }

    private void checkDependency(Player p, int count) {
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp == null) return;
            Object effects = cp.getClass().getMethod("effects").invoke(cp);
            if (count >= 4) {
                effects.getClass().getMethod("apply", Player.class, String.class)
                        .invoke(effects, p, "cybernetic_dependency");
                if (count >= 7) {
                    effects.getClass().getMethod("apply", Player.class, String.class)
                            .invoke(effects, p, "cyber_psychosis");
                }
            }
        } catch (Throwable ignored) {}
    }

    public int implantCount(UUID p) {
        Map<Slot, Implant> map = implants.get(p);
        return map == null ? 0 : map.size();
    }

    public Map<Slot, Implant> implantsOf(UUID p) {
        return implants.getOrDefault(p, java.util.Collections.emptyMap());
    }
}
