package kr.reborn.clan.war;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 두 가문(또는 동맹) 사이의 전쟁 인스턴스.
 *
 * 점수: 양 가문 멤버가 서로에게 가한 피해 누적.
 * 만료: 7일 또는 한 쪽 굴복(treasury 0).
 * 종전: 패전 가문은 treasury 30% 이양 + 영토 1개 이양.
 */
public final class ClanWar {

    public final String challengerClanId;
    public final String defenderClanId;
    public final long startedAt;
    public long endedAt;
    public double challengerScore;
    public double defenderScore;
    /** 동맹전 — 같은 편 가문 set */
    public final Set<String> challengerAllies = new HashSet<>();
    public final Set<String> defenderAllies = new HashSet<>();
    /** 가담 멤버 PK 면책 set (전시 면책) */
    public final Set<UUID> immunePlayers = new HashSet<>();

    public ClanWar(String challenger, String defender) {
        this.challengerClanId = challenger;
        this.defenderClanId = defender;
        this.startedAt = System.currentTimeMillis();
    }

    public boolean active() { return endedAt == 0; }

    public String winnerSide() {
        if (active()) return "";
        return challengerScore > defenderScore ? challengerClanId : defenderClanId;
    }

    public String loserSide() {
        if (active()) return "";
        return challengerScore > defenderScore ? defenderClanId : challengerClanId;
    }

    public boolean involves(String clanId) {
        return challengerClanId.equals(clanId) || defenderClanId.equals(clanId)
                || challengerAllies.contains(clanId) || defenderAllies.contains(clanId);
    }

    public boolean isChallengerSide(String clanId) {
        return challengerClanId.equals(clanId) || challengerAllies.contains(clanId);
    }
}
