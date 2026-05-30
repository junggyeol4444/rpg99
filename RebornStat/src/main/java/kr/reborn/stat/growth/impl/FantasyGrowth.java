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
 * 판타지 성장: 마법사 학파·룬·종족 보너스.
 *
 * 6 마법학파: ELEMENTAL, ARCANE, HOLY, NECROMANCY, ILLUSION, RUNE
 *   학파별 마스터리 0~1000. 마스터리 100당 해당 마법 위력 +5%.
 *   학파 마스터리 500 달성 시 "마법사" 칭호, 1000 = "대마법사"
 *
 * 룬 시스템: 룬 수집 (RUNE 마스터리에 가산), 룬 조합 시 마나 +50
 * 종족: HUMAN(균형), ELF(MANA ×1.5), DWARF(ENDURANCE +20%), HALFLING(LUCK +10)
 *
 * 외부 호출:
 *   onSpellCast(p, school): 학파 마스터리 +1
 *   onRuneCollect(p, runeId): 룬 수집
 *   setRace(p, race): 종족 설정 (영구)
 */
public final class FantasyGrowth implements GrowthStrategy {

    public enum School { ELEMENTAL, ARCANE, HOLY, NECROMANCY, ILLUSION, RUNE }
    public enum Race { HUMAN, ELF, DWARF, HALFLING }

    private final Map<UUID, Map<School, Double>> mastery = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> runes = new ConcurrentHashMap<>();
    private final Map<UUID, Race> race = new ConcurrentHashMap<>();
    private final Map<UUID, Set<School>> masterTitles = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.FANTASY; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        StatType stat = StatType.COMMON_8[Rand.range(0, 7)];
        RebornCore.get().api().addStat(p.getUniqueId(), stat,
                0.5 + Math.min(2.0, mobLevel / 20.0), "mob");
        // 종족 보너스
        applyRaceBonus(p, 0.2);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.INTELLIGENCE, 1.5 * weight, "quest");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.MANA, 5 * weight, "quest");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.MANA, 2 * quality, "meditate");
        // 모든 학파 마스터리 +0.3
        for (School s : School.values()) addMastery(p, s, 0.3 * quality);
    }

    /** 외부 호출 — 마법 시전. */
    public void onSpellCast(Player p, School school) {
        addMastery(p, school, 1.0);
    }

    /** 외부 호출 — 룬 수집. */
    public void onRuneCollect(Player p, String runeId) {
        Set<String> set = runes.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (set.add(runeId)) {
            addMastery(p, School.RUNE, 5);
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MANA, 50, "rune-collect");
            Msg.send(p, "&5룬 수집: " + runeId + " §7(총 " + set.size() + ")");
            if (set.size() % 10 == 0) {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.INTELLIGENCE, 5, "rune-10");
                Msg.send(p, "&5룬 10종 수집 — 지능 +5");
            }
        }
    }

    /** 외부 호출 — 종족 설정 (영구). */
    public void setRace(Player p, Race r) {
        Race prev = race.put(p.getUniqueId(), r);
        if (prev == r) return;
        if (prev != null) revertRaceBonus(p, prev);
        applyInitialRaceBonus(p, r);
        Msg.send(p, "&6종족: " + r);
    }

    private void addMastery(Player p, School school, double v) {
        Map<School, Double> map = mastery.computeIfAbsent(p.getUniqueId(),
                k -> new java.util.EnumMap<>(School.class));
        double next = Math.min(1000, map.getOrDefault(school, 0.0) + v);
        map.put(school, next);
        Set<School> titles = masterTitles.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        if (next >= 1000 && !titles.contains(school)) {
            titles.add(school);
            Bukkit.broadcastMessage("§5§l[대마법사] §f" + p.getName()
                    + " §7가 " + school + " 학파의 대마법사가 되었다!");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.INTELLIGENCE, 30, "school-master");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.MANA, 200, "school-master");
        } else if (next >= 500 && next - v < 500) {
            Msg.send(p, "&5" + school + " 마스터리 500 — 마법사 칭호");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.INTELLIGENCE, 10, "school-mastery-500");
        }
    }

    private void applyInitialRaceBonus(Player p, Race r) {
        switch (r) {
            case ELF -> {
                double cur = RebornCore.get().api().getStat(p.getUniqueId(), StatType.MANA);
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MANA, cur * 0.5, "race:ELF");
            }
            case DWARF -> {
                double cur = RebornCore.get().api().getStat(p.getUniqueId(), StatType.ENDURANCE);
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.ENDURANCE, cur * 0.2, "race:DWARF");
            }
            case HALFLING -> {
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.LUCK, 10, "race:HALFLING");
            }
            case HUMAN -> { /* 균형 — 영구 보너스 없음 */ }
        }
    }

    private void applyRaceBonus(Player p, double v) {
        Race r = race.get(p.getUniqueId());
        if (r == null) return;
        switch (r) {
            case ELF -> RebornCore.get().api().addStat(p.getUniqueId(), StatType.MANA, v * 2, "elf-mob");
            case DWARF -> RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, v, "dwarf-mob");
            case HALFLING -> RebornCore.get().api().addStat(p.getUniqueId(), StatType.AGILITY, v, "halfling-mob");
            case HUMAN -> RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.values()[Rand.range(0, 7)], v, "human-mob");
        }
    }

    private void revertRaceBonus(Player p, Race r) {
        // 단순화: 영구 종족 보너스는 되돌리지 않음 (한번 설정한 종족 변경은 드물기에)
    }

    public double masteryOf(UUID p, School s) {
        Map<School, Double> map = mastery.get(p);
        if (map == null) return 0;
        return map.getOrDefault(s, 0.0);
    }

    public Race raceOf(UUID p) { return race.get(p); }
    public int runesCollected(UUID p) {
        Set<String> set = runes.get(p);
        return set == null ? 0 : set.size();
    }
}
