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
        public long installedAt;
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

    private static final String NS = "RebornStat.cyber";

    private final Map<UUID, Map<Slot, Implant>> implants = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static String encodeBonus(Map<StatType, Double> b) {
        StringBuilder sb = new StringBuilder();
        for (var e : b.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey().name()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    private static Map<StatType, Double> decodeBonus(String csv) {
        Map<StatType, Double> b = new java.util.EnumMap<>(StatType.class);
        if (csv == null || csv.isEmpty()) return b;
        for (String part : csv.split(",")) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                try { b.put(StatType.valueOf(kv[0]), Double.parseDouble(kv[1])); }
                catch (Throwable ignored) {}
            }
        }
        return b;
    }

    private void persistImplant(UUID p, Implant imp) {
        String encoded = imp.id + "|" + imp.name + "|" + imp.installedAt + "|"
                + imp.lifespanMs + "|" + encodeBonus(imp.bonus);
        RebornCore.get().kv().put(NS, p, imp.slot.name(), encoded);
    }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            Map<Slot, Implant> map = new java.util.EnumMap<>(Slot.class);
            var all = RebornCore.get().kv().loadAll(NS, p);
            for (var e : all.entrySet()) {
                try {
                    Slot slot = Slot.valueOf(e.getKey());
                    String[] parts = e.getValue().split("\\|", -1);
                    if (parts.length < 5) continue;
                    Implant imp = new Implant(slot, parts[0], parts[1],
                            Long.parseLong(parts[3]), decodeBonus(parts[4]));
                    imp.installedAt = Long.parseLong(parts[2]);
                    map.put(slot, imp);
                } catch (Throwable ignored) {}
            }
            if (!map.isEmpty()) implants.put(p, map);
        }
    }

    @Override public WorldKey world() { return WorldKey.CYBERPUNK; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, 0.3, "kill");
        // 사이버 골렘 처치 시 임플란트 부품 드롭 (외부 mob plugin이 인벤 처리)
        // 소속 코프를 위한 전투 — 후원 코프 평판 미세 누적 (tier 변경 시에만 알림).
        String patron = patronCorp(p.getUniqueId());
        if (patron != null) gainCorpFavor(p, patron, 1);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        double adaptation = 2 * weight;
        double intel = 1.5 * weight;
        // 후원 코프 동맹 단계면 개조 지원 — 적응도 보너스 (기획서 5-11 동맹 효과).
        String patron = patronCorp(p.getUniqueId());
        if (patron != null) {
            int tier = corpTier(corpReputation(p.getUniqueId(), patron));
            if (tier >= 4) {
                adaptation *= 1.20;  // 코프 후원: 개조 비용 지원
                if (Rand.chance(0.25)) {
                    Msg.send(p, "&b[" + patron + "] §7코프 후원 — 개조 적응 가속.");
                }
            }
            // 코프 의뢰 수행 — 평판 누적 (라이벌 코프는 자동 감소).
            gainCorpFavor(p, patron, (int) Math.round(8 * weight));
        }
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.CYBER_ADAPTATION, adaptation, "augment");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.INTELLIGENCE, intel, "hack");
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
        ensureLoaded(p.getUniqueId());
        Map<Slot, Implant> map = implants.computeIfAbsent(p.getUniqueId(), k -> new java.util.EnumMap<>(Slot.class));
        if (map.containsKey(slot)) {
            Msg.error(p, "이미 " + slot + " 슬롯에 임플란트 장착됨.");
            return false;
        }
        Implant imp = new Implant(slot, id, name, lifespanMs, bonus);
        map.put(slot, imp);
        persistImplant(p.getUniqueId(), imp);
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
        ensureLoaded(p.getUniqueId());
        Map<Slot, Implant> map = implants.get(p.getUniqueId());
        if (map == null) return false;
        Implant imp = map.remove(slot);
        if (imp == null) return false;
        RebornCore.get().kv().remove(NS, p.getUniqueId(), slot.name());
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
        ensureLoaded(p.getUniqueId());
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
        ensureLoaded(p);
        Map<Slot, Implant> map = implants.get(p);
        return map == null ? 0 : map.size();
    }

    public Map<Slot, Implant> implantsOf(UUID p) {
        ensureLoaded(p);
        return implants.getOrDefault(p, java.util.Collections.emptyMap());
    }

    // ───────────────────────── 7대 메가코프 평판 시스템 ─────────────────────────
    /**
     * 기획서 5-11: 7대 메가코프 (AKRO/JINTECH/HEXACORP/ARCANEWORKS/FROSTLINE/DRAGON_NEXUS/SOLARIS).
     * 각 코프와의 평판(-1000 ~ +1000):
     *   +500↑  Ally — 코프 상점 할인 30%, 전용 임플란트 접근
     *   +100↑  Friendly — 코프 상점 접근
     *   -200↓  Hostile — 코프 영역 진입 시 경비 발동
     *   -500↓  Enemy — 현상수배, 코프 전투 병기 추격
     *
     * 적대적 코프에 우호하면 — 라이벌 코프 평판 자동 감소.
     */
    public static final String[] CORPS = {
            "AKRO", "JINTECH", "HEXACORP", "ARCANEWORKS",
            "FROSTLINE", "DRAGON_NEXUS", "SOLARIS"
    };

    /** 코프별 라이벌 — 한쪽 우호 시 다른 쪽 감소. */
    private static final Map<String, String> CORP_RIVALS = Map.of(
            "AKRO", "JINTECH",            // 인체 vs AI
            "JINTECH", "AKRO",
            "HEXACORP", "ARCANEWORKS",     // 데이터 vs 군수
            "ARCANEWORKS", "HEXACORP",
            "FROSTLINE", "SOLARIS",        // 극지 vs 태양광
            "SOLARIS", "FROSTLINE",
            "DRAGON_NEXUS", "AKRO"         // 제조 vs 인체개조 (자체 부품)
    );

    private static final String CORP_NS = "RebornStat.cyberpunk.corp";

    public int corpReputation(UUID p, String corpId) {
        return RebornCore.get().kv().getInt(CORP_NS, p, corpId, 0);
    }

    public void gainCorpFavor(Player p, String corpId, int delta) {
        if (!isCorp(corpId)) return;
        int cur = corpReputation(p.getUniqueId(), corpId);
        int next = Math.max(-1000, Math.min(1000, cur + delta));
        RebornCore.get().kv().putInt(CORP_NS, p.getUniqueId(), corpId, next);
        // 라이벌 코프 평판 감소 (delta가 양수일 때만)
        if (delta > 0) {
            String rival = CORP_RIVALS.get(corpId);
            if (rival != null) {
                int rivalCur = corpReputation(p.getUniqueId(), rival);
                int rivalNext = Math.max(-1000, rivalCur - (delta / 2));
                RebornCore.get().kv().putInt(CORP_NS, p.getUniqueId(), rival, rivalNext);
            }
        }
        // 단계 변경 알림
        notifyStatusChange(p, corpId, cur, next);
    }

    private void notifyStatusChange(Player p, String corpId, int prev, int cur) {
        int prevTier = corpTier(prev);
        int curTier = corpTier(cur);
        if (prevTier == curTier) return;
        String[] labels = {"적", "적대", "냉랭", "중립", "우호", "동맹"};
        String label = labels[Math.max(0, Math.min(labels.length - 1, curTier))];
        Msg.send(p, "&b[" + corpId + "] §f관계: §6" + label + " §7(" + cur + ")");
        if (curTier >= 4) {
            Bukkit.broadcastMessage("§b§l[" + corpId + "] §f" + p.getName()
                    + " §7가 §6" + label + " §7관계 도달.");
        }
    }

    /** 평판 → 단계 (0=적, 5=동맹). */
    public int corpTier(int rep) {
        if (rep <= -500) return 0;
        if (rep <= -200) return 1;
        if (rep < 100) return 2;
        if (rep < 300) return 3;
        if (rep < 500) return 4;
        return 5;
    }

    public boolean isCorp(String id) {
        if (id == null) return false;
        for (String c : CORPS) if (c.equals(id)) return true;
        return false;
    }

    /** 코프 임무 완료 시 적용. type별 평판 가산. */
    public void onCorpMission(Player p, String corpId, String missionType) {
        int amount = switch (missionType) {
            case "minor"  -> 25;
            case "major"  -> 75;
            case "legend" -> 200;
            case "betray" -> -150;
            default       -> 10;
        };
        gainCorpFavor(p, corpId, amount);
    }

    public Map<String, Integer> allCorpReputations(UUID p) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (String c : CORPS) out.put(c, corpReputation(p, c));
        return out;
    }

    /**
     * 후원 코프 — 평판이 가장 높은(>0) 코프. /corp join 또는 의뢰로 형성.
     * 후원 코프가 있어야 전투·의뢰가 평판을 누적하고 동맹 단계 후원을 받는다.
     */
    public String patronCorp(UUID p) {
        String best = null;
        int bestRep = 0;
        for (String c : CORPS) {
            int rep = corpReputation(p, c);
            if (rep > bestRep) { bestRep = rep; best = c; }
        }
        return best;
    }
}
