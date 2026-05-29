package kr.reborn.god.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 교단(敎團) — 신을 섬기는 사회 조직.
 *
 * 신도(player followers + NPC follower count) 수에 비례해 신앙(faith)이 누적된다.
 * 신앙은 FaithEngine이 주기적으로 신성(divinity)으로 변환해 신에게 공급한다.
 * 신앙은 시간 지나면 천천히 감쇠 (사람들이 잊는다).
 * 교단끼리 적대(anti)·동맹 관계가 있고, 적대 교단 신도에게는 신앙 감쇠가 더 빠르다.
 */
public final class Religion {
    public final String id;
    public final String name;
    /** 섬기는 신 ID (npcId 또는 player:UUID). */
    public final String godIdentifier;
    public String doctrine;
    public double faith;
    public final long foundedAt = System.currentTimeMillis();

    /** 등록된 플레이어 신도. */
    public final Set<UUID> followers = new HashSet<>();
    /** NPC 신도 추정 수 (config base + 동적 증가). */
    public int npcFollowerCount;

    /** 적대 교단 ID — faith가 그쪽으로 새거나 깎임. */
    public String antiReligion = "";
    /** 동맹 교단 ID. */
    public final Set<String> allyReligions = new HashSet<>();

    /** 금기 교단 여부 (어둠의 교단 등 — 발각 시 사회적 제재). */
    public boolean forbidden;
    /** 보호적 교단 (아자토스 봉인 같은 — 봉인 유지가 사명). */
    public boolean protective;

    public Religion(String id, String name, String godIdentifier, String doctrine, int baseFaith) {
        this.id = id;
        this.name = name;
        this.godIdentifier = godIdentifier;
        this.doctrine = doctrine == null ? "" : doctrine;
        this.faith = baseFaith;
        this.npcFollowerCount = baseFaith / 10;
    }

    /** 총 신도 수 (플레이어 + NPC). */
    public int totalFollowers() {
        return followers.size() + npcFollowerCount;
    }

    /** 한 주기에 생성되는 신앙 — 신도 수에 비례, 적대 교단 존재 시 일부 감쇠. */
    public double faithGainPerCycle() {
        double gain = totalFollowers() * 0.1;
        return Math.max(0, gain);
    }
}
