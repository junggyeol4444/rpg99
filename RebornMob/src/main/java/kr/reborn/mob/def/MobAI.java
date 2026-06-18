package kr.reborn.mob.def;

public enum MobAI {
    // 기존
    BASIC,       // 바닐라 — 특수 행동 없음
    PACK,        // 무리 — 동료 호출·협공
    RANGED,      // 원거리 — 거리 유지·사격(카이팅)
    BOSS,        // 보스 — HP% 페이즈 전환·패턴
    FLEE,        // 도주 — 플레이어 회피
    TERRITORIAL, // 영역 — 둥지 반경 안에서만 교전·복귀
    FLYING,      // 비행 — 바닐라
    AQUATIC,     // 수중 — 바닐라
    PASSIVE,     // 비공격 — 바닐라
    SWARM,       // 군집 — 다수 약체 돌격
    // Step 3 추가
    CASTER,      // 시전 — 주기적 마법(광역/디버프)
    TANK,        // 방패 — 저항 버프·전진
    SUPPORT,     // 지원 — 아군 회복·강화
    BERSERKER,   // 광폭 — HP 낮을수록 강해짐
    SUMMONER,    // 소환 — 하수인 소환
    AMBUSH       // 매복 — 은신 후 기습
}
