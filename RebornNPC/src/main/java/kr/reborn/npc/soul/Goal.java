package kr.reborn.npc.soul;

import java.util.UUID;

/**
 * NPC가 추구하는 단일 장기 목표 인스턴스.
 *
 * 예시:
 *   new Goal(GAIN_POWER, null, "왕이 되겠다")
 *   new Goal(AVENGE, "uuid-of-killer", "친구의 원수 X를 죽이겠다")
 *   new Goal(MASTER_ART, "cheonma_singong", "천마신공을 완성하겠다")
 *
 * 진행률:
 *   - 매 사이클 GoalProgressor가 약간씩 증가시킴 (느린 자연 진행)
 *   - 활동 (전투 승리·스킬 사용·결혼 등)에 따라 점프
 *   - 100 도달 시 fulfilled() 호출, archive로 이동
 */
public final class Goal {

    public final UUID id = UUID.randomUUID();
    public final GoalKind kind;
    /** 대상 — 사람(NPC id 또는 player UUID 문자열), 아이템/스킬 ID, 또는 null */
    public final String target;
    /** 사람 보기용 설명 */
    public final String description;
    /** 0~100 진행률 */
    public double progress = 0;
    /** 우선순위 — 성격·욕구로 결정됨. 행동 선택 시 가중치. */
    public int priority = 50;
    /** 생성 시각 */
    public final long createdAt = System.currentTimeMillis();
    /** 완료 시각 (미완료면 0) */
    public long completedAt;
    /** 포기 여부 (불가능 판단 시) */
    public boolean abandoned;

    public Goal(GoalKind kind, String target, String description) {
        this.kind = kind;
        this.target = target == null ? "" : target;
        this.description = description;
    }

    public boolean isFulfilled() { return progress >= 100; }
    public boolean isActive() { return !isFulfilled() && !abandoned; }

    /** 진행률 더하기 (0~100 클램프) */
    public void advance(double delta) {
        progress = Math.max(0, Math.min(100, progress + delta));
        if (progress >= 100 && completedAt == 0) completedAt = System.currentTimeMillis();
    }

    @Override public String toString() {
        return String.format("[%s%s] %s (%.0f%%)",
                kind.name(), target.isEmpty() ? "" : " → " + target,
                description, progress);
    }
}
