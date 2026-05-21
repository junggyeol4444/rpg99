package kr.reborn.npc.ai.behavior;

import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 친한 NPC가 살해당한 후, 살해자를 추적·공격하는 복수 행동.
 *
 * 트리거: aiData.put("revenge:target", killerUUID), aiData.put("revenge:until", expireMs)
 * 우선순위: 80 (Combat과 비슷, Flee보다는 낮음)
 * 살해자가 같은 월드에 있을 때만 동작.
 */
public final class RevengeBehavior implements Behavior {

    private final RebornNPC plugin;

    public RevengeBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "revenge"; }
    @Override public String category() { return "AGGRO"; }

    @Override public int priority(RebornNpc npc) {
        return (int) (utility(npc) * 100);
    }

    /**
     * Utility = target 존재 × 분노 × ANGER 가중치 (성격)
     * 평범한 NPC라도 가족 살해된 경우 utility 1.0 (priority 100 목표가 부스트).
     */
    @Override public double utility(RebornNpc npc) {
        if (npc.dead) return 0;
        UUID target = (UUID) npc.aiData.get("revenge:target");
        if (target == null) return 0;
        Long until = (Long) npc.aiData.get("revenge:until");
        if (until == null || System.currentTimeMillis() > until) {
            npc.aiData.remove("revenge:target");
            npc.aiData.remove("revenge:until");
            return 0;
        }
        Player p = Bukkit.getPlayer(target);
        if (p == null || npc.bukkitEntityId == null) return 0;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (ent == null || p.getWorld() != ent.getWorld()) return 0;
        double base = 0.85;
        if (npc.soul != null) {
            // AVENGE 목표 있으면 +0.15
            for (var g : npc.goals) {
                if (g.kind == kr.reborn.npc.soul.GoalKind.AVENGE
                        && g.target.equals(target.toString())) {
                    base = 1.0;
                    break;
                }
            }
        }
        return base;
    }

    @Override public boolean tick(RebornNpc npc) {
        UUID id = (UUID) npc.aiData.get("revenge:target");
        if (id == null) return true;
        Player target = Bukkit.getPlayer(id);
        if (target == null) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        double dist = mob.getLocation().distance(target.getLocation());
        if (dist > 1.8) {
            mob.getPathfinder().moveTo(target, 1.3);
        } else {
            Long next = (Long) npc.aiData.get("revenge:nextAttack");
            if (next == null || System.currentTimeMillis() >= next) {
                double dmg = npc.stats.getOrDefault("STRENGTH", 5.0) * 1.5; // 복수 +50%
                target.damage(dmg, mob);
                mob.swingMainHand();
                npc.emotion.add(Emotion.Kind.ANGER, -5);
                npc.aiData.put("revenge:nextAttack", System.currentTimeMillis() + 1200);
            }
        }
        return false;
    }
}
