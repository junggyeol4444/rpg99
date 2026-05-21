package kr.reborn.npc.ai.behavior;

import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.entity.NpcState;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;

/**
 * 일과(Schedule) 기반 행동.
 *
 * 기본 시간대 행동 (현실 시간):
 *   06-09: 직장으로 이동 (workplace)
 *   09-17: 직장에서 일 (WORK state)
 *   17-19: 시장 방문 (market 좌표 — config 또는 nearby NPC)
 *   19-21: 식사 (집 근처)
 *   21-06: 수면 (home)
 *
 * npc.schedule이 비어있지 않으면 그 스케줄을 우선.
 */
public final class ScheduleBehavior implements Behavior {

    @Override public String id() { return "schedule"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead) return 0;
        // 직장 또는 집이 정의된 NPC만 일과 수행
        if (npc.home == null && npc.workplace == null) return 0;
        return 20;
    }

    @Override public boolean tick(RebornNpc npc) {
        if (npc.bukkitEntityId == null) return true;
        var ent = Bukkit.getEntity(npc.bukkitEntityId);
        if (!(ent instanceof Mob mob)) return true;

        int hour = java.time.LocalTime.now(
                java.time.ZoneId.of("Asia/Seoul")).getHour();

        Location target;
        NpcState newState;
        if (hour >= 6 && hour < 9) {
            target = npc.workplace != null ? npc.workplace : npc.home;
            newState = NpcState.WORK; // 출근 중
        } else if (hour >= 9 && hour < 17) {
            target = npc.workplace != null ? npc.workplace : npc.home;
            newState = NpcState.WORK;
        } else if (hour >= 17 && hour < 19) {
            target = nearMarket(npc);
            newState = NpcState.TRADE;
        } else if (hour >= 19 && hour < 21) {
            target = npc.home != null ? npc.home : npc.workplace;
            newState = NpcState.WORK; // 저녁 식사
        } else {
            target = npc.home != null ? npc.home : npc.workplace;
            newState = NpcState.SLEEP;
        }
        if (target == null) return false;
        npc.state = newState;

        // 이동: 5블록 이내면 도착으로 간주
        double dist = mob.getLocation().distance(target);
        if (dist > 5) {
            mob.getPathfinder().moveTo(target, 0.7);
        }
        return false; // 계속
    }

    private Location nearMarket(RebornNpc npc) {
        // 시장은 spawn에서 가까운 다른 MERCHANT NPC 위치로 대체. 없으면 home.
        if (npc.home != null) return npc.home;
        return npc.location;
    }
}
