package kr.reborn.quest.contrib;

import kr.reborn.core.RebornCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 월드 퀘스트 기여도 추적: questId → (uuid → score) */
public final class ContributionTracker {

    private static final String NS = "RebornQuest.contrib";

    private final ConcurrentHashMap<String, Map<UUID, Double>> data = new ConcurrentHashMap<>();
    private final java.util.Set<String> loadedQuests = ConcurrentHashMap.newKeySet();

    public ContributionTracker() {
        // 부팅 시 전체 로드 — owner=null, key="questId:uuid"
        try {
            var all = RebornCore.get().kv().loadAll(NS, null);
            for (var e : all.entrySet()) {
                String[] parts = e.getKey().split(":", 2);
                if (parts.length != 2) continue;
                try {
                    UUID uuid = UUID.fromString(parts[1]);
                    double v = Double.parseDouble(e.getValue());
                    data.computeIfAbsent(parts[0], x -> new HashMap<>()).put(uuid, v);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public void add(String questId, UUID who, double score) {
        Map<UUID, Double> m = data.computeIfAbsent(questId, x -> new HashMap<>());
        double cur = m.merge(who, score, Double::sum);
        // owner=questId, key=playerUUID
        try {
            // KVStore.put은 UUID owner이지만 questId가 String이므로
            // namespace에 questId를 포함하지 않고, key를 "questId:uuid"로 구성
            RebornCore.get().kv().putDouble(NS, null, questId + ":" + who, cur);
        } catch (Throwable ignored) {}
    }

    public Map<UUID, Double> of(String questId) {
        return data.getOrDefault(questId, Map.of());
    }

    public void clear(String questId) {
        Map<UUID, Double> removed = data.remove(questId);
        if (removed != null) {
            for (UUID who : removed.keySet()) {
                try { RebornCore.get().kv().remove(NS, null, questId + ":" + who); }
                catch (Throwable ignored) {}
            }
        }
    }
}
