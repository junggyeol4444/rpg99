package kr.reborn.npc.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 대화 선택지 — 플레이어가 고를 1개 옵션.
 *
 * - label: 채팅에 표시될 문장
 * - nextNodeId: 선택 시 이동할 노드 (null = 종료)
 * - conditions: 표시 조건 (예: "favor>=50", "personality:EMPATHY>=70")
 * - actions: 선택 시 실행 (예: "addFavor:5", "giveItem:bread")
 */
public final class DialogueChoice {

    public final String label;
    public final String nextNodeId;
    public final List<String> conditions = new ArrayList<>();
    public final List<String> actions = new ArrayList<>();

    public DialogueChoice(String label, String nextNodeId) {
        this.label = label;
        this.nextNodeId = nextNodeId;
    }
}
