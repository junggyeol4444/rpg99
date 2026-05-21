package kr.reborn.npc.ai.behavior;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.Behavior;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * 결혼한 NPC 부부가 함께 있을 때 일정 확률로 자녀 NPC를 생성.
 * - 결혼 7일 후 가능
 * - 30일 쿨다운
 * - 매 사이클 1% 확률
 * - 자녀는 부모 stat의 5% 보너스 + 같은 세력
 */
public final class ChildbirthBehavior implements Behavior {

    private final RebornNPC plugin;

    public ChildbirthBehavior(RebornNPC plugin) { this.plugin = plugin; }

    @Override public String id() { return "childbirth"; }

    @Override public int priority(RebornNpc npc) {
        if (npc.dead) return 0;
        if (npc.spouseNpcId == null || npc.spouseNpcId.isEmpty()) return 0;
        long now = System.currentTimeMillis();
        if (now - npc.lastMarriageAt < 86_400_000L * 7) return 0; // 결혼 7일 후
        if (now - npc.lastChildAt < 86_400_000L * 30) return 0;   // 30일 쿨
        return 15; // idle보다 약간 높음
    }

    @Override public boolean tick(RebornNpc npc) {
        if (!Rand.chance(0.01)) return true;
        RebornNpc spouse = plugin.registry().get(npc.spouseNpcId);
        if (spouse == null || spouse.location == null) return true;
        // 둘이 같은 월드 + 5블록 내
        if (spouse.location.getWorld() != npc.location.getWorld()) return true;
        if (spouse.location.distance(npc.location) > 5) return true;

        String childId = npc.id + "_c" + (System.currentTimeMillis() % 100000);
        String childName = "§d" + npc.displayName.replaceAll("§.", "") + "의 자녀";
        Location at = npc.location.clone().add(Rand.rangeD(-1, 1), 0, Rand.rangeD(-1, 1));
        RebornNpc child = plugin.registry().spawn(
                childId, childName, npc.world, at, npc.faction, "VILLAGER");
        if (child != null) {
            // 부모 stat 5% 상속
            for (var e : npc.stats.entrySet()) {
                child.stats.merge(e.getKey(), e.getValue() * 0.025, Double::sum);
            }
            for (var e : spouse.stats.entrySet()) {
                child.stats.merge(e.getKey(), e.getValue() * 0.025, Double::sum);
            }
            child.home = npc.home;
            npc.children.add(childId);
            spouse.children.add(childId);
            npc.lastChildAt = System.currentTimeMillis();
            spouse.lastChildAt = System.currentTimeMillis();
            Bukkit.broadcastMessage("§d§l[NPC 자녀] §f" + npc.displayName
                    + "과 " + spouse.displayName + "이(가) " + childName + "을(를) 얻었다!");
        }
        return true;
    }
}
