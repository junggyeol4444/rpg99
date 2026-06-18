package kr.reborn.npc.ai;

import kr.reborn.npc.entity.RebornNpc;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 의사결정 컨트롤러 (Utility-based).
 *
 * 매 AI tick:
 *   1) 모든 Behavior의 utility() 계산
 *   2) 가장 높은 utility 선택
 *   3) 현재 행동 진행 중이면 utility 차이가 임계 이상일 때만 전환
 *      - 같은 category면 작은 차이도 전환 (5%)
 *      - 다른 category면 큰 차이 필요 (20%)
 *
 * 이로써:
 *   - 같은 상황도 NPC마다 성격에 따라 다른 행동 (utility 다르게 계산됨)
 *   - 부드러운 전환 (hysteresis로 chattering 방지)
 *   - 명확한 디버그 (각 Behavior utility 출력 가능)
 */
public final class NpcBrain {

    private final RebornNpc npc;
    private final List<Behavior> behaviors = new ArrayList<>();
    private Behavior current;
    private double currentUtility;
    private long currentStartedAt;

    /** 디버그용 — 마지막 결정 시 모든 Behavior의 utility 스냅샷. */
    public final java.util.Map<String, Double> lastScores = new java.util.LinkedHashMap<>();

    public NpcBrain(RebornNpc npc) { this.npc = npc; }

    public void register(Behavior b) { behaviors.add(b); }

    public Behavior current() { return current; }
    public double currentUtility() { return currentUtility; }

    /** 매 AI tick. */
    public void tick() {
        // 1. 현재 행동 진행
        if (current != null) {
            boolean done = current.tick(npc);
            if (done) {
                current.stop(npc);
                current = null;
            }
        }

        // 2. 모든 Behavior utility 계산
        lastScores.clear();
        Behavior best = null;
        double bestUtil = 0;
        for (Behavior b : behaviors) {
            double u = b.utility(npc);
            lastScores.put(b.id(), u);
            if (u > bestUtil) { bestUtil = u; best = b; }
        }
        if (best == null || bestUtil <= 0) {
            if (current != null) { current.stop(npc); current = null; }
            return;
        }

        // 3. 전환 결정 (hysteresis)
        if (current == null) {
            current = best;
            currentUtility = bestUtil;
            currentStartedAt = System.currentTimeMillis();
            current.start(npc);
            return;
        }
        if (current == best) {
            currentUtility = bestUtil;
            return;
        }
        // 다른 행동이 선호됨 — 전환 임계 체크
        boolean sameCategory = current.category().equals(best.category());
        double threshold = sameCategory ? 0.05 : 0.20;
        // 최소 2초는 같은 행동 유지 (instant switch 방지)
        long minHold = sameCategory ? 1000 : 2500;
        if (System.currentTimeMillis() - currentStartedAt < minHold) return;
        if (bestUtil - currentUtility >= threshold) {
            current.stop(npc);
            current = best;
            currentUtility = bestUtil;
            currentStartedAt = System.currentTimeMillis();
            current.start(npc);
        }
    }
}
