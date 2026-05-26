package kr.reborn.npc.social;

import java.util.HashMap;
import java.util.Map;

/**
 * 한 NPC가 다른 존재들에 대해 갖는 평판 점수.
 *
 * 소문(Rumor)을 들으면 갱신. -100(악명) ~ +100(명성).
 * 직접 경험(Memory)과 별개 — 한 번도 안 만난 자도 소문만으로 호불호 형성.
 *
 * Soul.relationToward에서 Memory.sentiment + Reputation×0.5로 합산됨.
 */
public final class Reputation {

    private final Map<String, Double> scores = new HashMap<>();
    private long lastDecayAt = System.currentTimeMillis();

    public double scoreOf(String subject) {
        return scores.getOrDefault(subject, 0.0);
    }

    public void apply(String subject, double delta) {
        scores.merge(subject, delta, (a, b) -> Math.max(-100, Math.min(100, a + b)));
    }

    /** 소문 1건 흡수. */
    public void absorb(Rumor rumor) {
        apply(rumor.subject, rumor.reputationEffect());
    }

    /** 시간 경과 평판 감쇠 — 0으로 서서히 회귀 (잊혀짐). 하루 0.1/점. */
    public void decay() {
        long now = System.currentTimeMillis();
        double days = (now - lastDecayAt) / 86_400_000.0;
        if (days < 0.01) return;
        double amount = days * 0.1 * 100;  // 하루 10점 회귀
        scores.replaceAll((k, v) -> {
            if (v > 0) return Math.max(0, v - amount);
            if (v < 0) return Math.min(0, v + amount);
            return 0.0;
        });
        scores.values().removeIf(v -> Math.abs(v) < 0.5);
        lastDecayAt = now;
    }

    public Map<String, Double> all() { return scores; }

    /** 가장 평판 나쁜 대상 (악인 — DEFEAT_RIVAL 목표 트리거용). */
    public String worstReputation(double threshold) {
        String worst = null;
        double worstV = -threshold;
        for (var e : scores.entrySet()) {
            if (e.getValue() < worstV) { worstV = e.getValue(); worst = e.getKey(); }
        }
        return worst;
    }

    /** 가장 평판 좋은 대상 (영웅·존경 — SERVE_LORD 트리거용). */
    public String bestReputation(double threshold) {
        String best = null;
        double bestV = threshold;
        for (var e : scores.entrySet()) {
            if (e.getValue() > bestV) { bestV = e.getValue(); best = e.getKey(); }
        }
        return best;
    }
}
