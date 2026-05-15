package kr.reborn.hiddenclass.manager;

import kr.reborn.hiddenclass.RebornHiddenClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 플레이어별 보유 히든 클래스 + 진행 통계 (kill count, trade count 등). */
public final class PlayerProgress {

    private final RebornHiddenClass plugin;
    private final Map<UUID, Set<String>> unlocked = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tradeCounts = new ConcurrentHashMap<>();
    /** uuid → npcId → favor */
    private final Map<UUID, Map<String, Integer>> npcFavor = new ConcurrentHashMap<>();
    /** uuid → quest ids */
    private final Map<UUID, Set<String>> completedQuests = new ConcurrentHashMap<>();

    public PlayerProgress(RebornHiddenClass plugin) { this.plugin = plugin; }

    public Set<String> unlocked(UUID p) {
        return unlocked.computeIfAbsent(p, k -> new HashSet<>());
    }

    public boolean has(UUID p, String id) { return unlocked(p).contains(id); }

    public void markUnlocked(UUID p, String id) { unlocked(p).add(id); }

    public int kills(UUID p) { return killCounts.getOrDefault(p, 0); }
    public void incrementKill(UUID p) { killCounts.merge(p, 1, Integer::sum); }
    public int trades(UUID p) { return tradeCounts.getOrDefault(p, 0); }
    public void incrementTrade(UUID p) { tradeCounts.merge(p, 1, Integer::sum); }
    public int favor(UUID p, String npc) {
        return npcFavor.getOrDefault(p, Map.of()).getOrDefault(npc, 0);
    }
    public void setFavor(UUID p, String npc, int v) {
        npcFavor.computeIfAbsent(p, k -> new HashMap<>()).put(npc, v);
    }
    public Set<String> quests(UUID p) {
        return completedQuests.computeIfAbsent(p, k -> new HashSet<>());
    }

    /** Listener에서 호출하는 별칭 메서드. */
    public void incKills(UUID p) { incrementKill(p); }
    public void incTrades(UUID p) { incrementTrade(p); }
    public void markQuestComplete(UUID p, String questId) { quests(p).add(questId); }
    public void addFavor(UUID p, String npc, int delta) {
        setFavor(p, npc, favor(p, npc) + delta);
    }
}
