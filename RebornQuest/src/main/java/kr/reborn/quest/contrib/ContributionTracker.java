package kr.reborn.quest.contrib;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 월드 퀘스트 기여도 추적: questId → (uuid → score) */
public final class ContributionTracker {

    private final ConcurrentHashMap<String, Map<UUID, Double>> data = new ConcurrentHashMap<>();

    public void add(String questId, UUID who, double score) {
        data.computeIfAbsent(questId, x -> new HashMap<>())
                .merge(who, score, Double::sum);
    }

    public Map<UUID, Double> of(String questId) {
        return data.getOrDefault(questId, Map.of());
    }

    public void clear(String questId) { data.remove(questId); }
}
