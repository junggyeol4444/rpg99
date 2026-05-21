package kr.reborn.npc.ai.behavior;

import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Memory;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/**
 * 성격·기억 기반 전투 의사결정.
 *
 * 단순 "분노 80 → 공격"이 아님:
 *   - AGGRESSION 높음 → 작은 도발에도 공격
 *   - AGGRESSION 낮음 → 큰 분노에도 회피
 *   - PRIDE 높음 → 모욕(INSULTED_ME) 기억만으로도 공격
 *   - EMPATHY 높음 → 다친 적도 봐줌
 *   - BRAVERY 낮음 → 강한 적은 도주
 *   - LOYALTY 높음 → 친구·가족 위협받으면 즉시 가담
 *
 * 누구를 공격할지도 성격이 결정 — 가장 미워하는 대상 우선.
 */
public final class CombatBehavior implements Behavior {

    private final RebornNPC plugin;

    public CombatBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "combat"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead || npc.soul == null) return 0;
        var p = npc.soul.personality;

        int angerThreshold = 80 - p.get(Personality.Trait.AGGRESSION) / 2
                + p.get(Personality.Trait.EMPATHY) / 3;
        if (npc.emotion.get(Emotion.Kind.ANGER) > angerThreshold) {
            return 85 + p.get(Personality.Trait.AGGRESSION) / 10;
        }
        if (p.get(Personality.Trait.LOYALTY) > 40 && allyInDanger(npc)) {
            return 80;
        }
        LivingEntity hated = pickBestTarget(npc);
        if (hated != null) {
            int base = 70;
            if (hated instanceof Player pl) {
                double sent = npc.soul.relationToward(pl.getUniqueId().toString());
                if (sent < -50) base = 90;
            }
            return base;
        }
        return 0;
    }

    @Override public void start(RebornNpc npc) {
        npc.state = NpcState.COMBAT;
    }

    @Override public boolean tick(RebornNpc npc) {
        if (npc.bukkitEntityId == null) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        var pers = npc.soul.personality;
        double fleeThreshold = 0.10 + (40 - pers.get(Personality.Trait.BRAVERY)) * 0.005;
        if (mob.getHealth() / mob.getMaxHealth() < fleeThreshold) {
            npc.emotion.set(Emotion.Kind.FEAR, 80);
            return true;
        }

        LivingEntity target = pickBestTarget(npc);
        if (target == null || target.isDead()) {
            npc.emotion.set(Emotion.Kind.ANGER, npc.emotion.get(Emotion.Kind.ANGER) - 10);
            return true;
        }

        double dist = mob.getLocation().distance(target.getLocation());
        if (dist > 1.8) {
            mob.getPathfinder().moveTo(target, 1.2);
        } else {
            Long nextAttack = (Long) npc.aiData.get("combat:nextAttack");
            if (nextAttack == null || System.currentTimeMillis() >= nextAttack) {
                double dmg = npc.stats.getOrDefault("STRENGTH", 5.0);
                dmg *= 1 + pers.get(Personality.Trait.AGGRESSION) / 500.0;
                target.damage(Math.max(1, dmg), mob);
                mob.swingMainHand();
                npc.soul.needs.add(kr.reborn.npc.soul.Needs.Kind.ACHIEVEMENT, +0.5);
                npc.aiData.put("combat:nextAttack", System.currentTimeMillis() + 1500);

                if (target instanceof Player p) {
                    npc.soul.memory.record(p.getUniqueId().toString(),
                            Memory.Kind.ATTACKED_ME, 20, "전투");
                }
            }
        }
        return false;
    }

    @Override public void stop(RebornNpc npc) {
        npc.aiData.remove("combat:nextAttack");
    }

    private boolean allyInDanger(RebornNpc npc) {
        if (npc.bukkitEntityId == null) return false;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (ent == null) return false;
        for (var e : ent.getNearbyEntities(10, 6, 10)) {
            if (!(e instanceof Mob other)) continue;
            var otherNpc = plugin.registry().byEntity(e.getUniqueId());
            if (otherNpc == null) continue;
            if (npc.faction.equals(otherNpc.faction) && other.getHealth() < other.getMaxHealth() * 0.5) {
                return true;
            }
        }
        return false;
    }

    private LivingEntity pickBestTarget(RebornNpc npc) {
        if (npc.bukkitEntityId == null) return null;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (ent == null) return null;
        LivingEntity best = null;
        double bestScore = -1;
        var pers = npc.soul.personality;
        for (var e : ent.getNearbyEntities(16, 8, 16)) {
            if (!(e instanceof LivingEntity le) || le.getUniqueId().equals(npc.bukkitEntityId)) continue;
            double score;
            if (le instanceof Player p) {
                double sent = npc.soul.relationToward(p.getUniqueId().toString());
                if (sent > -20) continue;
                score = -sent;
            } else {
                var other = plugin.registry().byEntity(le.getUniqueId());
                if (other != null) {
                    if (npc.faction.equals(other.faction) && !npc.faction.isEmpty()) continue;
                    double sent = npc.soul.relationToward(other.id);
                    if (sent > -20) continue;
                    score = -sent;
                } else {
                    if (pers.get(Personality.Trait.AGGRESSION) < 20) continue;
                    score = 20;
                }
            }
            if (ent instanceof Mob m
                    && le.getMaxHealth() > m.getMaxHealth() * 1.5
                    && pers.get(Personality.Trait.BRAVERY) < 30) continue;
            double dist = ent.getLocation().distance(le.getLocation());
            score -= dist;
            if (score > bestScore) { bestScore = score; best = le; }
        }
        return best;
    }
}
