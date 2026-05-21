package kr.reborn.npc.ai.behavior;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Memory;
import kr.reborn.npc.soul.Needs;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

/**
 * 성격·욕구 기반 사회 행동.
 * - SOCIABILITY 높음 → 자주 다가가 대화
 * - SOCIABILITY 낮음 → 혼자 있길 선호
 * - COMPANIONSHIP 욕구 낮을수록 (외로움) 사회 행동 우선
 * - 가치관 차이 클수록 충돌, 비슷할수록 우호
 * - 결혼은 LOVE 욕구 + 친밀도 + 가치관 일치 + 양쪽 미혼
 */
public final class SocialBehavior implements Behavior {

    private final RebornNPC plugin;

    public SocialBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "social"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead || npc.soul == null) return 0;
        var p = npc.soul.personality;
        if (p.get(Personality.Trait.SOCIABILITY) < -50) return 0;

        double comp = npc.soul.needs.get(Needs.Kind.COMPANIONSHIP);
        double love = npc.soul.needs.get(Needs.Kind.LOVE);
        int base = (int) Math.max(0, 50 - comp / 2);
        if (love < 30) base += 10;
        base += p.get(Personality.Trait.SOCIABILITY) / 5;
        if (findNearby(npc) == null) base = Math.min(base, 20);
        return Math.min(base, 50);
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
        Long next = (Long) npc.aiData.get("social:nextChat");
        if (next == null || System.currentTimeMillis() >= next) {
            interact(npc, partner);
            npc.aiData.put("social:nextChat", System.currentTimeMillis() + Rand.range(10000, 30000));
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
            double sent = npc.soul.relationToward(other.id);
            if (sent < -50) continue;
            double d = other.location.distance(npc.location);
            if (d < bestD) { bestD = d; best = other; }
        }
        return best;
    }

    private void interact(RebornNpc a, RebornNpc b) {
        var pa = a.soul.personality;
        var pb = b.soul.personality;
        int diff = Math.abs(pa.get(Personality.Trait.EMPATHY) - pb.get(Personality.Trait.EMPATHY))
                + Math.abs(pa.get(Personality.Trait.LOYALTY) - pb.get(Personality.Trait.LOYALTY))
                + Math.abs(pa.get(Personality.Trait.AGGRESSION) - pb.get(Personality.Trait.AGGRESSION));
        boolean sameFaction = !a.faction.isEmpty() && a.faction.equals(b.faction);

        if (pa.get(Personality.Trait.PRIDE) > 50 && pb.get(Personality.Trait.PRIDE) > 50 && diff > 100) {
            a.soul.memory.record(b.id, Memory.Kind.INSULTED_ME, 15, "충돌");
            b.soul.memory.record(a.id, Memory.Kind.INSULTED_ME, 15, "충돌");
            a.emotion.add(Emotion.Kind.ANGER, 10);
            b.emotion.add(Emotion.Kind.ANGER, 10);
            return;
        }
        if (diff < 100 && sameFaction) {
            a.soul.memory.record(b.id, Memory.Kind.HELPED_ME, 5, "대화");
            b.soul.memory.record(a.id, Memory.Kind.HELPED_ME, 5, "대화");
        }

        a.soul.needs.add(Needs.Kind.COMPANIONSHIP, +3);
        b.soul.needs.add(Needs.Kind.COMPANIONSHIP, +3);
        a.soul.reclassify(b.id);
        b.soul.reclassify(a.id);

        double senta = a.soul.relationToward(b.id);
        double sentb = b.soul.relationToward(a.id);
        if (senta > 60 && sentb > 60 && diff < 80
                && a.soul.needs.get(Needs.Kind.LOVE) < 50
                && b.soul.needs.get(Needs.Kind.LOVE) < 50
                && a.spouseNpcId.isEmpty() && b.spouseNpcId.isEmpty()
                && System.currentTimeMillis() - a.lastMarriageAt > 86_400_000L) {
            marry(a, b);
        }
        if (pa.get(Personality.Trait.AMBITION) > 40 && pb.get(Personality.Trait.AMBITION) > 40
                && !sameFaction && diff < 120) {
            a.soul.memory.record(b.id, Memory.Kind.ALLIED_WITH_ME, 10, "정치 동맹");
            b.soul.memory.record(a.id, Memory.Kind.ALLIED_WITH_ME, 10, "정치 동맹");
        }
    }

    private void marry(RebornNpc a, RebornNpc b) {
        a.spouseNpcId = b.id;
        b.spouseNpcId = a.id;
        a.lastMarriageAt = System.currentTimeMillis();
        b.lastMarriageAt = System.currentTimeMillis();
        a.soul.memory.record(b.id, Memory.Kind.MARRIED_ME, 90, "결혼");
        b.soul.memory.record(a.id, Memory.Kind.MARRIED_ME, 90, "결혼");
        a.soul.family.add(b.id);
        b.soul.family.add(a.id);
        a.soul.needs.add(Needs.Kind.LOVE, +50);
        b.soul.needs.add(Needs.Kind.LOVE, +50);
        Bukkit.broadcastMessage("§d§l[NPC 결혼] §f" + a.displayName + " ❤ " + b.displayName);
    }
}
