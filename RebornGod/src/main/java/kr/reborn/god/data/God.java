package kr.reborn.god.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class God {
    public final UUID owner;       // null이면 NPC 신
    public final String npcId;
    public String name;
    public double divinity;
    public final Set<UUID> followers = new HashSet<>();
    public final Set<UUID> allies = new HashSet<>();
    public String domainWorld = ""; // 신역 월드
    public boolean sealed;

    public God(UUID owner, String npcId, String name, double divinity) {
        this.owner = owner; this.npcId = npcId; this.name = name; this.divinity = divinity;
    }

    public String tier(java.util.List<java.util.Map<?, ?>> tiers) {
        String t = "하급신";
        for (var e : tiers) {
            double min = ((Number) e.get("min")).doubleValue();
            if (divinity >= min) t = String.valueOf(e.get("name"));
        }
        return t;
    }
}
