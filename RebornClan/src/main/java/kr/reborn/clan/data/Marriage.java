package kr.reborn.clan.data;

import java.util.UUID;

public final class Marriage {
    public final UUID a;
    public final UUID b;          // 플레이어 또는 NPC UUID
    public final String npcId;    // NPC와 결혼한 경우의 NPC ID (없으면 빈 문자열)
    public final long marriedAt;

    public Marriage(UUID a, UUID b, String npcId, long marriedAt) {
        this.a = a; this.b = b; this.npcId = npcId; this.marriedAt = marriedAt;
    }
}
