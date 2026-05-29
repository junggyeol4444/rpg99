package kr.reborn.god.war;

/**
 * 두 신 사이의 전쟁 상태.
 *
 * 신도 간 적대 활성화 + 신성 소비 가속 + 종료 시 패전 신의 신앙 일부가 승전 신으로 이양.
 */
public final class DivineWar {

    public final String challengerGodId;
    public final String defenderGodId;
    public final long startedAt;
    public long endedAt;
    /** 양 신도가 서로에게 가한 누적 피해 (전쟁 점수). */
    public double challengerScore;
    public double defenderScore;

    public DivineWar(String challenger, String defender) {
        this.challengerGodId = challenger;
        this.defenderGodId = defender;
        this.startedAt = System.currentTimeMillis();
    }

    public boolean active() { return endedAt == 0; }

    public String winnerSide() {
        if (active()) return "";
        return challengerScore > defenderScore ? challengerGodId : defenderGodId;
    }

    public String loserSide() {
        if (active()) return "";
        return challengerScore > defenderScore ? defenderGodId : challengerGodId;
    }
}
