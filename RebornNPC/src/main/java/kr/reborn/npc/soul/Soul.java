package kr.reborn.npc.soul;

/**
 * NPC의 "영혼" — 성격·기억·욕구의 통합.
 * RebornNpc에 1:1로 연결.
 *
 * 모든 의사결정 (UtilityBrain)이 이 데이터를 읽어서 점수를 매긴다.
 */
public final class Soul {

    public final Personality personality;
    public final Memory memory;
    public final Needs needs;
    /** 소문으로 형성된 평판 (직접 경험과 별개). */
    public final kr.reborn.npc.social.Reputation reputation = new kr.reborn.npc.social.Reputation();
    /** 명시적 관계 (가족·스승·주군·연인 등). SocialNetwork와 동기화. */
    public final java.util.Map<String, kr.reborn.npc.social.RelationshipType> relationships = new java.util.HashMap<>();
    /** 최근 들은 소문 (최대 50건, 오래된 것부터 제거). */
    public final java.util.List<kr.reborn.npc.social.Rumor> rumorsHeard = new java.util.ArrayList<>();

    /** 가족 — 부모, 자녀, 형제. 강한 결속. (id 리스트) */
    public final java.util.List<String> family = new java.util.ArrayList<>();
    /** 친구 — 깊은 관계. 가족 다음으로 강함. */
    public final java.util.List<String> friends = new java.util.ArrayList<>();
    /** 라이벌 — 적대적이지만 인정. */
    public final java.util.List<String> rivals = new java.util.ArrayList<>();
    /** 원수 — 죽이고 싶을 정도. */
    public final java.util.List<String> nemeses = new java.util.ArrayList<>();

    /** 출생 시각 (가상 나이 계산용). */
    public final long birthAt;
    /** 가상 나이 (years). 매 사이클 누적. */
    public double ageYears;

    public Soul(Personality personality) {
        this.personality = personality;
        this.memory = new Memory();
        this.needs = new Needs();
        this.birthAt = System.currentTimeMillis();
        this.ageYears = 20;  // 기본 성인
    }

    /**
     * subject에 대한 종합 관계 점수.
     * = 직접 경험(Memory) + 소문 평판(Reputation ×0.5) + 친밀도 + 명시적 관계 보너스
     */
    public double relationToward(String subject) {
        double s = memory.sentimentFor(subject);
        s += reputation.scoreOf(subject) * 0.5;  // 소문은 절반 가중 (직접 경험보다 약함)
        if (family.contains(subject)) s += 40;
        if (friends.contains(subject)) s += 25;
        if (rivals.contains(subject)) s -= 30;
        if (nemeses.contains(subject)) s -= 80;
        // 명시적 관계 보너스
        var rel = relationships.get(subject);
        if (rel != null) {
            switch (rel) {
                case KIN: case SPOUSE: case LOVER: s += 35; break;
                case FRIEND: case ALLY: s += 20; break;
                case MENTOR: case STUDENT: s += 25; break;
                case LORD: case SUBJECT: s += 15; break;
                case RIVAL: s -= 25; break;
                case ENEMY: s -= 50; break;
                case NEMESIS: s -= 80; break;
                default: break;
            }
        }
        return Math.max(-100, Math.min(100, s));
    }

    /** 가족·친구 분류 자동 갱신 — sentiment 기반. */
    public void reclassify(String subject) {
        double s = memory.sentimentFor(subject);
        family.remove(subject);
        friends.remove(subject);
        rivals.remove(subject);
        nemeses.remove(subject);
        if (s >= 70 && memory.hasMemoryOf(subject, Memory.Kind.MARRIED_ME)) family.add(subject);
        else if (s >= 50) friends.add(subject);
        else if (s <= -70) nemeses.add(subject);
        else if (s <= -30) rivals.add(subject);
    }

    /** 행복 지수 — 욕구 만족 평균 + 기억 종합. */
    public double happiness() {
        double base = needs.averageSatisfaction();
        // 친구 많으면 +, 원수 많으면 -
        return Math.max(0, Math.min(100, base + friends.size() * 2 - nemeses.size() * 3));
    }
}
