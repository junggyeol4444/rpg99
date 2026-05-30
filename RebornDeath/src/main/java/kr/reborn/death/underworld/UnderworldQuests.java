package kr.reborn.death.underworld;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 명계 토착민 전용 의뢰 시스템.
 *
 * 토착민(UnderworldResident)에게 명기를 모아 의뢰를 수행 → 명기·환생 가능 자원 획득.
 *
 * 의뢰 종류:
 *   COLLECT_BONE — 명병 처치 5
 *   ESCAPE_SCOUT — 명계 일정 거리 탐색
 *   BURNT_OFFERING — 무산귀에게 제물 5개
 *   RESCUE_LOST — 잃은 영혼 3 구출 (NPC interact)
 *
 * 명기 10000 누적 → 환생 자격 자동 부여.
 */
public final class UnderworldQuests {

    private final RebornDeath plugin;
    /** uuid → 진행중 의뢰 → 진척 */
    private final Map<UUID, Map<String, Integer>> progress = new ConcurrentHashMap<>();

    public UnderworldQuests(RebornDeath plugin) {
        this.plugin = plugin;
        RebornCore.get().scheduler().runTimer(this::checkReincarnation, 1200L, 1200L);
    }

    /** 의뢰 진행 증가. */
    public void progress(Player p, String questId, int delta) {
        Map<String, Integer> map = progress.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        int cur = map.merge(questId, delta, Integer::sum);
        int target = targetOf(questId);
        if (cur >= target) {
            completeQuest(p, questId);
            map.remove(questId);
        }
    }

    /** 의뢰 완료 → 명기 보상. */
    private void completeQuest(Player p, String questId) {
        int reward = rewardOf(questId);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.UNDERWORLD_KI, reward, "underworld-quest:" + questId);
        Msg.send(p, "&5명계 의뢰 완료: " + labelOf(questId) + " §6명기 +" + reward);
        Bukkit.broadcastMessage("§5§l[명계] §f" + p.getName() + " §7가 " + labelOf(questId) + " 완료");
    }

    /** 매 분 명기 10000+ 체크. */
    private void checkReincarnation() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d == null) continue;
            if (d.worldKey() != WorldKey.UNDERWORLD) continue;
            double ki = d.getStat(StatType.UNDERWORLD_KI);
            if (ki >= 10000) {
                if (alreadyEligible(p)) continue;
                grantReincarnationEligibility(p);
            }
        }
    }

    private final java.util.Set<UUID> reincarnationGranted = new java.util.HashSet<>();
    private boolean alreadyEligible(Player p) {
        return reincarnationGranted.contains(p.getUniqueId());
    }

    private void grantReincarnationEligibility(Player p) {
        reincarnationGranted.add(p.getUniqueId());
        Bukkit.broadcastMessage("§5§l[명계 해탈] §f" + p.getName()
                + " §7가 명기 10000을 모았다! 환생 자격 회복.");
        Msg.send(p, "&6/reroll 명령으로 환생을 시도할 수 있다.");
    }

    private int targetOf(String questId) {
        return switch (questId) {
            case "collect_bone" -> 5;
            case "escape_scout" -> 100;
            case "burnt_offering" -> 5;
            case "rescue_lost" -> 3;
            default -> 1;
        };
    }

    private int rewardOf(String questId) {
        return switch (questId) {
            case "collect_bone" -> 100;
            case "escape_scout" -> 200;
            case "burnt_offering" -> 150;
            case "rescue_lost" -> 300;
            default -> 50;
        };
    }

    private String labelOf(String questId) {
        return switch (questId) {
            case "collect_bone" -> "명병 뼈 5개 수집";
            case "escape_scout" -> "명계 척후 100블록";
            case "burnt_offering" -> "무산귀 제물 5개";
            case "rescue_lost" -> "잃은 영혼 3 구출";
            default -> "?";
        };
    }
}
