package kr.reborn.npc.ai;

import kr.reborn.npc.entity.RebornNpc;

/**
 * NPC가 수행할 수 있는 단일 행동.
 *
 * 의사결정 흐름:
 *   1. priority(npc) — 현재 NPC 상황에서 이 행동을 할 가치 (0~100)
 *   2. start(npc) — 이 행동 선택 시 1회 호출
 *   3. tick(npc) — 매 AI tick 호출. true 반환 = 행동 종료
 *   4. stop(npc) — 행동 끝나거나 다른 행동으로 전환 시 호출
 *
 * 각 Behavior는 stateless하게 작성하고, 진행 상태는 RebornNpc.aiData에 저장.
 */
public interface Behavior {

    /** 행동 식별자. */
    String id();

    /** 현재 상황에서 이 행동의 우선순위. 0 = 적용 불가, 100 = 최우선. */
    int priority(RebornNpc npc);

    /** 이 행동을 시작할 때 1회 호출. */
    default void start(RebornNpc npc) {}

    /**
     * 매 AI tick (config ai-tick-interval) 호출.
     * @return true = 행동 완료 / 다음 의사결정 / false = 계속 진행
     */
    boolean tick(RebornNpc npc);

    /** 행동 종료 시 호출. */
    default void stop(RebornNpc npc) {}
}
