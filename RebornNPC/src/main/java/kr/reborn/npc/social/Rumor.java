package kr.reborn.npc.social;

/**
 * 전파되는 소문 한 건. 불변(생성 후 hopCount·believability만 사본으로 변경).
 *
 * 전파될 때마다 retold()로 새 Rumor 생성 — hopCount+1, believability×0.85.
 * believability < 0.1 또는 hopCount > 5면 폐기.
 */
public final class Rumor {

    /** 소문을 처음 만든 NPC id (목격자). */
    public final String originator;
    /** 소문의 주인공 — NPC id 또는 player UUID 문자열. */
    public final String subject;
    /** 부차 대상 (예: "A가 B를 죽였다"의 B). 없으면 빈 문자열. */
    public final String object;
    public final RumorContent content;
    /** 사건 강도 0~100 (살인 90, 가벼운 모욕 15 등). */
    public final int severity;
    /** 진실성 0~1. 전달될수록 감소(변질). */
    public final double believability;
    public final int hopCount;
    public final long createdAt;

    public Rumor(String originator, String subject, String object,
                 RumorContent content, int severity,
                 double believability, int hopCount, long createdAt) {
        this.originator = originator;
        this.subject = subject;
        this.object = object == null ? "" : object;
        this.content = content;
        this.severity = severity;
        this.believability = believability;
        this.hopCount = hopCount;
        this.createdAt = createdAt;
    }

    /** 새로 발생한 1차 소문. */
    public static Rumor fresh(String originator, String subject, String object,
                              RumorContent content, int severity) {
        return new Rumor(originator, subject, object, content, severity,
                1.0, 0, System.currentTimeMillis());
    }

    /** 전달된 사본 — 신뢰도 감소·hop 증가. */
    public Rumor retold() {
        return new Rumor(originator, subject, object, content, severity,
                believability * 0.85, hopCount + 1, createdAt);
    }

    /** 더 전파될 가치가 있는가. */
    public boolean isViable() {
        return believability >= 0.1 && hopCount <= 5;
    }

    /** 이 소문이 reputation에 미칠 영향 (음수=평판↓). */
    public double reputationEffect() {
        return content.defaultDelta * (severity / 100.0) * believability;
    }

    /** 같은 사건인지 (중복 전파 방지용 키). */
    public String eventKey() {
        return subject + ":" + content.name() + ":" + object;
    }

    public String story(String subjectName) {
        return subjectName + "이(가) " + content.storyTemplate;
    }
}
