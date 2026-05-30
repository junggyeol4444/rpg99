package kr.reborn.mob.boss;

/**
 * 보스 패턴 enum — 페이즈별 호출되는 공격/특수 행동.
 *
 * 각 패턴은 BossManager.executePattern에서 실제 효과 적용.
 * 패턴은 cooldownMs마다 한 번씩 시전.
 */
public enum BossPattern {
    /** 광역 슬로우 - 반경 15블록 적에게 SLOW 5초. */
    AOE_SLOW              (15_000),
    /** 광역 위더 + 약화 - 반경 12. */
    AOE_WITHER            (20_000),
    /** 광역 직접 데미지 - 반경 10. */
    AOE_BURST             (25_000),
    /** 광역 폭발 (TNT 시뮬) - 반경 8 데미지 + 흩날림. */
    AOE_EXPLOSION         (30_000),
    /** 유성우 - 8개 폭발 무작위 위치. */
    METEOR_RAIN           (40_000),
    /** 직선 빔 - 가장 가까운 플레이어 방향 데미지. */
    BEAM_LINE             (15_000),
    /** 자신 회복 - HP 15%. */
    SELF_HEAL             (45_000),
    /** 자기 강화 - INCREASE_DAMAGE + REGEN 10초. */
    SELF_BUFF             (60_000),
    /** 일시 무적 - DAMAGE_RESISTANCE 5초. */
    INVULNERABLE_BRIEF    (90_000),
    /** 부하 소환 - 동일 정의의 약체 몹 3마리. */
    SUMMON_MINIONS        (50_000),
    /** 텔레포트 - 가장 가까운 플레이어 뒤로. */
    TELEPORT_TO_TARGET    (20_000),
    /** 끌어당기기 - 반경 20 플레이어를 자신 위치로. */
    PULL_PLAYERS          (35_000),
    /** 밀어내기 - 반경 10 플레이어 멀리. */
    KNOCKBACK             (25_000),
    /** 분노 - 페이즈 전환 시 전체 데미지 ×1.5. */
    ENRAGE                (0),
    /** 동결 - 반경 8 플레이어 SLOW 8 5초. */
    FREEZE_AOE            (30_000),
    /** 도트 광역 - 반경 12 POISON 10초. */
    POISON_FOG            (35_000),
    /** 시야 차단 - 반경 15 BLINDNESS 5초. */
    BLIND_AOE             (40_000),
    /** 텔레포트 무작위 - 보스 자신 무작위 위치로 (회피). */
    TELEPORT_RANDOM       (20_000),
    /** 광역 위압 - 반경 30 적에게 WEAKNESS 6 + SLOW 4. */
    OPRESSIVE_AURA        (45_000),
    /** 차원 베기 - 직선 30블록 데미지. */
    DIMENSIONAL_SLASH     (50_000);

    public final long cooldownMs;

    BossPattern(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }
}
