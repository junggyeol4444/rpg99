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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 지구계 성장: 헌터·게이트.
 *
 * 레벨업: 10 경험치당 +1 레벨, 레벨업마다 +3 자유 스탯
 * 게이트 카드 시스템:
 *   몬스터 처치 시 1% 확률로 해당 종 "각인 카드" 획득 (수집 요소)
 *   각인 카드 수 = 보너스 스탯 (10장당 +5 모든 8공통 스탯)
 *
 * S랭크 게이트: onSRankGate(p) = 큰 보상
 * 헌터 협회 명성: 외부 호출 onAssocReputation
 */
public final class EarthGrowth implements GrowthStrategy {

    /** uuid → 수집한 각인 카드 종 ID 집합 */
    private final Map<UUID, Set<String>> cards = new ConcurrentHashMap<>();
    /** uuid → 헌터 협회 명성 */
    private final Map<UUID, Double> reputation = new ConcurrentHashMap<>();
    /** uuid → S랭크 클리어 횟수 */
    private final Map<UUID, Integer> sRankClears = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.EARTH; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        double oldLv = d.getStat(StatType.LEVEL);
        double exp = Math.max(0.5, mobLevel / 10.0);
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.LEVEL, exp, "kill-exp");
        double newLv = d.getStat(StatType.LEVEL);
        if (Math.floor(newLv / 10.0) > Math.floor(oldLv / 10.0)) {
            applyLevelUp(p);
        }
        // 각인 카드 1% 확률
        if (Rand.chance(0.01)) {
            String species = guessSpecies(mobLevel);
            collectCard(p, species);
        }
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.LEVEL, 5 * weight, "quest-exp");
        onAssocReputation(p, weight * 2);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        Msg.warn(p, "지구에서는 운기조식이 통하지 않는다.");
    }

    /** 외부 호출 — S랭크 게이트 클리어. */
    public void onSRankGate(Player p) {
        int n = sRankClears.merge(p.getUniqueId(), 1, Integer::sum);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.LEVEL, 50, "s-rank-clear");
        for (StatType t : StatType.COMMON_8) {
            RebornCore.get().api().addStat(p.getUniqueId(), t, 5, "s-rank-clear");
        }
        Bukkit.broadcastMessage("§e§l[S랭크] §f" + p.getName()
                + " §7가 S랭크 게이트 클리어! §6(총 " + n + "회) §7모든 스탯 +5");
        onAssocReputation(p, 50);
    }

    /** 외부 호출 — 헌터 협회 명성. */
    public void onAssocReputation(Player p, double delta) {
        double cur = reputation.merge(p.getUniqueId(), delta, Double::sum);
        // 1000, 5000, 10000 마일스톤
        if (cur >= 10000 && cur - delta < 10000) {
            Bukkit.broadcastMessage("§e§l[헌터 협회] §f" + p.getName()
                    + " §7가 헌터 명예의 전당에 등록되었다!");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.CHARISMA, 50, "assoc-fame");
        } else if (cur >= 5000 && cur - delta < 5000) {
            Msg.send(p, "&6헌터 협회: 명성 5000 달성 — A급 인증 +CHA 20");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.CHARISMA, 20, "assoc-A");
        } else if (cur >= 1000 && cur - delta < 1000) {
            Msg.send(p, "&e헌터 협회: 명성 1000 달성 — B급 인증");
        }
    }

    private void applyLevelUp(Player p) {
        for (int i = 0; i < 3; i++) {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.values()[Rand.range(0, 7)], 1, "earth-levelup");
        }
        Msg.send(p, "&6레벨 업! +3 자유 스탯 (랜덤 분배)");
    }

    private String guessSpecies(double mobLevel) {
        if (mobLevel >= 200) return "boss_imprint";
        if (mobLevel >= 100) return "elite_imprint";
        if (mobLevel >= 50)  return "common_imprint";
        return "weak_imprint";
    }

    private void collectCard(Player p, String species) {
        Set<String> set = cards.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (set.add(species)) {
            Msg.send(p, "&6각인 카드 획득: " + species + " §7(총 " + set.size() + "종)");
            // 10장당 +5 모든 스탯
            if (set.size() % 10 == 0) {
                for (StatType t : StatType.COMMON_8) {
                    RebornCore.get().api().addStat(p.getUniqueId(), t, 5, "card-10");
                }
                Msg.send(p, "&e각인 카드 " + set.size() + "종 — 모든 스탯 +5");
            }
        }
    }

    public Set<String> cardsOf(UUID p) { return cards.getOrDefault(p, java.util.Collections.emptySet()); }
    public double reputationOf(UUID p) { return reputation.getOrDefault(p, 0.0); }
    public int sRankClearsOf(UUID p) { return sRankClears.getOrDefault(p, 0); }
}
