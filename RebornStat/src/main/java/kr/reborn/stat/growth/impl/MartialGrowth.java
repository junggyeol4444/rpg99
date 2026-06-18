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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 무협계 성장: 사냥 무의미, 운기조식·깨달음·단약·비급·진법이 핵심.
 *
 * 단약 효과: 대환단=내공 +500, 단봉환=정신 +20, 빙심단=주화입마 해제·내공 +200
 * 운기조식 quality:
 *   0.9+ → 깨달음 5% 확률 → 정신 +5, 내공 ×1.5 일시
 *   0.5+ → 보통 회복
 *   <0.5 → 주화입마 1% 확률 (RebornCurse hook)
 * 비급 깨달음: onSkillUse 호출 시 누적 → 일정량 후 무공 단계 상승
 * 진법: 다인 합동 수련 시 +20% boost (외부 호출)
 *
 * 정파/사파/마교 분기는 클랜 모드에 따라 달라짐:
 *   정파(orthodox): 깨달음 ×1.5, 단약 효과 ×1
 *   사파(unorthodox): 깨달음 ×1, 약탈 시 추가 내공
 *   마교(cult): 깨달음 ×0.7, 살생 시 마기 추가 (DEMON_KI)
 */
public final class MartialGrowth implements GrowthStrategy {

    private static final String NS = "RebornStat.martial";

