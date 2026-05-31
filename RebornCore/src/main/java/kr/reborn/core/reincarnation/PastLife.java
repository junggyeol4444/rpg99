package kr.reborn.core.reincarnation;

import kr.reborn.core.data.WorldKey;

/**
 * 전생 기록 — 한 번의 환생 사이클을 압축한 메모리.
 *
 * 환생 후 일정 확률로 단편적 회상 (꿈·플래시백) 발생.
 * 전생의 특정 스킬 일부 잔존 (10~30%).
 */
public final class PastLife {

    public final int reincarnationNumber;
    public final WorldKey world;
    public final String tier;
    public final long durationMs;
    public final long totalStatPeak;
    public final String causeOfDeath;
    /** 주요 업적 (예: "killed_marwang", "achieved_immortal", "founded_clan") */
    public final java.util.Set<String> achievements = new java.util.HashSet<>();
    /** 마지막 보유 스킬 — 다음 생에 부분 잔존 가능 */
    public final java.util.Set<String> knownSkills = new java.util.HashSet<>();
    public final long timestamp;

    public PastLife(int reincarnationNumber, WorldKey world, String tier,
                    long durationMs, long totalStatPeak, String causeOfDeath) {
        this.reincarnationNumber = reincarnationNumber;
        this.world = world; this.tier = tier;
        this.durationMs = durationMs;
        this.totalStatPeak = totalStatPeak;
        this.causeOfDeath = causeOfDeath;
        this.timestamp = System.currentTimeMillis();
    }
}
