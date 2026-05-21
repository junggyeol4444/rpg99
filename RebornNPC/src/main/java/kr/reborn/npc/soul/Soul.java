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

    /** subject에 대한 종합 관계 점수 = 기억의 sentiment + 친밀도 보너스 */
    public double relationToward(String subject) {
        double s = memory.sentimentFor(subject);
        if (family.contains(subject)) s += 40;
        if (friends.contains(subject)) s += 25;
        if (rivals.contains(subject)) s -= 30;
        if (nemeses.contains(subject)) s -= 80;
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
