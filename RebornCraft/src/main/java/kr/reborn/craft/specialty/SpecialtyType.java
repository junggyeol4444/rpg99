package kr.reborn.craft.specialty;

import kr.reborn.core.data.StatType;

/**
 * 특화 제작 enum — 각 세계 고유 제작술.
 *
 * 각 특화는 필요 스탯 + 베이스 성공률 + 숙련도(별도) 기반.
 */
public enum SpecialtyType {

    /** 단약 제조 (무협·선계) — INTELLIGENCE + MENTAL */
    ELIXIR_BREWING       (StatType.INTELLIGENCE, StatType.MENTAL, 0.60, "연단", "단약 제조 — 대환단·자하단·빙심단"),
    /** 마도공학 (마도공학계) — INTELLIGENCE + MAGITECH_ENERGY */
    MAGITECH_FORGE       (StatType.INTELLIGENCE, StatType.MAGITECH_ENERGY, 0.50, "마도공학", "골렘·자동인형·마법기구"),
    /** 해킹/사이버 (사이버펑크) — INTELLIGENCE + CYBER_ADAPTATION */
    CYBER_HACK           (StatType.INTELLIGENCE, StatType.CYBER_ADAPTATION, 0.55, "사이버해킹", "데이터 칩·바이러스·임플란트"),
    /** 연금술 (판타지) — INTELLIGENCE + MANA */
    ALCHEMY              (StatType.INTELLIGENCE, StatType.MANA, 0.60, "연금술", "포션·변환술·물질 합성"),
    /** 결정학 (정령계) — INTELLIGENCE + SPIRIT_POWER */
    CRYSTALLURGY         (StatType.INTELLIGENCE, StatType.SPIRIT_POWER, 0.55, "결정학", "마정석·원소 결정 가공"),
    /** 룬각 (판타지·드워프) — INTELLIGENCE + STRENGTH */
    RUNE_INSCRIPTION     (StatType.INTELLIGENCE, StatType.STRENGTH, 0.50, "룬각", "룬 새기기·부적 제작"),
    /** 영조 (마계·요계) — MENTAL + DEMON_KI */
    SPIRIT_FORGING       (StatType.MENTAL, StatType.DEMON_KI, 0.45, "영조", "영혼 무기·저주 도구"),
    /** 단조/정련 (모든 세계 공통) — STRENGTH + ENDURANCE */
    METALWORKING         (StatType.STRENGTH, StatType.ENDURANCE, 0.70, "단조", "무기·갑옷 일반 제련"),
    /** 직조 (판타지·아포칼립스) — AGILITY + CHARM */
    WEAVING              (StatType.AGILITY, StatType.CHARM, 0.65, "직조", "마법 의복·천 갑옷"),
    /** 보석 세공 (드래곤·해양) — INTELLIGENCE + LUCK */
    GEM_CUTTING          (StatType.INTELLIGENCE, StatType.LUCK, 0.55, "보석세공", "마법 보석·장신구"),
    /** 향수술 (요계·정령) — CHARM + LUCK */
    PERFUMERY            (StatType.CHARM, StatType.LUCK, 0.65, "향수술", "매혹·환각 향수"),
    /** 약학 (지구·아포칼립스) — INTELLIGENCE + LUCK */
    PHARMACOLOGY         (StatType.INTELLIGENCE, StatType.LUCK, 0.65, "약학", "현대 의약품·치료제");

    public final StatType primaryStat;
    public final StatType secondaryStat;
    public final double baseSuccessRate;
    public final String koreanName;
    public final String description;

    SpecialtyType(StatType primary, StatType secondary, double base, String kor, String desc) {
        this.primaryStat = primary; this.secondaryStat = secondary;
        this.baseSuccessRate = base; this.koreanName = kor; this.description = desc;
    }
}
