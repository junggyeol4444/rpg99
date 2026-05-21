package kr.reborn.npc.ai.behavior;

import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Goal;
import kr.reborn.npc.soul.GoalKind;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

/**
 * 활성 목표를 추구하는 행동.
 *
 * 가장 priority 높은 활성 목표를 선택, 그 목표 종류에 맞는 구체적 행동을 수행:
 *   GAIN_POWER       → 강한 같은 세력 NPC 옆에 머무름 (정치)
 *   SERVE_LORD       → target NPC 위치로 이동
 *   ASCEND           → 명상 자세 (수련)
 *   FIND_LOVE        → 가까운 매력적 NPC에게 접근 (Social에 위임)
 *   AVENGE           → target 추적 (RevengeBehavior가 이미 처리, 여기는 sync)
 *   PROTECT_FAMILY   → 가족 NPC 근처 머무름
 *   EXPLORE          → 멀리 가본 적 없는 방향
 *   HIDE             → 인적 드문 곳으로
 *   FOUND_TOWN       → 외딴 곳 정착
 *   MASTER_ART       → 명상 (수련 자세)
 *   ACCUMULATE_KNOWLEDGE → 책 NPC·도서관 근처
 *
 * 우선순위: 35 (Schedule 위, Combat·Flee 아래) — 위급 상황은 무시되지 않음.
 */
public final class PursueGoalBehavior implements Behavior {

    private final RebornNPC plugin;

