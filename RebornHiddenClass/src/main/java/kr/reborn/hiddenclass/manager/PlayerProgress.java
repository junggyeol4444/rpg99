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

    private static final String NS = "RebornHiddenClass.progress";

    private final RebornHiddenClass plugin;
    private final Map<UUID, Set<String>> unlocked = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> killCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tradeCounts = new ConcurrentHashMap<>();
    /** uuid → npcId → favor */
    private final Map<UUID, Map<String, Integer>> npcFavor = new ConcurrentHashMap<>();
    /** uuid → quest ids */
    private final Map<UUID, Set<String>> completedQuests = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public PlayerProgress(RebornHiddenClass plugin) { this.plugin = plugin; }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, p);
            // unlocked classes (csv)
            String unl = all.get("unlocked");
            if (unl != null && !unl.isEmpty()) {
                Set<String> s = new HashSet<>();
                for (String c : unl.split(",")) if (!c.isEmpty()) s.add(c);
                unlocked.put(p, s);
            }
            // quests (csv)
            String qs = all.get("quests");
            if (qs != null && !qs.isEmpty()) {
                Set<String> s = new HashSet<>();
                for (String c : qs.split(",")) if (!c.isEmpty()) s.add(c);
                completedQuests.put(p, s);
            }
            // kills, trades
            String k = all.get("kills");
            if (k != null) try { killCounts.put(p, Integer.parseInt(k)); } catch (Throwable ignored) {}
            String t = all.get("trades");
            if (t != null) try { tradeCounts.put(p, Integer.parseInt(t)); } catch (Throwable ignored) {}
            // npc favor: "favor.<npcId>" → int
            Map<String, Integer> favor = new HashMap<>();
            for (var e : all.entrySet()) {
                if (e.getKey().startsWith("favor.")) {
                    try { favor.put(e.getKey().substring(6), Integer.parseInt(e.getValue())); }
                    catch (Throwable ignored) {}
                }
            }
            if (!favor.isEmpty()) npcFavor.put(p, favor);
        }
    }

    public Set<String> unlocked(UUID p) {
        ensureLoaded(p);
        return unlocked.computeIfAbsent(p, k -> new HashSet<>());
    }

    public boolean has(UUID p, String id) { return unlocked(p).contains(id); }

    public void markUnlocked(UUID p, String id) {
        ensureLoaded(p);
        if (unlocked(p).add(id)) {
            kr.reborn.core.RebornCore.get().kv().put(NS, p, "unlocked", String.join(",", unlocked(p)));
        }
    }

    public int kills(UUID p) { ensureLoaded(p); return killCounts.getOrDefault(p, 0); }
    public void incrementKill(UUID p) {
        ensureLoaded(p);
        int n = killCounts.merge(p, 1, Integer::sum);
        kr.reborn.core.RebornCore.get().kv().putInt(NS, p, "kills", n);
    }
    public int trades(UUID p) { ensureLoaded(p); return tradeCounts.getOrDefault(p, 0); }
    public void incrementTrade(UUID p) {
        ensureLoaded(p);
        int n = tradeCounts.merge(p, 1, Integer::sum);
        kr.reborn.core.RebornCore.get().kv().putInt(NS, p, "trades", n);
    }
    public int favor(UUID p, String npc) {
        ensureLoaded(p);
        return npcFavor.getOrDefault(p, Map.of()).getOrDefault(npc, 0);
    }
    public void setFavor(UUID p, String npc, int v) {
        ensureLoaded(p);
        npcFavor.computeIfAbsent(p, k -> new HashMap<>()).put(npc, v);
        kr.reborn.core.RebornCore.get().kv().putInt(NS, p, "favor." + npc, v);
    }
    public Set<String> quests(UUID p) {
        ensureLoaded(p);
        return completedQuests.computeIfAbsent(p, k -> new HashSet<>());
    }

    /** Listener에서 호출하는 별칭 메서드. */
    public void incKills(UUID p) { incrementKill(p); }
    public void incTrades(UUID p) { incrementTrade(p); }
    public void markQuestComplete(UUID p, String questId) {
        ensureLoaded(p);
        if (quests(p).add(questId)) {
            kr.reborn.core.RebornCore.get().kv().put(NS, p, "quests", String.join(",", quests(p)));
        }
    }
    public void addFavor(UUID p, String npc, int delta) {
        setFavor(p, npc, favor(p, npc) + delta);
    }
}
