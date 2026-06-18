package kr.reborn.npc.dialog;

import java.util.UUID;

/** 한 플레이어 × NPC 활성 대화 세션. */
public final class DialogueSession {

    public final UUID playerId;
    public final String npcId;
    public final Dialogue dialogue;
    public DialogueNode current;
    public final long startedAt;

    public DialogueSession(UUID p, String npcId, Dialogue d, DialogueNode start) {
        this.playerId = p; this.npcId = npcId;
        this.dialogue = d; this.current = start;
        this.startedAt = System.currentTimeMillis();
    }

    public boolean ended() { return current == null; }
}
