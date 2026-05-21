package kr.reborn.npc.ai.behavior;

import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

/**
 * 공포 > 70일 때 가장 가까운 위협의 반대 방향으로 도주.
 * 5초마다 새 도주 지점 갱신.
 * 공포 < 40으로 떨어지면 종료.
 */
public final class FleeBehavior implements Behavior {

    @Override public String id() { return "flee"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead) return 0;
        if (npc.emotion.get(Emotion.Kind.FEAR) > 70) return 95;
        return 0;
    }

    @Override public void start(RebornNpc npc) {
        npc.state = NpcState.FLEE;
    }

    @Override public boolean tick(RebornNpc npc) {
        if (npc.emotion.get(Emotion.Kind.FEAR) < 40) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        Long next = (Long) npc.aiData.get("flee:nextAt");
        if (next == null || System.currentTimeMillis() >= next) {
            // 가장 가까운 LivingEntity (위협)의 반대 방향
            LivingEntity threat = null;
            double bestD = 12;
            for (var e : mob.getNearbyEntities(12, 6, 12)) {
                if (e instanceof LivingEntity le && !le.getUniqueId().equals(npc.bukkitEntityId)) {
                    double d = mob.getLocation().distance(le.getLocation());
                    if (d < bestD) { bestD = d; threat = le; }
                }
            }
            if (threat != null) {
                Vector away = mob.getLocation().toVector()
                        .subtract(threat.getLocation().toVector()).normalize().multiply(8);
                Location target = mob.getLocation().clone().add(away);
                mob.getPathfinder().moveTo(target, 1.4);
            }
            // 공포 자연 감쇠
            npc.emotion.set(Emotion.Kind.FEAR, npc.emotion.get(Emotion.Kind.FEAR) - 5);
            npc.aiData.put("flee:nextAt", System.currentTimeMillis() + 3000);
        }
        return false;
    }
}
