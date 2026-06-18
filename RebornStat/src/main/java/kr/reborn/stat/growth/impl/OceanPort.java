package kr.reborn.stat.growth.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 해양 7대 항구 (기획서 5-13 제국 거점).
 *
 * 각 항구는 어느 제국이 점령했는지(currentRuler)와 제국별 영향력을 갖는다.
 * 영향력 1000 초과 시 제국 점령 가능 — 기존 통치자와 다르면 정복.
 *
 * 7대 항구:
 *   ATLANTIS_HARBOR    아틀란티스 모항 — 해군 본거지
 *   CORAL_TRADE_PORT   코럴 무역항 — 무역 중심
 *   ABYSS_TEMPLE_DOCK  심해 신전 부두 — 종교
 *   PEARL_LAGOON       인어 라군 — 인어 왕국 거점
 *   FREEPORT_HAVEN     자유항 — 해적 해방구
 *   STORM_FORTRESS     폭풍 요새 — 군사
 *   GHOST_GRAVEYARD    유령 묘지 — 망자의 해류
 */
public final class OceanPort {

    public static final String[] PORTS = {
            "ATLANTIS_HARBOR", "CORAL_TRADE_PORT", "ABYSS_TEMPLE_DOCK",
            "PEARL_LAGOON", "FREEPORT_HAVEN", "STORM_FORTRESS", "GHOST_GRAVEYARD"
    };

    public final String id;
    /** 현재 통치 제국 id, null이면 무주. */
    public String currentRuler;
    public long ruledSince;
    /** 제국 id → 영향력 (0 ~ 5000). */
    public final Map<String, Integer> influence = new LinkedHashMap<>();

    /** 물리 좌표 — config로 설정 시 자동 정박 감지. null이면 /port enter 수동만. */
    public String world;
    public double x, z, radius;

    public OceanPort(String id) {
        this.id = id;
    }

    public boolean hasBounds() {
        return world != null && radius > 0;
    }

    public boolean contains(String worldName, double px, double pz) {
        if (!hasBounds()) return false;
        if (!world.equalsIgnoreCase(worldName)) return false;
        double dx = px - x, dz = pz - z;
        return dx * dx + dz * dz <= radius * radius;
    }

    public static boolean isPort(String id) {
        if (id == null) return false;
        for (String p : PORTS) if (p.equals(id)) return true;
        return false;
    }
}
