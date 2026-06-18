package kr.reborn.npc.faction;

import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Location;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * NPC 자생 세력. 지도자 1명 + 구성원 다수.
 *
 * 관계망(SocialNetwork)·소문(Reputation) 위에 얹히는 "집단" 단위.
 * 이념(구성원 평균 성격)으로 외교 유사도를 계산해 동맹·전쟁을 결정.
 */
public final class Faction {

    public final String id;
    public String name;
    public String leaderId;
    public final Set<String> members = new HashSet<>();

    /** 영토 중심 (Step 1.6 월드 임팩트에서 실제 마을로 구현). */
    public Location territoryCenter;
    public double territoryRadius = 48;
    /** 국고 — 세금·정복·교역으로 누적. */
    public double treasury = 0;
    public final long foundedAt = System.currentTimeMillis();

    /** 다른 세력 id → 외교 태도. */
    public final Map<String, FactionStance> relations = new HashMap<>();
    /** 이념 — 구성원 평균 성격 (외교 유사도 계산용). */
    public final EnumMap<Personality.Trait, Double> ideology = new EnumMap<>(Personality.Trait.class);

    public Faction(String id, String name, String leaderId) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        members.add(leaderId);
    }

    public int size() { return members.size(); }
    public boolean isLeader(String npcId) { return leaderId.equals(npcId); }

    public FactionStance stanceToward(String otherFactionId) {
        return relations.getOrDefault(otherFactionId, FactionStance.NEUTRAL);
    }

    /** 구성원 평균 성격으로 이념 재계산. */
    public void recomputeIdeology(Function<String, RebornNpc> lookup) {
        EnumMap<Personality.Trait, Double> sum = new EnumMap<>(Personality.Trait.class);
        int n = 0;
        for (String mid : members) {
            RebornNpc m = lookup.apply(mid);
            if (m == null || m.soul == null) continue;
            for (Personality.Trait t : Personality.Trait.values()) {
                sum.merge(t, (double) m.soul.personality.get(t), Double::sum);
            }
            n++;
        }
        ideology.clear();
        if (n == 0) return;
        for (Personality.Trait t : Personality.Trait.values()) {
            ideology.put(t, sum.getOrDefault(t, 0.0) / n);
        }
    }

    /** 두 세력 이념 유사도 0(정반대)~1(동일). */
    public double ideologySimilarity(Faction other) {
        if (ideology.isEmpty() || other.ideology.isEmpty()) return 0.5;
        double totalDiff = 0;
        int n = 0;
        for (Personality.Trait t : Personality.Trait.values()) {
            double a = ideology.getOrDefault(t, 0.0);
            double b = other.ideology.getOrDefault(t, 0.0);
            totalDiff += Math.abs(a - b);
            n++;
        }
        double avgDiff = totalDiff / n;  // 0~200
        return Math.max(0, 1 - avgDiff / 200.0);
    }

    /** 세력 총 전투력 — 살아있는 구성원 effectiveTotal 합. */
    public double power(Function<String, RebornNpc> lookup) {
        double p = 0;
        for (String mid : members) {
            RebornNpc m = lookup.apply(mid);
            if (m != null && !m.dead) p += m.effectiveTotal();
        }
        return p;
    }
}