    public PursueGoalBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "pursue_goal"; }
    @Override public String category() { return "GOAL"; }

    @Override public int priority(RebornNpc npc) {
        return (int) (utility(npc) * 100);
    }

    /**
     * Utility = 활성 목표 priority / 100 × 진행 부족 (0% → 1.0, 100% → 0)
     * 목표 priority 100인 AVENGE는 utility ~1.0
     * 목표 priority 50인 GAIN_WEALTH는 utility ~0.4
     */
    @Override public double utility(RebornNpc npc) {
        if (npc.dead || npc.soul == null || npc.goals.isEmpty()) return 0;
        Goal top = topActive(npc);
        if (top == null) return 0;
        double priScore = top.priority / 100.0;
        double remaining = (100.0 - top.progress) / 100.0;
        // 거의 끝났으면 utility↓ — 다른 목표로 자연 전환
        double tailBoost = top.progress > 90 ? 0.7 : 1.0;
        return Math.min(0.7, priScore * remaining * tailBoost);
        // 0.7 cap — 위급(공포·복수·전투)는 항상 우선
    }

    @Override public void start(RebornNpc npc) {
        npc.state = NpcState.WORK;
    }

    @Override public boolean tick(RebornNpc npc) {
        Goal g = topActive(npc);
        if (g == null) return true;
        if (npc.bukkitEntityId == null) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        switch (g.kind) {
            case SERVE_LORD: {
                RebornNpc lord = plugin.registry().get(g.target);
                if (lord == null || lord.dead || lord.location == null) return true;
                if (mob.getLocation().distance(lord.location) > 8) {
                    mob.getPathfinder().moveTo(lord.location, 0.9);
                } else {
                    // 옆에서 호위 자세 — LOYALTY 욕구 충족
                    npc.soul.needs.add(kr.reborn.npc.soul.Needs.Kind.STATUS, +0.2);
                }
                break;
            }
            case GAIN_POWER: {
                // 같은 세력의 가장 강한 NPC 근처에 머무름 (정치)
                RebornNpc strong = findStrongestSameFaction(npc);
                if (strong != null && strong.location != null) {
                    if (mob.getLocation().distance(strong.location) > 10) {
                        mob.getPathfinder().moveTo(strong.location, 0.7);
                    }
                }
                break;
            }
            case ASCEND:
            case MASTER_ART: {
                // 명상 자세 — 천천히 진행
                npc.soul.needs.add(kr.reborn.npc.soul.Needs.Kind.MASTERY, +0.5);
                npc.soul.needs.add(kr.reborn.npc.soul.Needs.Kind.AUTONOMY, +0.2);
                // 가만히 — wander 안 함
                break;
            }
            case PROTECT_FAMILY: {
                // 가까운 가족 NPC 옆에
                RebornNpc family = findClosestFamily(npc);
                if (family != null && family.location != null) {
                    if (mob.getLocation().distance(family.location) > 12) {
                        mob.getPathfinder().moveTo(family.location, 0.8);
                    }
                }
                break;
            }
            case EXPLORE: {
                // 매우 멀리 이동 (200블록 밖)
                Long next = (Long) npc.aiData.get("goal:exploreAt");
                if (next == null || System.currentTimeMillis() >= next) {
                    Location far = mob.getLocation().clone().add(
                            kr.reborn.core.util.Rand.rangeD(-50, 50), 0,
                            kr.reborn.core.util.Rand.rangeD(-50, 50));
                    mob.getPathfinder().moveTo(far, 0.9);
                    npc.aiData.put("goal:exploreAt", System.currentTimeMillis() + 30_000L);
                }
                break;
            }
            case HIDE: {
                // 모든 사람·NPC 반대 방향
                kr.reborn.npc.entity.RebornNpc nearest = plugin.registry().nearest(mob.getLocation(), 30);
                if (nearest != null && nearest.location != null) {
                    var vec = mob.getLocation().toVector().subtract(nearest.location.toVector()).normalize().multiply(15);
                    mob.getPathfinder().moveTo(mob.getLocation().clone().add(vec), 0.9);
                }
                break;
            }
            case FOUND_TOWN: {
                // 인적 드문 곳 — 다른 NPC가 없는 방향
                kr.reborn.npc.entity.RebornNpc nearest = plugin.registry().nearest(mob.getLocation(), 50);
                if (nearest != null && nearest.location != null
                        && mob.getLocation().distance(nearest.location) < 30) {
                    var vec = mob.getLocation().toVector().subtract(nearest.location.toVector()).normalize().multiply(40);
                    mob.getPathfinder().moveTo(mob.getLocation().clone().add(vec), 0.8);
                }
                break;
            }
            case AVENGE:
            case DEFEAT_RIVAL: {
                // RevengeBehavior가 처리 — 여기서는 aiData 동기화만
                if (!g.target.isEmpty()) {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(g.target);
                        npc.aiData.putIfAbsent("revenge:target", id);
                        npc.aiData.putIfAbsent("revenge:until", System.currentTimeMillis() + 3_600_000L);
                    } catch (Exception ignored) {}
                }
                break;
            }
            default:
                break;
        }
        // 한 tick에 끝내지 않음 — 목표가 사라지거나 더 높은 우선순위 행동 전환될 때까지
        return false;
    }

    private Goal topActive(RebornNpc npc) {
        Goal best = null;
        int bestPri = -1;
        for (Goal g : npc.goals) {
            if (!g.isActive()) continue;
            if (g.priority > bestPri) { bestPri = g.priority; best = g; }
        }
        return best;
    }

    private RebornNpc findStrongestSameFaction(RebornNpc self) {
        if (self.faction.isEmpty()) return null;
        RebornNpc best = null;
        double bestT = self.effectiveTotal();
        for (RebornNpc other : plugin.registry().all()) {
            if (other == self || other.dead) continue;
            if (!self.faction.equals(other.faction)) continue;
            if (other.effectiveTotal() > bestT) { bestT = other.effectiveTotal(); best = other; }
        }
        return best;
    }

    private RebornNpc findClosestFamily(RebornNpc self) {
        if (self.soul == null || self.soul.family.isEmpty()) return null;
        RebornNpc best = null;
        double bestD = 9999;
        for (String fid : self.soul.family) {
            RebornNpc f = plugin.registry().get(fid);
            if (f == null || f.dead || f.location == null) continue;
            if (f.location.getWorld() != self.location.getWorld()) continue;
            double d = f.location.distance(self.location);
            if (d < bestD) { bestD = d; best = f; }
        }
        return best;
    }
}
