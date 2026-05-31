package kr.reborn.core.reincarnation;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 환생 메모리 — 전생 기록 + 회상 + 잔존 효과.
 *
 * 매 환생 시:
 *   1. 현재 생 → PastLife로 저장
 *   2. 새 생 시작 시 일부 잔존 효과 부여:
 *      - 전생 스킬 10~30% 확률 자동 학습
 *      - 전생 절대자급(스탯 5000+)이었으면 INT/MEN 보너스
 *      - 전생 신·용·요왕 등 도달이면 신성·용력·요기 일부 잔존
 *
 * 매 시간 5% 확률로 "전생 회상" 발생:
 *   - 무작위 전생의 업적 표시 (방송)
 *   - 정신력 +5
 */
public final class ReincarnationMemory {

    private final RebornCore plugin;
    private final Map<UUID, List<PastLife>> lives = new ConcurrentHashMap<>();

    public ReincarnationMemory(RebornCore plugin) {
        this.plugin = plugin;
        // 매 시간 회상 체크
        plugin.scheduler().runTimer(this::tickFlashback, 72_000L, 72_000L);
    }

    /** 환생 직전 호출 — 현재 생 저장. */
    public void recordPastLife(Player p, String causeOfDeath) {
        PlayerData d = plugin.api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        long total = (long) plugin.api().getTotalStats(p.getUniqueId());
        long duration = System.currentTimeMillis() - d.firstJoin();
        PastLife life = new PastLife(d.reincarnations(), d.worldKey(),
                d.tier(), duration, total, causeOfDeath);
        // 업적 자동 인식
        if (total >= 5000) life.achievements.add("absolute_tier");
        if (d.getStat(StatType.DIVINITY) >= 100) life.achievements.add("became_god");
        if (d.getStat(StatType.DEMON_KI) >= 20000) life.achievements.add("became_demon_lord");
        if (d.getStat(StatType.YOKAI_KI) >= 9000) life.achievements.add("became_nine_tails");
        if (d.getStat(StatType.DRAGON_POWER) >= 5000) life.achievements.add("became_dragon_god");
        if (d.getStat(StatType.OCEAN_POWER) >= 5000) life.achievements.add("became_sea_king");
        if (d.deaths() >= 100) life.achievements.add("undying");
        // 보유 스킬 — RebornSkill reflection
        try {
            var sp = Bukkit.getPluginManager().getPlugin("RebornSkill");
            if (sp != null) {
                Object store = sp.getClass().getMethod("store").invoke(sp);
                Object skillSet = store.getClass().getMethod("owned", UUID.class)
                        .invoke(store, p.getUniqueId());
                if (skillSet instanceof java.util.Collection<?> col) {
                    for (Object o : col) life.knownSkills.add(String.valueOf(o));
                }
            }
        } catch (Throwable ignored) {}
        lives.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(life);
    }

    /** 새 환생 시 잔존 효과 적용. */
    public void applyResidualEffects(Player p) {
        var pastLives = lives.get(p.getUniqueId());
        if (pastLives == null || pastLives.isEmpty()) return;
        PastLife last = pastLives.get(pastLives.size() - 1);
        Msg.send(p, "&5&l[전생의 흔적] §7" + (last.reincarnationNumber + 1) + "번째 생이 시작된다.");
        // 절대자 도달자 → 정신력 +20
        if (last.achievements.contains("absolute_tier")) {
            plugin.api().addStat(p.getUniqueId(), StatType.MENTAL, 20, "past-life-absolute");
            Msg.send(p, "&7  전생의 절대자 — 정신 +20 잔존");
        }
        // 신 도달자 → 신성 잔존
        if (last.achievements.contains("became_god")) {
            plugin.api().addStat(p.getUniqueId(), StatType.DIVINITY, 10, "past-life-god");
            Msg.send(p, "&7  전생의 신 — 신성 +10 잔존");
        }
        // 마왕 → 마기 잔존
        if (last.achievements.contains("became_demon_lord")) {
            plugin.api().addStat(p.getUniqueId(), StatType.DEMON_KI, 500, "past-life-demon");
            Msg.send(p, "&7  전생의 마왕 — 마기 +500 잔존");
        }
        // 구미호 → 요기 잔존
        if (last.achievements.contains("became_nine_tails")) {
            plugin.api().addStat(p.getUniqueId(), StatType.YOKAI_KI, 300, "past-life-yokai");
        }
        // 용신 → 용력 잔존
        if (last.achievements.contains("became_dragon_god")) {
            plugin.api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, 200, "past-life-dragon");
        }
        // 해왕 → 해양력 잔존
        if (last.achievements.contains("became_sea_king")) {
            plugin.api().addStat(p.getUniqueId(), StatType.OCEAN_POWER, 200, "past-life-sea");
        }
        // 불사 → 체력 +20%
        if (last.achievements.contains("undying")) {
            try {
                p.setMaxHealth(p.getMaxHealth() * 1.2);
                Msg.send(p, "&7  전생의 불사 — 최대 체력 +20%");
            } catch (Throwable ignored) {}
        }
        // 전생 스킬 일부 잔존 (각 스킬 마다 10% 확률)
        try {
            var sp = Bukkit.getPluginManager().getPlugin("RebornSkill");
            if (sp != null) {
                int retained = 0;
                for (String skillId : last.knownSkills) {
                    if (Rand.chance(0.1)) {
                        sp.getClass().getMethod("learnByApi", UUID.class, String.class)
                                .invoke(sp, p.getUniqueId(), skillId);
                        retained++;
                    }
                }
                if (retained > 0) {
                    Msg.send(p, "&7  전생의 스킬 §a" + retained + " §7개 잔존");
                }
            }
        } catch (Throwable ignored) {}
    }

    /** 매 시간 5% 확률 회상. */
    private void tickFlashback() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            var pastLives = lives.get(p.getUniqueId());
            if (pastLives == null || pastLives.isEmpty()) continue;
            if (!Rand.chance(0.05)) continue;
            PastLife life = pastLives.get(Rand.range(0, pastLives.size() - 1));
            Msg.send(p, "&5&l[회상] §7" + (life.reincarnationNumber)
                    + "번째 생 — §f" + life.world + " §7에서 §e" + life.tier);
            if (!life.achievements.isEmpty()) {
                p.sendMessage("§7  업적: §f" + life.achievements);
            }
            plugin.api().addStat(p.getUniqueId(), StatType.MENTAL, 5, "flashback");
        }
    }

    public List<PastLife> livesOf(UUID p) {
        return lives.getOrDefault(p, java.util.Collections.emptyList());
    }

    public Map<UUID, List<PastLife>> all() { return lives; }
}
