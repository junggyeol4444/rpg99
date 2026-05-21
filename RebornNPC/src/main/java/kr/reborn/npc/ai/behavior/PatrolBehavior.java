package kr.reborn.npc.ai.behavior;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

/**
 * 경비병(GUARD) 직업의 순찰 행동.
 * - 스폰 지점 기준 ±15블록 영역을 순찰
 * - 경비 상태에서 적대 엔티티 발견 시 CombatBehavior에 위임
 */
public final class PatrolBehavior implements Behavior {

    @Override public String id() { return "patrol"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead) return 0;
        if ("GUARD".equals(npc.job) || "PATROL".equals(npc.job)
                || "WORLD_AI_PATROL".equals(npc.aiData.get("override-job"))) {
            return 25;
        }
        return 0;
    }

    @Override public void start(RebornNpc npc) {
        npc.state = NpcState.PATROL;
    }

    @Override public boolean tick(RebornNpc npc) {
        if (npc.bukkitEntityId == null || npc.location == null) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        Long next = (Long) npc.aiData.get("patrol:nextAt");
        if (next == null || System.currentTimeMillis() >= next) {
            Location dest = npc.location.clone().add(
                    Rand.rangeD(-15, 15), 0, Rand.rangeD(-15, 15));
            mob.getPathfinder().moveTo(dest, 0.8);
            npc.aiData.put("patrol:nextAt", System.currentTimeMillis() + Rand.range(5000, 12000));
        }
        return false;
    }
}
