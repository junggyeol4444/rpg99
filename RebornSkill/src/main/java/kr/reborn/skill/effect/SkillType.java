package kr.reborn.skill.effect;

/**
 * 스킬이 실제로 어떻게 작동하는가.
 *
 * YAML에 명시(type:)할 수 있으나, 대부분은 기존 필드(damage/aoe-radius/category)에서
 * 자동 추론한다 — 136개 스킬을 손으로 분류하지 않아도 종류별로 다르게 동작.
 */
public enum SkillType {
    MELEE,       // 정면 근접 (콘 범위 1명)
    PROJECTILE,  // 투사체 발사 → 명중 시 피해
    AOE,         // 지점 중심 반경 광역
    HEAL,        // 회복 (자신 + 반경 아군)
    BUFF,        // 자기 강화 (포션·비행·보호막 N초)
    DASH,        // 전방 돌진 (속도 부여)
    BLINK,       // 순간이동 (바라보는 지점)
    DOT,         // 지속 피해 (출혈·중독·연소)
    CHAIN,       // 연쇄 (대상→근처 대상으로 튐)
    SUMMON,      // 소환수
    UTILITY;     // 기타 자기효과 (감응·항해 등)

    public static SkillType infer(String explicit, String damage, double aoeRadius,
                                  String category, String id, String name) {
        if (explicit != null && !explicit.isBlank()) {
            try { return valueOf(explicit.trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        String key = (id + " " + (name == null ? "" : name)).toLowerCase();
        // 이름 기반 이동기 감지
        if (containsAny(key, "돌진", "질풍", "dash", "charge", "rush")) return DASH;
        if (containsAny(key, "순간이동", "블링크", "blink", "축지", "순보", "teleport", "gate", "게이트")) return BLINK;
        if (containsAny(key, "소환", "summon", "골렘", "golem", "드론", "drone")) return SUMMON;
        if (containsAny(key, "비행", "flight", "수중", "호흡", "navigation", "항해")) return UTILITY;
        // 피해 수식 기반
        boolean hasDamage = damage != null && !damage.isBlank();
        if (hasDamage && damage.trim().startsWith("-")) return HEAL;
        if (aoeRadius > 0) return AOE;
        if (containsAny(key, "연쇄", "체인", "chain", "다발", "스톰", "storm", "미사일")) return CHAIN;
        if (containsAny(key, "독", "중독", "출혈", "poison", "bleed", "drain", "흡성", "흡혈")) return DOT;
        if (!hasDamage) return BUFF;
        // 피해 있고 광역 아님: 무공은 근접, 그 외(마법·원소)는 투사체
        if ("MARTIAL".equalsIgnoreCase(category)) return MELEE;
        return PROJECTILE;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }
}
