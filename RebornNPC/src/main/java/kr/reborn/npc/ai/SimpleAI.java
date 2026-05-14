package kr.reborn.npc.ai;

import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;

/** 매우 단순한 상태 머신. AI 판단 주기마다 호출. */
public final class SimpleAI {
    private SimpleAI() {}

    public static void step(RebornNpc n) {
        if (n.state == NpcState.DEAD) return;

        // 감정 → 행동 전이
        if (n.emotion.get(Emotion.Kind.ANGER) > 80) {
            n.state = NpcState.COMBAT;
            return;
        }
        if (n.emotion.get(Emotion.Kind.FEAR) > 70) {
            n.state = NpcState.FLEE;
            return;
        }
        // 직업별 기본 행동
        switch (n.job) {
            case "GUARD":   n.state = NpcState.PATROL; break;
            case "MERCHANT":n.state = NpcState.TRADE; break;
            case "FARMER":
            case "BLACKSMITH":
            case "ALCHEMIST":
                n.state = NpcState.WORK; break;
            default: n.state = NpcState.IDLE;
        }
    }
}
