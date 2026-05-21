package kr.reborn.npc.ai;

import kr.reborn.npc.entity.RebornNpc;

import java.util.ArrayList;
import java.util.List;

/**
 * 한 NPC의 의사결정 컨트롤러.
 * 매 AI tick마다 등록된 Behavior 중 priority 가장 높은 것을 선택·실행.
 *
 * "최우선이 같은 경우" 현재 진행 중인 행동을 계속 (전환 비용 회피).
 */
public final class NpcBrain {

    private final RebornNpc npc;
    private final List<Behavior> behaviors = new ArrayList<>();
    private Behavior current;

    public NpcBrain(RebornNpc npc) { this.npc = npc; }

    public void register(Behavior b) { behaviors.add(b); }

    public Behavior current() { return current; }

    /** 매 AI tick마다 호출. */
    public void tick() {
        // 현재 행동 완료 체크
        if (current != null) {
            boolean done = current.tick(npc);
            if (!done) {
                // 더 높은 priority의 행동이 있는지 체크 (긴급 상황 처리)
                Behavior best = pickBest();
                if (best != null && best != current
                        && best.priority(npc) >= current.priority(npc) + 20) {
                    // 20+ 차이날 때만 전환 (chattering 방지)
                    current.stop(npc);
                    current = best;
                    current.start(npc);
                }
                return;
            }
            current.stop(npc);
            current = null;
        }
        // 새 행동 선택
        Behavior next = pickBest();
        if (next == null) return;
        current = next;
        current.start(npc);
    }

    private Behavior pickBest() {
        Behavior best = null;
        int bestP = 0;
        for (Behavior b : behaviors) {
            int p = b.priority(npc);
            if (p > bestP) { bestP = p; best = b; }
        }
        return best;
    }
}
