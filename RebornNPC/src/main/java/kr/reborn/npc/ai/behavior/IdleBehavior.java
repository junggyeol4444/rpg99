package kr.reborn.npc.ai.behavior;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

/**
 * 가장 낮은 우선순위 — 다른 행동이 없을 때 무작위로 돌아다님.
 * 5~15초마다 8블록 이내 랜덤 지점으로 이동.
 */
public final class IdleBehavior implements Behavior {

    @Override public String id() { return "idle"; }
    @Override public String category() { return "BASE"; }

    @Override public int priority(RebornNpc npc) {
        return (int) (utility(npc) * 100);
    }

    /** Idle은 항상 매우 낮은 baseline utility (0.05). 다른 모두 0일 때만 선택됨. */
    @Override public double utility(RebornNpc npc) {
        if (npc.dead) return 0;
        return 0.05;
    }

    @Override public void start(RebornNpc npc) {
        npc.state = NpcState.IDLE;
        scheduleWander(npc);
    }

    @Override public boolean tick(RebornNpc npc) {
        Long next = (Long) npc.aiData.get("idle:nextAt");
        if (next == null || System.currentTimeMillis() >= next) {
            scheduleWander(npc);
        }
        return false; // 계속 진행
    }

    private void scheduleWander(RebornNpc npc) {
        if (npc.bukkitEntityId == null || npc.location == null) return;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (ent instanceof Mob mob) {
            // 현재 위치 기준 ±6블록
            Location dest = mob.getLocation().clone().add(
                    Rand.rangeD(-6, 6), 0, Rand.rangeD(-6, 6));
            mob.getPathfinder().moveTo(dest, 0.6);
        }
        npc.aiData.put("idle:nextAt", System.currentTimeMillis() + Rand.range(5000, 15000));
    }
}
