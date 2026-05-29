package kr.reborn.god.trial;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 신 등극 시련 — 절대자가 신이 되기 위한 3가지 시련 통과 상태.
 *
 * 1) 권능 시련 (POWER) — 강한 적 3체 격파
 * 2) 지혜 시련 (WISDOM) — 3개 수수께끼 정답
 * 3) 의지 시련 (CONVICTION) — 위험한 환경에서 5분 생존
 *
 * 3 시련 모두 통과 시 자동으로 신 등극 가능.
 */
public final class AscensionTrial {

    public enum Stage { POWER, WISDOM, CONVICTION, COMPLETE }

    public final UUID player;
    public final long startedAt = System.currentTimeMillis();

    /** 격파한 강한 적 수 (POWER 단계). */
    public int powerKills;
    /** 정답 맞춘 수수께끼 수 (WISDOM 단계). */
    public int wisdomCorrect;
    /** 의지 시련 시작 시각 (CONVICTION 단계). 0이면 미시작. */
    public long convictionStartedAt;
    /** 통과한 단계 집합. */
    public final Set<Stage> passed = new HashSet<>();
    /** 현재 묻고 있는 수수께끼 인덱스. */
    public int currentRiddleIdx = -1;
    /** WISDOM 단계 출제 받은 수수께끼 인덱스 (중복 출제 방지). */
    public final Set<Integer> askedRiddles = new HashSet<>();

    public AscensionTrial(UUID player) { this.player = player; }

    public boolean passedAll() {
        return passed.contains(Stage.POWER)
                && passed.contains(Stage.WISDOM)
                && passed.contains(Stage.CONVICTION);
    }

    public Stage nextStage() {
        if (!passed.contains(Stage.POWER)) return Stage.POWER;
        if (!passed.contains(Stage.WISDOM)) return Stage.WISDOM;
        if (!passed.contains(Stage.CONVICTION)) return Stage.CONVICTION;
        return Stage.COMPLETE;
    }
}
