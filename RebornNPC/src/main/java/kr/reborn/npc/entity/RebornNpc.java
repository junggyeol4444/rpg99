package kr.reborn.npc.entity;

import kr.reborn.core.data.WorldKey;
import kr.reborn.npc.ai.NpcBrain;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.relation.Relations;
import kr.reborn.npc.soul.Personality;
import kr.reborn.npc.soul.Soul;
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
    /** "집" 위치 — 수면·휴식 행동의 목표. 비어있으면 location 사용. */
    public Location home;
    /** "직장" 위치 — 출근 행동의 목표. */
    public Location workplace;
    public String faction = "";
    public String job = "VILLAGER";
    public NpcState state = NpcState.IDLE;

    /** 단순 스탯 (StatType 이름 → 값). */
    public final Map<String, Double> stats = new HashMap<>();
    public final Emotion emotion = new Emotion();
    public final Relations relations = new Relations();
    /** 영혼 — 성격·기억·욕구. 진짜 의사결정의 핵심. */
    public Soul soul;

    /** Brain — 의사결정 컨트롤러. 생성 시 등록. */
    public transient NpcBrain brain;
    /** Behavior 간 공유 임시 데이터 (예: 현재 이동 목표, 대화 상대 등). */
    public final Map<String, Object> aiData = new HashMap<>();

    /** 은둔고수 여부 */
    public boolean hermit = false;
    /** 정체 공개 여부 */
    public boolean revealed = false;

    /** 실제 엔티티 (브레인의 movement 명령 대상). */
    public UUID bukkitEntityId;

    /** 사망 여부 + 사망 원인 (복수 트리거용). */
    public boolean dead = false;
    public UUID killerId;
    public long deathAt;

    /** 결혼 상대 NPC id (없으면 빈 문자열). */
    public String spouseNpcId = "";
    /** 마지막 결혼·출산 timestamp. */
    public long lastMarriageAt;
    public long lastChildAt;
    /** 자녀 NPC id 목록. */
    public final java.util.List<String> children = new java.util.ArrayList<>();

    /** 일과 스케줄 (시작 시간 → 목적지 키워드). 비어있으면 기본 일과 사용. */
    public final Map<Integer, String> schedule = new HashMap<>();

    public RebornNpc(String id, String displayName, WorldKey world, Location location) {
        this.id = id; this.displayName = displayName; this.world = world; this.location = location;
        this.soul = new Soul(Personality.random());  // 기본 무작위. job 설정 시 fromJob으로 교체.
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
