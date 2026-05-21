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
    @Override public String category() { return "SOCIAL"; }

    @Override public int priority(RebornNpc npc) {
        return (int) (utility(npc) * 100);
    }

    /**
     * Utility = COMPANIONSHIP 부족 × SOCIABILITY × 근처 NPC 존재
     * + LOVE 욕구 부족 보너스
     * SOCIABILITY -50 이하면 거의 0 (은둔)
     */
    @Override public double utility(RebornNpc npc) {
        if (npc.dead || npc.soul == null) return 0;
        var p = npc.soul.personality;
        if (p.get(Personality.Trait.SOCIABILITY) < -50) return 0;
        double socScore = kr.reborn.npc.ai.utility.ResponseCurve.linear(
                p.get(Personality.Trait.SOCIABILITY), -50, 100);
        socScore = 0.2 + 0.8 * socScore;
        double compScore = kr.reborn.npc.ai.utility.ResponseCurve.linearInverted(
                npc.soul.needs.get(Needs.Kind.COMPANIONSHIP), 0, 80);
        double loveBonus = npc.soul.needs.get(Needs.Kind.LOVE) < 30 ? 1.3 : 1.0;
        double nearbyScore = findNearby(npc) != null ? 1.0 : 0.2;
        return Math.min(1.0, socScore * compScore * nearbyScore * loveBonus * 0.5);
        // ×0.5 캡 — 사회 행동은 위급보다는 낮은 우선순위
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
        // FIND_LOVE 목표 완료 이벤트
        plugin.registry().goalProgressor().onEvent(a,
                new kr.reborn.npc.soul.GoalProgressor.Event(
                        kr.reborn.npc.soul.GoalProgressor.EventKind.MARRIED, b.id));
        plugin.registry().goalProgressor().onEvent(b,
                new kr.reborn.npc.soul.GoalProgressor.Event(
                        kr.reborn.npc.soul.GoalProgressor.EventKind.MARRIED, a.id));
        Bukkit.broadcastMessage("§d§l[NPC 결혼] §f" + a.displayName + " ❤ " + b.displayName);
    }
}
