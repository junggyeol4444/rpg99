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
}
