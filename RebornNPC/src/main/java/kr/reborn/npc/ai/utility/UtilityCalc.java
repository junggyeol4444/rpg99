package kr.reborn.npc.ai.utility;

import kr.reborn.npc.entity.RebornNpc;

import java.util.List;

/**
 * Consideration 리스트를 종합하여 행동의 최종 utility 계산.
 *
 * 기본 방식: 모든 Consideration 곱 (multiplicative)
 *   → 하나라도 0이면 전체 0 (행동 불가)
 *   → 모두 만족이면 최대 1.0
 *
 * "Compensation factor" — Consideration 수가 많을수록 평균이 낮아지는 편향 보정:
 *   final = compensated × (1 + (1 - compensated) × modFactor)
 *   여기서 modFactor = (consideration_count - 1) / consideration_count
 */
public final class UtilityCalc {

    private UtilityCalc() {}

    public static double evaluate(RebornNpc npc, List<Consideration> considerations) {
        if (considerations.isEmpty()) return 0;
        double product = 1.0;
        for (Consideration c : considerations) {
            double s = c.score(npc);
            if (s <= 0) return 0;
            product *= s;
        }
        // 보정
        double modFactor = 1.0 - (1.0 / considerations.size());
        double compensation = (1.0 - product) * modFactor;
        return product + (compensation * product);
    }

    /** 디버그 — 각 Consideration의 점수를 한 줄로. */
    public static String breakdown(RebornNpc npc, List<Consideration> considerations) {
        StringBuilder sb = new StringBuilder();
        for (Consideration c : considerations) {
            sb.append(c.name()).append("=").append(String.format("%.2f", c.score(npc))).append(" ");
        }
        return sb.toString();
    }
}
