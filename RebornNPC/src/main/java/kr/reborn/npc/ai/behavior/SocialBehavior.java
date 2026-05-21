package kr.reborn.npc.ai.behavior;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

/**
 * 호기심 > 60일 때 근처 NPC와 자율 대화·관계도 갱신.
 *
 * 같은 세력 NPC를 만나면 +0.5 관계도, 다른 세력이면 -0.5.
 * 호감도 80+ 둘이 만나면 결혼 이벤트 등록 (RebornClan 연동).
 */
public final class SocialBehavior implements Behavior {

    private final RebornNPC plugin;

    public SocialBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "social"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead) return 0;
        if (npc.emotion.get(Emotion.Kind.CURIOSITY) > 60) return 30;
        // 호감도 높은 동성/이성 NPC가 근처에 있으면 자동 사회 활동
        return findNearby(npc) != null ? 25 : 0;
    }

    @Override public boolean tick(RebornNpc npc) {
        RebornNpc partner = findNearby(npc);
        if (partner == null) {
            npc.emotion.set(Emotion.Kind.CURIOSITY, npc.emotion.get(Emotion.Kind.CURIOSITY) - 5);
            return true;
        }
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        double dist = mob.getLocation().distance(partner.location);
        if (dist > 3) {
            mob.getPathfinder().moveTo(partner.location, 0.8);
            return false;
        }
        // 대화 (관계도 갱신은 cooldown)
        Long next = (Long) npc.aiData.get("social:nextChat");
        if (next == null || System.currentTimeMillis() >= next) {
            boolean sameFaction = !npc.faction.isEmpty() && npc.faction.equals(partner.faction);
            double delta = sameFaction ? +1 : -1;
            npc.relations.addNpc(partner.id, delta);
            partner.relations.addNpc(npc.id, delta);

            // 결혼 가능 여부 체크 (호감도 80+ + 양쪽 미혼)
            if (npc.relations.npc(partner.id) >= 80
                    && partner.relations.npc(npc.id) >= 80
                    && npc.spouseNpcId.isEmpty()
                    && partner.spouseNpcId.isEmpty()
                    && System.currentTimeMillis() - npc.lastMarriageAt > 86_400_000L) {
                marry(npc, partner);
            }

            npc.aiData.put("social:nextChat", System.currentTimeMillis() + Rand.range(10000, 30000));
            npc.emotion.add(Emotion.Kind.HAPPINESS, 2);
        }
        return false;
    }

    private RebornNpc findNearby(RebornNpc npc) {
        if (npc.bukkitEntityId == null || npc.location == null) return null;
        RebornNpc best = null;
        double bestD = 8;
        for (RebornNpc other : plugin.registry().all()) {
            if (other == npc || other.dead) continue;
            if (other.location == null || other.location.getWorld() != npc.location.getWorld()) continue;
            double d = other.location.distance(npc.location);
            if (d < bestD && npc.relations.npc(other.id) > -50) {
                bestD = d; best = other;
            }
        }
        return best;
    }

    private void marry(RebornNpc a, RebornNpc b) {
        a.spouseNpcId = b.id;
        b.spouseNpcId = a.id;
        a.lastMarriageAt = System.currentTimeMillis();
        b.lastMarriageAt = System.currentTimeMillis();
        Bukkit.broadcastMessage("§d§l[NPC 결혼] §f" + a.displayName + " ❤ " + b.displayName);
    }
}