    /** 각 플레이어가 누적한 깨달음 점수 — 100마다 한 단계 무공 진척. */
    private final Map<UUID, Double> enlightenment = new ConcurrentHashMap<>();
    /** 깨달음 단계 (각 100점 누적마다 +1) */
    private final Map<UUID, Integer> enlightenLevel = new ConcurrentHashMap<>();
    /** 단약 사용 카운트 — 같은 단약 남용 시 부작용 */
    private final Map<UUID, Map<String, Integer>> pillUsage = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            enlightenment.put(p, RebornCore.get().kv().getDouble(NS, p, "enlightProgress", 0.0));
            enlightenLevel.put(p, RebornCore.get().kv().getInt(NS, p, "enlightLevel", 0));
            // pillUsage: pill.<id> 키들을 모두 로드
            var all = RebornCore.get().kv().loadAll(NS, p);
            Map<String, Integer> usage = new HashMap<>();
            for (var e : all.entrySet()) {
                if (e.getKey().startsWith("pill.")) {
                    try { usage.put(e.getKey().substring(5), Integer.parseInt(e.getValue())); }
                    catch (Throwable ignored) {}
                }
            }
            pillUsage.put(p, usage);
        }
    }

    @Override public WorldKey world() { return WorldKey.MARTIAL; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        // 기획서 5-5: 무협 분기별 살생 효과 분리
        String clanType = clanType(d);
        switch (clanType == null ? "" : clanType) {
            case "cult" -> {
                // 마교: 살생 = 마기 흡수 (DEMON_KI)
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.DEMON_KI, 0.5, "martial-kill-cult");
                // 마교는 정신력에 부담 (심마 위험)
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MENTAL, -0.05, "martial-kill-cult");
            }
            case "unorthodox" -> {
                // 사파: 약탈/살생 = 내공 추가 + 잔인함 보정 (정신 약간 -)
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.INNER_KI, 0.3, "martial-kill-sapa");
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.STRENGTH, 0.1, "martial-kill-sapa");
            }
            case "orthodox" -> {
                // 정파: 의선/포두 — 살생 금기 (정신 -, 도력 -)
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MENTAL, -0.1, "martial-kill-orthodox");
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.TAO_POWER, -0.05, "martial-kill-orthodox");
            }
            default -> {
                // 미정파: 매우 미미한 내공 (기본 무인 가정)
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.INNER_KI, 0.1, "martial-kill-none");
            }
        }
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INNER_KI, 3 * weight, "quest");
        // 도술 수련자 — 도력도 누적 (기획서 5-5: "무공과 도술 둘 다 수련 가능")
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.TAO_POWER, 1.5 * weight, "quest-tao");
        // 깨달음 약간 누적
        gainEnlightenment(p, 1.0 * weight);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        double mult = clanMultiplier(d);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.INNER_KI, 5 * quality * mult, "meditate");
        // 도력 동시 누적 (도술 수련자 대비. 내공의 60% 비율)
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.TAO_POWER, 3 * quality * mult, "meditate-tao");
        // 운기 결과에 따라 분기
        if (quality >= 0.9) {
            // 깨달음 5% 확률
            if (Rand.chance(0.05)) {
                triggerEnlightenment(p);
            } else {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MENTAL, 0.5, "meditate-perfect");
                gainEnlightenment(p, 3 * mult);
            }
        } else if (quality < 0.5) {
            // 주화입마 1% 확률
            if (Rand.chance(0.01)) {
                triggerQiDeviation(p);
            } else {
                gainEnlightenment(p, 0.5);
            }
        } else {
            gainEnlightenment(p, 1.5 * mult);
        }
    }

    /** 외부 호출 — 비급/초식 사용 시 호출되어 깨달음 누적. */
    public void onSkillUse(Player p, double weight) {
        gainEnlightenment(p, weight);
    }

    /** 외부 호출 — 단약 복용. */
    public void consumePill(Player p, String pillId) {
        ensureLoaded(p.getUniqueId());
        Map<String, Integer> usage = pillUsage.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        int count = usage.merge(pillId, 1, Integer::sum);
        RebornCore.get().kv().putInt(NS, p.getUniqueId(), "pill." + pillId, count);
        double penalty = Math.max(0.3, 1.0 - (count - 1) * 0.1); // 같은 단약 남용 시 효과 감소

        switch (pillId) {
            case "great_return_pill" -> { // 대환단
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.INNER_KI, 500 * penalty, "pill:" + pillId);
                Msg.send(p, "&6대환단의 기운이 단전을 가득 채운다 — 내공 +" + (int)(500*penalty));
            }
            case "spirit_clear_pill" -> { // 단봉환
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MENTAL, 20 * penalty, "pill:" + pillId);
                Msg.send(p, "&b단봉환 — 정신 맑아진다. +" + (int)(20*penalty));
            }
            case "ice_heart_pill" -> { // 빙심단
                // 주화입마 해제 + 내공 +200
                try {
                    var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                    if (cp != null) {
                        Object effects = cp.getClass().getMethod("effects").invoke(cp);
                        effects.getClass().getMethod("cure", Player.class, String.class)
                                .invoke(effects, p, "qi_deviation");
                    }
                } catch (Throwable ignored) {}
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.INNER_KI, 200 * penalty, "pill:" + pillId);
                Msg.send(p, "&f빙심단 — 주화입마 해제 + 내공 +" + (int)(200*penalty));
            }
            case "nine_yang_pill" -> { // 구양환 — 양강 무공 부작용 진정
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.INNER_KI, 100, "pill:" + pillId);
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 5, "pill:" + pillId);
            }
            case "blood_replenish_pill" -> { // 보혈단
                try { p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 10)); }
                catch (Throwable ignored) {}
            }
            case "purify_pill" -> { // 해독단
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 3, "pill:" + pillId);
            }
            default -> {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.INNER_KI, 50 * penalty, "pill:unknown");
            }
        }
        if (count > 3) {
            Msg.warn(p, "&7같은 단약을 반복 복용해 효과가 감소했다. (-" + (int)((1-penalty)*100) + "%)");
        }
    }

    /** 진법 합동 수련 — 외부에서 명상 partyMultiplier에 곱해 호출. */
    public void onArrayMeditate(Player p, int partyCount) {
        double quality = 0.8 + Math.min(0.2, partyCount * 0.05);
        double mult = 1.0 + partyCount * 0.2; // 5명 = +100%
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.INNER_KI, 5 * quality * mult, "array-meditate");
        gainEnlightenment(p, 2 * mult);
        Msg.send(p, "&5진법 합동 수련 — 내공 ×" + String.format("%.1f", mult));
    }

    /* ─────────────── 내부 헬퍼 ─────────────── */

    private void gainEnlightenment(Player p, double v) {
        UUID id = p.getUniqueId();
        ensureLoaded(id);
        double cur = enlightenment.getOrDefault(id, 0.0) + v;
        enlightenment.put(id, cur);
        // 100마다 단계 상승
        while (cur >= 100) {
            cur -= 100;
            int lvl = enlightenLevel.merge(id, 1, Integer::sum);
            RebornCore.get().kv().putInt(NS, id, "enlightLevel", lvl);
            RebornCore.get().api().addStat(id, StatType.MENTAL, 2, "enlighten-lvl");
            RebornCore.get().api().addStat(id, StatType.INNER_KI, 30, "enlighten-lvl");
            Msg.send(p, "&5&l깨달음 단계 상승 — Lv." + lvl + " §7| 정신 +2, 내공 +30");
            if (lvl == 9) {
                Bukkit.broadcastMessage("§5§l[깨달음] §f" + p.getName()
                        + " §7가 9단계 대오각성 — 무공의 극의에 닿다!");
                RebornCore.get().api().addStat(id, StatType.MENTAL, 30, "enlighten-9");
                RebornCore.get().api().addStat(id, StatType.INNER_KI, 500, "enlighten-9");
            }
        }
        enlightenment.put(id, cur);
        RebornCore.get().kv().putDouble(NS, id, "enlightProgress", cur);
    }

    private void triggerEnlightenment(Player p) {
        Bukkit.broadcastMessage("§5§l[돈오] §f" + p.getName() + " §7가 깨달음의 경지를 엿보았다!");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 5, "sudden-enlighten");
        gainEnlightenment(p, 50);
    }

    private void triggerQiDeviation(Player p) {
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object effects = cp.getClass().getMethod("effects").invoke(cp);
                effects.getClass().getMethod("apply", Player.class, String.class)
                        .invoke(effects, p, "qi_deviation");
            } else {
                Msg.error(p, "&5주화입마! 정신·내공이 흐트러진다.");
            }
        } catch (Throwable ignored) {}
    }

    private String clanType(PlayerData d) {
        // RebornClan 측 type 추적. 단순화: 클랜 ID 접두사로 판별.
        String clanId = d.clanId();
        if (clanId == null) return "";
        if (clanId.startsWith("orthodox_") || clanId.startsWith("alliance_")) return "orthodox";
        if (clanId.startsWith("cult_") || clanId.startsWith("demon_")) return "cult";
        if (clanId.startsWith("sapa_") || clanId.startsWith("unorthodox_")) return "unorthodox";
        return "";
    }

    private double clanMultiplier(PlayerData d) {
        switch (clanType(d)) {
            case "orthodox" -> { return 1.5; }
            case "cult" -> { return 0.7; }
            case "unorthodox" -> { return 1.0; }
            default -> { return 1.0; }
        }
    }

    public int enlightenLevelOf(UUID p) { ensureLoaded(p); return enlightenLevel.getOrDefault(p, 0); }
    public double enlightenmentProgress(UUID p) { ensureLoaded(p); return enlightenment.getOrDefault(p, 0.0); }
}
