package kr.reborn.npc.ai.utility;

import kr.reborn.npc.entity.RebornNpc;

/**
 * 한 행동의 utility 계산에 들어가는 단일 고려사항.
 *
 * Behavior가 여러 Consideration을 가지고, 결과는 모두 곱해진다 (multiplicative).
 *  → 어느 하나라도 0이면 그 행동의 utility는 0
 *  → 모두 1이면 utility 최대
 *
 * 예: CombatBehavior의 Consideration들:
 *   - "분노 수준" (sigmoid mid=60)
 *   - "AGGRESSION 가중" (linear -100→0.2, 100→1.0)
 *   - "HP 비율" (linearInverted 0→1, 1→0.3 — HP 낮으면 공격 의지 떨어짐)
 *   - "근처 적 있음" (bool)
 *
 * 한 NPC가 AGGRESSION=-50이면 utility 매우 낮아 평생 안 싸움.
 */
public interface Consideration {
    /** @return 0~1 utility 점수 */
    double score(RebornNpc npc);

    /** 디버그용 이름 */
    String name();
}
