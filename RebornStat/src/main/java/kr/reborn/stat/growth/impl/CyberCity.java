package kr.reborn.stat.growth.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사이버시티 7대 구역 (기획서 5-11 도시 점령).
 *
 * 각 구역은 어느 코프가 점령했는지(currentOwner)와 코프별 영향력(influence)을 갖는다.
 * 영향력 1000 초과 시 코프 점령 가능 — 기존 소유자와 다르면 정복.
 *
 * 7대 구역:
 *   NIGHT_MARKET     자유시장 — 무역·암시장
 *   CORPORATE_PLAZA  본사 단지 — 정치·계약
 *   TECH_SLUMS       테크 빈민가 — 해커 본거지
 *   DATA_CORE        데이터 코어 — 정보전 중심
 *   BIOTECH_LABS     생체공학 단지 — 임플란트 R&D
 *   NEON_DOCKS       네온 부두 — 항만·밀수
 *   SKYBRIDGE        스카이브릿지 — 상류층 거주지
 */
public final class CyberCity {

    public static final String[] DISTRICTS = {
            "NIGHT_MARKET", "CORPORATE_PLAZA", "TECH_SLUMS",
            "DATA_CORE", "BIOTECH_LABS", "NEON_DOCKS", "SKYBRIDGE"
    };

    public final String id;
    /** 현재 점령 코프 id, null이면 무주공산. */
    public String currentOwner;
    /** 현 소유자가 점령한 시각 (ms). */
    public long ownedSince;
    /** 코프 id → 영향력 (0 ~ 5000). */
    public final Map<String, Integer> influence = new LinkedHashMap<>();

    /** 물리 좌표 — config로 설정 시 자동 진입 감지. null이면 /district enter 수동만. */
    public String world;
    public double x, z, radius;

    public CyberCity(String id) {
        this.id = id;
    }

    /** 좌표 설정 여부. */
    public boolean hasBounds() {
        return world != null && radius > 0;
    }

    /** 주어진 (worldName, x, z)가 이 구역 안인지. */
    public boolean contains(String worldName, double px, double pz) {
        if (!hasBounds()) return false;
        if (!world.equalsIgnoreCase(worldName)) return false;
        double dx = px - x, dz = pz - z;
        return dx * dx + dz * dz <= radius * radius;
    }

    public static boolean isDistrict(String id) {
        if (id == null) return false;
        for (String d : DISTRICTS) if (d.equals(id)) return true;
        return false;
    }
}
