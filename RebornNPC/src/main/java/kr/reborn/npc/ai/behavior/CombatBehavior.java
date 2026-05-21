package kr.reborn.npc.ai.behavior;

import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 전투 행동.
 * - 분노 > 80 또는 적대 플레이어 근처에 있을 때 트리거
 * - 가장 가까운 적 추적 + 공격 (1.5블록 내)
 * - 공격 사이클: 1.5초마다 damage()
 * - HP 20% 이하 → fleeing 전환 (FleeBehavior에 위임)
 */
public final class CombatBehavior implements Behavior {

    private final RebornNPC plugin;

    public CombatBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "combat"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead) return 0;
        if (npc.emotion.get(Emotion.Kind.ANGER) > 80) return 90;
        // 적대 플레이어가 시야 내
        if (findTarget(npc) != null) return 85;
        return 0;
    }

    @Override public void start(RebornNpc npc) {
        npc.state = NpcState.COMBAT;
    }

    @Override public boolean tick(RebornNpc npc) {
        if (npc.bukkitEntityId == null) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        // HP 20% 이하 → 도주 위임
        if (mob.getHealth() / mob.getMaxHealth() < 0.2) {
            npc.emotion.set(Emotion.Kind.FEAR, 80);
            return true; // FleeBehavior가 받음
        }

        LivingEntity target = findTarget(npc);
        if (target == null || target.isDead()) {
            // 적이 없으면 분노 감쇠 시작 + 종료
            npc.emotion.set(Emotion.Kind.ANGER, npc.emotion.get(Emotion.Kind.ANGER) - 10);
            return true;
        }

        double dist = mob.getLocation().distance(target.getLocation());
        if (dist > 1.8) {
            mob.getPathfinder().moveTo(target, 1.2);
        } else {
            // 공격: 1.5초 쿨다운
            Long nextAttack = (Long) npc.aiData.get("combat:nextAttack");
            if (nextAttack == null || System.currentTimeMillis() >= nextAttack) {
                double dmg = npc.stats.getOrDefault("STRENGTH", 5.0);
                target.damage(Math.max(1, dmg), mob);
                mob.swingMainHand();
                npc.aiData.put("combat:nextAttack", System.currentTimeMillis() + 1500);
            }
        }
        return false;
    }

    @Override public void stop(RebornNpc npc) {
        npc.aiData.remove("combat:nextAttack");
    }

    private LivingEntity findTarget(RebornNpc npc) {
        if (npc.bukkitEntityId == null) return null;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (ent == null) return null;
        LivingEntity best = null;
        double bestDist = 16;
        for (var e : ent.getNearbyEntities(16, 8, 16)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.getUniqueId().equals(npc.bukkitEntityId)) continue;
            // 플레이어 적대 체크
            if (le instanceof Player p && !npc.isHostileTo(p.getUniqueId())) continue;
            // 다른 NPC면 같은 세력이면 스킵
            var other = plugin.registry().byEntity(le.getUniqueId());
            if (other != null) {
                if (!npc.faction.isEmpty() && npc.faction.equals(other.faction)) continue;
                if (!npc.isHostileTo(java.util.UUID.fromString(other.id.replaceAll("[^a-f0-9-]", "0").substring(0, Math.min(36, other.id.length()))))) {
                    // 같은 세력이 아니라도 분노 < 80이면 패스
                    if (npc.emotion.get(Emotion.Kind.ANGER) < 60) continue;
                }
            }
            double d = ent.getLocation().distance(le.getLocation());
            if (d < bestDist) { bestDist = d; best = le; }
        }
        return best;
    }
}
