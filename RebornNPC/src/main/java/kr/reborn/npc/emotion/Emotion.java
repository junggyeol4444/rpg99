package kr.reborn.npc.emotion;

import java.util.EnumMap;

public final class Emotion {
    public enum Kind { HAPPINESS, ANGER, FEAR, TRUST, SADNESS, CURIOSITY }

    private final EnumMap<Kind, Double> values = new EnumMap<>(Kind.class);

    public Emotion() {
        for (Kind k : Kind.values()) values.put(k, 50.0);
    }

    public double get(Kind k) { return values.getOrDefault(k, 50.0); }
    public void set(Kind k, double v) { values.put(k, Math.max(0, Math.min(100, v))); }
    public void add(Kind k, double v) { set(k, get(k) + v); }

    public void decay(EnumMap<Kind, Double> rates) {
        for (Kind k : Kind.values()) {
            double r = rates.getOrDefault(k, 0.0);
            double curr = get(k);
            // 50으로 회귀
            if (curr > 50) set(k, Math.max(50, curr - r));
            else if (curr < 50) set(k, Math.min(50, curr + r));
        }
    }
}
