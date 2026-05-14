package kr.reborn.core.data;

public enum StatType {
    // 공통 8종
    ENDURANCE, STRENGTH, AGILITY, INTELLIGENCE, MENTAL, LUCK, CHARISMA, CHARM,
    // 세계별 특수 스탯
    MANA, AURA, DEMON_KI, HEAVEN_KI, IMMORTAL_KI, YOKAI_KI, SPIRIT_POWER,
    DRAGON_POWER, OCEAN_POWER, MAGITECH_ENERGY, CYBER_ADAPTATION,
    LEVEL, INNER_KI, TAO_POWER,
    // 명계
    UNDERWORLD_KI,
    // 신성 (RebornGod)
    DIVINITY,
    // 심연 내성 (RebornDeath/심연계)
    ABYSS_RESISTANCE;

    public static StatType[] COMMON_8 = {
            ENDURANCE, STRENGTH, AGILITY, INTELLIGENCE,
            MENTAL, LUCK, CHARISMA, CHARM
    };

    public static boolean isCommon(StatType t) {
        for (StatType c : COMMON_8) if (c == t) return true;
        return false;
    }
}
