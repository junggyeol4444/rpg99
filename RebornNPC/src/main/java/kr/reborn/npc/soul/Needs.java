package kr.reborn.npc.soul;

import java.util.EnumMap;

/**
 * 매슬로우 욕구 피라미드 기반 NPC 욕구 시스템.
 *
 * 각 욕구는 0~100 만족도. 낮을수록 절박.
 * 자연 감쇠 — 시간이 지나면 음식·휴식 욕구는 떨어짐, 채워지지 않으면 행동이 절박해짐.
 *
 * 의사결정:
 *   가장 낮은 욕구 = 가장 우선해야 할 활동.
 *   FOOD < 30 → 음식 찾는 행동 우선
 *   SAFETY < 40 → 도주·은신
 *   COMPANIONSHIP < 30 → 사교 행동
 *   STATUS < 30 → 권력·명예 추구
 *   MASTERY < 30 → 수련·연구
 */
public final class Needs {

    public enum Kind {
        // 생리적 (Physiological)
        FOOD(0.5),           // 분당 -0.5
        REST(0.3),           // 휴식·수면

        // 안전 (Safety)
        SAFETY(0.2),

        // 소속·애정 (Belonging)
        COMPANIONSHIP(0.15), // 외로움
        LOVE(0.05),          // 사랑·관계

        // 존경 (Esteem)
        STATUS(0.08),        // 사회적 지위
        ACHIEVEMENT(0.05),   // 성취감

        // 자아실현 (Self-actualization)
        MASTERY(0.03),       // 기술·지식 숙련
        AUTONOMY(0.02);      // 자율·자유

        public final double decayPerMinute;
        Kind(double decay) { this.decayPerMinute = decay; }
    }

    private final EnumMap<Kind, Double> values = new EnumMap<>(Kind.class);
    private long lastDecayAt = System.currentTimeMillis();

    public Needs() {
        for (Kind k : Kind.values()) values.put(k, 70.0);
    }

    public double get(Kind k) { return values.getOrDefault(k, 50.0); }
    public void set(Kind k, double v) { values.put(k, Math.max(0, Math.min(100, v))); }
    public void add(Kind k, double delta) { set(k, get(k) + delta); }

    /** 매 사이클마다 호출 — 시간 경과만큼 욕구 감쇠. */
    public void decay() {
        long now = System.currentTimeMillis();
        double minutes = (now - lastDecayAt) / 60_000.0;
        if (minutes < 0.1) return;
        for (Kind k : Kind.values()) add(k, -k.decayPerMinute * minutes);
        lastDecayAt = now;
    }

    /** 절박한 욕구 (30 이하) 가장 낮은 것. 없으면 null. */
    public Kind mostUrgent() {
        Kind best = null;
        double bestV = 30;
        for (Kind k : Kind.values()) {
            double v = get(k);
            if (v < bestV) { bestV = v; best = k; }
        }
        return best;
    }

    /** 만족도 평균 — 행복 지표. */
    public double averageSatisfaction() {
        double sum = 0;
        for (Kind k : Kind.values()) sum += get(k);
        return sum / Kind.values().length;
    }
}
