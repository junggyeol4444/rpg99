package kr.reborn.core.tier;

public final class Tier {
    public final String name;
    public final double min, max;
    /** 드래곤계 추가 조건 (필요 나이 등) */
    public final int requiredAge;

    public Tier(String name, double min, double max, int requiredAge) {
        this.name = name; this.min = min; this.max = max; this.requiredAge = requiredAge;
    }

    public boolean inRange(double total) {
        return total >= min && total < max;
    }
}
