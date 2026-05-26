package kr.reborn.npc.social;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 전체 NPC 관계 그래프. 방향성 있음 (A→B가 MENTOR이면 B→A는 STUDENT).
 *
 * 명시적 관계만 저장 (가족·스승·주군·연인·동맹 등). 감정 수치는 Soul/Memory가 담당.
 * 소문 전파 경로 계산(친구의 친구...), 영향력 있는 NPC 탐색에 사용.
 */
public final class SocialNetwork {

    /** npcId → (상대 npcId → 관계 타입) */
    private final Map<String, Map<String, RelationshipType>> edges = new HashMap<>();

    public void setRelation(String a, String b, RelationshipType type) {
        edges.computeIfAbsent(a, k -> new HashMap<>()).put(b, type);
        // 비대칭 역방향 자동 설정
        RelationshipType inverse = inverseOf(type);
        edges.computeIfAbsent(b, k -> new HashMap<>()).put(a, inverse);
    }

    public RelationshipType getRelation(String a, String b) {
        return edges.getOrDefault(a, Map.of()).get(b);
    }

    public void removeRelation(String a, String b) {
        if (edges.containsKey(a)) edges.get(a).remove(b);
        if (edges.containsKey(b)) edges.get(b).remove(a);
    }

    public Map<String, RelationshipType> neighbors(String npc) {
        return edges.getOrDefault(npc, Map.of());
    }

    /** a와 b 사이 최단 관계 거리 (소문 도달 가능성). 도달 불가면 -1. */
    public int shortestPath(String a, String b, int maxHops) {
        if (a.equals(b)) return 0;
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        ArrayDeque<Integer> depth = new ArrayDeque<>();
        queue.add(a); depth.add(0); visited.add(a);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int d = depth.poll();
            if (d >= maxHops) continue;
            for (String next : neighbors(cur).keySet()) {
                if (next.equals(b)) return d + 1;
                if (visited.add(next)) { queue.add(next); depth.add(d + 1); }
            }
        }
        return -1;
    }

    /** 연결 수 = 영향력. 사회망 허브 NPC 찾기. */
    public int connectionCount(String npc) {
        return neighbors(npc).size();
    }

    /** 특정 타입 관계의 상대들만 (예: 모든 가족, 모든 신하). */
    public List<String> relationsOfType(String npc, RelationshipType type) {
        List<String> out = new java.util.ArrayList<>();
        for (var e : neighbors(npc).entrySet()) {
            if (e.getValue() == type) out.add(e.getKey());
        }
        return out;
    }

    public void removeAllOf(String npc) {
        edges.remove(npc);
        for (var m : edges.values()) m.remove(npc);
    }

    private RelationshipType inverseOf(RelationshipType t) {
        switch (t) {
            case MENTOR:   return RelationshipType.STUDENT;
            case STUDENT:  return RelationshipType.MENTOR;
            case EMPLOYER: return RelationshipType.EMPLOYEE;
            case EMPLOYEE: return RelationshipType.EMPLOYER;
            case LORD:     return RelationshipType.SUBJECT;
            case SUBJECT:  return RelationshipType.LORD;
            case DEBTOR:   return RelationshipType.CREDITOR;
            case CREDITOR: return RelationshipType.DEBTOR;
            default:       return t; // 대칭 (가족·친구·연인·적 등)
        }
    }

    public Map<String, Map<String, RelationshipType>> raw() { return edges; }
}
