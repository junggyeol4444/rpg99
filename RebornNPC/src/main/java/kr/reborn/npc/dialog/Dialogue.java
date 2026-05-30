package kr.reborn.npc.dialog;

import java.util.HashMap;
import java.util.Map;

/**
 * 한 NPC(또는 NPC 카테고리)의 대화 트리.
 *
 * 식별 키: id (예: "common_villager", "solim_abbot", "blacksmith")
 * NPC와 매핑 — NpcInteractListener가 npc.dialogId 또는 npc.job 기반으로 검색.
 *
 * rootNodeId: 대화 시작 노드 (보통 "greet")
 * 노드 맵으로 임의 분기 (게이트·루프 모두 가능)
 */
public final class Dialogue {

    public final String id;
    public final String rootNodeId;
    public final Map<String, DialogueNode> nodes = new HashMap<>();

    public Dialogue(String id, String rootNodeId) {
        this.id = id;
        this.rootNodeId = rootNodeId;
    }

    public DialogueNode node(String id) { return nodes.get(id); }
    public DialogueNode root() { return nodes.get(rootNodeId); }
}
