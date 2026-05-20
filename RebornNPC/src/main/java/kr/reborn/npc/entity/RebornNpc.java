package kr.reborn.npc.entity;

import kr.reborn.core.data.WorldKey;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.relation.Relations;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RebornNpc {

    public final String id;
    public String displayName;
    public WorldKey world;
    public Location location;
    public String faction = "";
    public String job = "VILLAGER";
    public NpcState state = NpcState.IDLE;

    public final Map<String, Double> stats = new HashMap<>(); // 단순 스탯 (StatType 문자열)
    public final Emotion emotion = new Emotion();
    public final Relations relations = new Relations();

    /** 은둔고수 여부 */
    public boolean hermit = false;
    /** 정체 공개 여부 */
    public boolean revealed = false;

    /** 패킷 NPC 미사용 시 폴백으로 쓰는 실제 엔티티 */
    public UUID bukkitEntityId;

    public RebornNpc(String id, String displayName, WorldKey world, Location location) {
        this.id = id; this.displayName = displayName; this.world = world; this.location = location;
    }

    public double totalStats() {
        double sum = 0;
        for (double v : stats.values()) sum += v;
        return sum;
    }

    public double effectiveTotal() {
        if (hermit && !revealed) return totalStats() * 0.05;
        return totalStats();
    }

    public boolean isHostileTo(UUID playerId) {
        return relations.stagePlayer(playerId) == Relations.Stage.NEMESIS;
    }

    public boolean isAggro() {
        return state == NpcState.COMBAT;
    }

    public Class<? extends Villager> defaultEntity() { return Villager.class; }
}
