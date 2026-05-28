package kr.reborn.skill.technique;

/**
 * 무공/마법 한 초식(招式). 비급(SkillDef) 하나가 여러 초식을 가진다.
 *
 * 예: 독고구검 — 총결식·파검식·파도식·파창식·파편식·파삭식·파장식·파전식·파기식 (9초식)
 * 시전 시 초식이 순환하며, 각 초식이 데미지 배수·속성·설명을 다르게 가진다.
 */
public final class Technique {

    public final String name;
    /** 기본 데미지에 곱하는 배수 (1.0 = 그대로). */
    public final double mult;
    /** 속성 덮어쓰기 (비어있으면 비급의 element 유지). */
    public final String elementOverride;
    public final String description;

    public Technique(String name, double mult, String elementOverride, String description) {
        this.name = name;
        this.mult = mult;
        this.elementOverride = (elementOverride == null || elementOverride.isEmpty()) ? null : elementOverride;
        this.description = description == null ? "" : description;
    }
}
