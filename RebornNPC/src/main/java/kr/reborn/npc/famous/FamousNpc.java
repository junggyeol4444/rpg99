package kr.reborn.npc.famous;

import kr.reborn.core.data.WorldKey;

/**
 * 유명 NPC 정의 — 기획서에 명명된 등장 인물.
 *
 * 각 NPC는:
 *   - 영구 보존 (사망 시 자동 부활 또는 후계자 NPC)
 *   - 강력한 스탯 (최소 10만)
 *   - 비급/스킬 보유
 *   - 고유 대화 트리
 *   - 호의도 임계마다 큰 보상
 */
public final class FamousNpc {

    public final String id;
    public final String displayName;
    public final WorldKey world;
    public final String job;
    public final String faction;
    /** "마왕" / "천제" / "용왕" 등 호칭 */
    public final String title;
    /** 권력 등급 (1~10, 10=세계 최강) */
    public final int powerRank;
    /** 호의 100 도달 시 비급/보상 */
    public final String rewardManualId;
    public final String description;

    public FamousNpc(String id, String name, WorldKey world, String job, String faction,
                     String title, int powerRank, String rewardManualId, String description) {
        this.id = id; this.displayName = name; this.world = world;
        this.job = job; this.faction = faction; this.title = title;
        this.powerRank = powerRank;
        this.rewardManualId = rewardManualId;
        this.description = description;
    }
}
