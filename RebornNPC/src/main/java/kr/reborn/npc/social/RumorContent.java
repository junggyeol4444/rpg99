package kr.reborn.npc.social;

/**
 * 소문의 내용 종류. 각각 대상에 대한 reputation에 미치는 기본 영향(delta) 정의.
 *
 * 좋은 소문(IS_HERO, MARRIED, ACHIEVED_TIER) → 평판↑
 * 나쁜 소문(MURDERED, BETRAYED, CHEATED) → 평판↓
 *
 * severity·believability와 곱해져 최종 reputation 변동 결정.
 */
public enum RumorContent {

    // 긍정 (subject에 대한 평판↑)
    IS_HERO          (+30, "영웅이라더라"),
    MARRIED          (+10, "결혼했다더라"),
    ACHIEVED_TIER    (+20, "경지에 올랐다더라"),
    FOUNDED_CLAN     (+25, "가문을 세웠다더라"),
    ASCENDED         (+40, "초월했다더라 (신·선인·용왕)"),
    GIFTED_TO        (+15, "후하게 베풀었다더라"),
    SAVED_A_LIFE     (+50, "목숨을 구해줬다더라"),

    // 부정 (subject에 대한 평판↓)
    MURDERED         (-50, "사람을 죽였다더라"),
    BETRAYED         (-70, "배신했다더라"),
    CHEATED          (-25, "속였다더라"),
    INSULTED         (-15, "모욕했다더라"),
    ATTACKED_BY      (-30, "공격했다더라"),
    STOLE_FROM       (-35, "훔쳤다더라"),
    IS_VILLAIN       (-40, "악인이라더라"),
    SECRET_LOVER     (-10, "비밀 연인이 있다더라"),   // 가치관에 따라 다르지만 보통 추문
    BROKE_OATH       (-45, "맹세를 어겼다더라");

    public final int defaultDelta;
    public final String storyTemplate;

    RumorContent(int delta, String story) {
        this.defaultDelta = delta;
        this.storyTemplate = story;
    }

    public boolean isPositive() { return defaultDelta > 0; }
    public boolean isMajor() { return Math.abs(defaultDelta) >= 40; }
}
