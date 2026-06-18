package kr.reborn.npc.faction;

/**
 * 두 세력 사이의 외교 태도.
 *
 * 일부는 비대칭: 종주국(OVERLORD)의 상대는 조공국(TRIBUTARY).
 * 나머지(전쟁·동맹·중립·휴전·적대)는 대칭.
 */
public enum FactionStance {
    WAR      ("전쟁",   false),
    ENEMY    ("적대",   false),
    NEUTRAL  ("중립",   true),
    TRUCE    ("휴전",   true),
    ALLY     ("동맹",   true),
    TRIBUTARY("조공국", true),   // 이 세력이 상대에게 조공을 바침
    OVERLORD ("종주국", true);   // 이 세력이 상대로부터 조공을 받음

    public final String label;
    public final boolean peaceful;

    FactionStance(String label, boolean peaceful) {
        this.label = label;
        this.peaceful = peaceful;
    }

    /** 상대 세력 입장에서 본 태도. */
    public FactionStance inverse() {
        switch (this) {
            case TRIBUTARY: return OVERLORD;
            case OVERLORD:  return TRIBUTARY;
            default:        return this;  // 대칭
        }
    }
}
