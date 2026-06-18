package kr.reborn.npc.social;

/**
 * 두 NPC(또는 NPC↔플레이어) 사이의 관계 종류.
 *
 * 양방향 비대칭 가능: A→B가 MENTOR이면 B→A는 STUDENT.
 */
public enum RelationshipType {
    KIN         (true,  "가족"),
    LOVER       (true,  "연인"),
    SPOUSE      (true,  "배우자"),
    FRIEND      (true,  "친구"),
    ALLY        (true,  "동맹"),
    NEUTRAL     (false, "중립"),
    ACQUAINTANCE(false, "지인"),
    RIVAL       (false, "라이벌"),
    ENEMY       (false, "적"),
    NEMESIS     (false, "불구대천"),
    MENTOR      (true,  "스승"),         // A→B (B는 STUDENT)
    STUDENT     (true,  "제자"),         // A→B (B는 MENTOR)
    EMPLOYER    (true,  "고용주"),       // A→B (B는 EMPLOYEE)
    EMPLOYEE    (true,  "고용인"),
    SUBJECT     (true,  "신하"),         // A→B (B는 LORD)
    LORD        (true,  "주군"),
    DEBTOR      (false, "채무자"),
    CREDITOR    (false, "채권자");

    public final boolean positive;
    public final String label;
    RelationshipType(boolean pos, String label) { this.positive = pos; this.label = label; }
}
