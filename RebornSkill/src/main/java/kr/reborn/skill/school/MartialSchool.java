package kr.reborn.skill.school;

import kr.reborn.core.data.StatType;

import java.util.Map;

/**
 * 무공 학파 — 정파/사파/마교 + 각 학파의 비급 분류.
 *
 * 학파별 보너스:
 *   ORTHODOX (정파): 깨달음 ×1.5, 단약 효과 ×1, 명예 +
 *   UNORTHODOX (사파): 자유, 약탈 시 보너스, 사파 NPC 호의 +
 *   DEMON_CULT (마교): 마기 +, 살생 시 흡성, 정파 NPC 적대
 *   IMPERIAL (황궁): 카리스마 +, 황실 권력 사용 가능
 *   HERMIT (은둔): 모든 학파와 중립, 깨달음 단계 보너스
 */
public enum MartialSchool {

    ORTHODOX("정파", "&3", Map.of(
            StatType.MENTAL, 30.0,
            StatType.INNER_KI, 200.0,
            StatType.CHARISMA, 20.0
    )),
    UNORTHODOX("사파", "&6", Map.of(
            StatType.AGILITY, 30.0,
            StatType.LUCK, 20.0,
            StatType.INNER_KI, 150.0
    )),
    DEMON_CULT("마교", "&5", Map.of(
            StatType.DEMON_KI, 300.0,
            StatType.STRENGTH, 30.0,
            StatType.INNER_KI, 100.0
    )),
    IMPERIAL("황궁", "&6&l", Map.of(
            StatType.CHARISMA, 50.0,
            StatType.CHARM, 30.0,
            StatType.INNER_KI, 100.0
    )),
    HERMIT("은둔파", "&7", Map.of(
            StatType.MENTAL, 50.0,
            StatType.INNER_KI, 300.0,
            StatType.LUCK, 10.0
    ));

    public final String koreanName;
    public final String colorCode;
    public final Map<StatType, Double> bonus;

    MartialSchool(String name, String color, Map<StatType, Double> bonus) {
        this.koreanName = name; this.colorCode = color; this.bonus = bonus;
    }

    /**
     * 학파가 다른 학파의 비급을 시전할 수 있는지.
     * 기획서 5-5: 정파↔마교 상호 배척, 정파↔사파 상극.
     * 황궁은 정파에 가까움. 은둔파는 모든 학파의 비급 학습 가능 (단 마교 제외).
     */
    public boolean canUse(MartialSchool skillSchool) {
        if (skillSchool == null) return true;          // 무학파 비급은 누구나
        if (this == skillSchool) return true;          // 자기 학파는 항상 OK
        if (this == HERMIT) return skillSchool != DEMON_CULT;
        switch (this) {
            case ORTHODOX:
                // 정파 = 황궁만 OK, 사파·마교 모두 차단
                return skillSchool == IMPERIAL;
            case UNORTHODOX:
                // 사파 = 황궁만 OK (정파 비급은 사용 안 함)
                return skillSchool == IMPERIAL;
            case DEMON_CULT:
                // 마교 = 자기 학파 외 모두 차단
                return false;
            case IMPERIAL:
                // 황궁 = 마교 외 모두 허용
                return skillSchool != DEMON_CULT;
            default:
                return false;
        }
    }
}
