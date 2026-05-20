package kr.reborn.npc.relation;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC가 가지는 다른 존재(NPC/플레이어)와의 관계도.
 * -100 ~ 100. 50+: 우호 / 0~50: 중립 / 0 미만: 적대 / -50 미만: 불구대천.
 */
public final class Relations {

    private final ConcurrentHashMap<UUID, Double> playerScores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> npcScores = new ConcurrentHashMap<>();

    public double player(UUID id) { return playerScores.getOrDefault(id, 0.0); }
    public double npc(String id) { return npcScores.getOrDefault(id, 0.0); }

    public void addPlayer(UUID id, double delta) {
        playerScores.merge(id, delta, (a, b) -> Math.max(-100, Math.min(100, a + b)));
    }
    public void addNpc(String id, double delta) {
        npcScores.merge(id, delta, (a, b) -> Math.max(-100, Math.min(100, a + b)));
    }

    public Stage stagePlayer(UUID id) {
        double v = player(id);
        if (v <= -50) return Stage.NEMESIS;
        if (v < 0) return Stage.WARY;
        if (v < 30) return Stage.NEUTRAL;
        if (v < 60) return Stage.FRIENDLY;
        if (v < 80) return Stage.CLOSE;
        return Stage.BEST;
    }

    public enum Stage { NEMESIS, WARY, NEUTRAL, FRIENDLY, CLOSE, BEST }
}
