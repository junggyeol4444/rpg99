package kr.reborn.god.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 신(神) 1 인스턴스 — 플레이어가 등극한 신 또는 NPC 신.
 *
 * 신성(divinity)은 다음 등급 결정 + 기적·시련·신역의 자원으로 쓰임.
 * 신앙(faith)은 신도(religion 통해)에게서 지속 유입.
 * 진행중 신 전쟁·기적 쿨다운도 추적.
 */
public final class God {
    public final UUID owner;         // null이면 NPC 신
    public final String npcId;        // NPC 신 ID (플레이어 신은 "")
    public String name;
    public double divinity;
    public final Set<UUID> followers = new HashSet<>();
    public final Set<UUID> allies = new HashSet<>();
    /** 진행중 전쟁 상대 신 id. */
    public String warOpponent = "";
    public long warStartedAt;
    /** 신역(domain) 월드 이름. */
    public String domainWorld = "";
    /** NPC 신 봉인 여부 (아자토스 등). */
    public boolean sealed;
    /** 기적별 쿨다운 — miracleId → 다음 사용 가능 ms. */
    public final Map<String, Long> miracleCooldowns = new HashMap<>();
    /** 등극·즉위 시각. */
    public final long ascendedAt = System.currentTimeMillis();
    /** 영향력 — 신도/동맹 수에 따라 증가, 신 전쟁·외교 가중치. */
    public double influence;

    public God(UUID owner, String npcId, String name, double divinity) {
        this.owner = owner;
        this.npcId = npcId == null ? "" : npcId;
        this.name = name;
        this.divinity = divinity;
    }

    /** 신성 등급 계산 — 하급/중급/상급/주신/개념신. */
    public String tier(java.util.List<java.util.Map<?, ?>> tiers) {
        String t = "하급신";
        for (var e : tiers) {
            double min = ((Number) e.get("min")).doubleValue();
            if (divinity >= min) t = String.valueOf(e.get("name"));
        }
        return t;
    }

    public boolean atWar() { return !warOpponent.isEmpty(); }
    public String identifier() { return owner != null ? "player:" + owner : "npc:" + npcId; }
}
