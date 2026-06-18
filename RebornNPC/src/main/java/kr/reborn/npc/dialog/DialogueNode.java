package kr.reborn.npc.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 대화 노드 — 한 줄의 대사 + 선택지 또는 다음 노드.
 *
 * - text: NPC가 말하는 대사 (컬러코드 가능)
 * - choices: 플레이어 선택지 (비어 있으면 자동 다음 또는 종료)
 * - autoNext: 선택지 없을 때 자동으로 이동할 노드 ID (null = 종료)
 * - actions: 이 노드 도달 시 실행할 액션 ID 목록 (give_item, addFavor, giveQuest 등)
 * - conditions: 이 노드 표시 조건 (favor>=N, hasItem:X 등) — false면 fallback 사용
 * - fallbackNodeId: condition 실패 시 대체 노드
 */
public final class DialogueNode {

    public final String id;
    public final String text;
    public final List<DialogueChoice> choices = new ArrayList<>();
    public String autoNext;
    public final List<String> actions = new ArrayList<>();
    public final List<String> conditions = new ArrayList<>();
    public String fallbackNodeId;

    public DialogueNode(String id, String text) {
        this.id = id;
        this.text = text;
    }
}
