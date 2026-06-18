package kr.reborn.spawn.race;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 종족 enum — 환생 시 자동 배정. 세계별 가능한 종족 목록.
 *
 * 각 종족: 영구 스탯 보너스 + 약간의 페널티 (밸런스).
 * 일부는 초레어 (5% 가중) — DEMI_GOD, ANCIENT_DRAGON, FROST_GIANT 등.
 */
public enum Race {

    // ─── 판타지 ───
    HUMAN          ("인간",       1.0,  Map.of(StatType.LUCK, 10.0)),
    HIGH_ELF       ("하이엘프",   0.8,  Map.of(StatType.INTELLIGENCE, 30.0, StatType.MANA, 100.0)),
    DARK_ELF       ("다크엘프",   0.6,  Map.of(StatType.AGILITY, 25.0, StatType.CHARM, 15.0)),
    DWARF          ("드워프",     0.8,  Map.of(StatType.ENDURANCE, 40.0, StatType.STRENGTH, 20.0)),
    HALFLING       ("호빗",       0.7,  Map.of(StatType.AGILITY, 20.0, StatType.LUCK, 30.0)),
    ORC            ("오크",       0.7,  Map.of(StatType.STRENGTH, 35.0, StatType.ENDURANCE, 30.0)),
    HALF_DRAGON    ("하프드래곤", 0.1,  Map.of(StatType.DRAGON_POWER, 100.0, StatType.STRENGTH, 25.0)),

    // ─── 마계 ───
    LESSER_DEMON   ("하급악마",   0.9,  Map.of(StatType.DEMON_KI, 50.0)),
    SUCCUBUS       ("서큐버스",   0.5,  Map.of(StatType.CHARM, 50.0, StatType.DEMON_KI, 30.0)),
    INCUBUS        ("인큐버스",   0.5,  Map.of(StatType.CHARISMA, 40.0, StatType.DEMON_KI, 30.0)),
    DEMI_GOD       ("반신",       0.05, Map.of(StatType.DIVINITY, 50.0, StatType.STRENGTH, 50.0)),

    // ─── 천계 ───
    ANGEL_NOVICE   ("천사 견습",  0.8,  Map.of(StatType.HEAVEN_KI, 50.0, StatType.MENTAL, 20.0)),
    SERAPH_KIN     ("세라프 혈통",0.1,  Map.of(StatType.HEAVEN_KI, 200.0, StatType.DIVINITY, 30.0)),

    // ─── 정령계 ───
    SPIRIT_HUMAN   ("정령 친화 인간", 1.0, Map.of(StatType.SPIRIT_POWER, 30.0)),
    HALF_SPIRIT    ("반정령",     0.4,  Map.of(StatType.SPIRIT_POWER, 100.0, StatType.MANA, 50.0)),

    // ─── 무협 ───
    MARTIAL_HUMAN  ("무인",       1.0,  Map.of(StatType.INNER_KI, 50.0, StatType.MENTAL, 10.0)),
    HEAVENLY_BLOOD ("천생기재",   0.05, Map.of(StatType.INNER_KI, 500.0, StatType.MENTAL, 50.0)),

    // ─── 선계 ───
    IMMORTAL_NOVICE("선인 견습",  0.8,  Map.of(StatType.IMMORTAL_KI, 50.0)),
    SPIRIT_CHILD   ("선택받은 자",0.05, Map.of(StatType.IMMORTAL_KI, 300.0, StatType.LUCK, 30.0)),

    // ─── 요계 ───
    KITSUNE        ("구미호 일족",0.5,  Map.of(StatType.YOKAI_KI, 80.0, StatType.CHARM, 30.0)),
    ONI            ("오니",       0.6,  Map.of(StatType.STRENGTH, 50.0, StatType.YOKAI_KI, 30.0)),
    TANUKI         ("너구리 요괴",0.4,  Map.of(StatType.LUCK, 40.0, StatType.YOKAI_KI, 20.0)),
    YOKAI_KING_BLOOD("요왕 혈통",0.05,  Map.of(StatType.YOKAI_KI, 500.0, StatType.CHARISMA, 50.0)),

    // ─── 지구 ───
    AWAKENED       ("각성자",     1.0,  Map.of(StatType.LEVEL, 5.0)),
    HUNTER_KIN     ("헌터 혈통",  0.3,  Map.of(StatType.STRENGTH, 30.0, StatType.AGILITY, 30.0)),

    // ─── 마도공학 ───
    GNOME          ("노움",       0.7,  Map.of(StatType.INTELLIGENCE, 40.0, StatType.MAGITECH_ENERGY, 50.0)),
    GOLEM_BORN     ("골렘의 후예",0.1,  Map.of(StatType.MAGITECH_ENERGY, 200.0, StatType.ENDURANCE, 50.0)),

    // ─── 아포칼립스 ───
    SURVIVOR       ("생존자",     1.0,  Map.of(StatType.ENDURANCE, 20.0, StatType.LUCK, 10.0)),
    MUTANT         ("변종 인간",  0.3,  Map.of(StatType.STRENGTH, 40.0, StatType.AGILITY, -10.0)),

    // ─── 사이버펑크 ───
    NEUROBOOSTER   ("뉴로부스터",  0.7, Map.of(StatType.INTELLIGENCE, 30.0, StatType.CYBER_ADAPTATION, 30.0)),
    CHROME_BORN    ("크롬 인간",  0.3,  Map.of(StatType.CYBER_ADAPTATION, 80.0, StatType.MENTAL, -10.0)),

    // ─── 드래곤 ───
    DRAGONLING     ("어린 용",    0.9,  Map.of(StatType.DRAGON_POWER, 50.0, StatType.ENDURANCE, 20.0)),
    ANCIENT_DRAGON ("고룡",       0.03, Map.of(StatType.DRAGON_POWER, 1000.0, StatType.STRENGTH, 100.0)),

    // ─── 해양 ───
    HUMAN_SAILOR   ("인간 선원",  1.0,  Map.of(StatType.OCEAN_POWER, 20.0, StatType.AGILITY, 10.0)),
    MERFOLK        ("인어",       0.3,  Map.of(StatType.OCEAN_POWER, 100.0, StatType.CHARM, 30.0)),
    KRAKEN_SPAWN   ("크라켄 후손",0.05, Map.of(StatType.OCEAN_POWER, 500.0, StatType.STRENGTH, 50.0)),
    FROST_GIANT    ("서리 거인",  0.02, Map.of(StatType.STRENGTH, 200.0, StatType.ENDURANCE, 100.0));

    public final String koreanName;
    /** 가중치 (1.0 = 일반, 0.05 = 5% 희귀) */
    public final double weight;
    public final Map<StatType, Double> bonus;

    Race(String koreanName, double weight, Map<StatType, Double> bonus) {
        this.koreanName = koreanName;
        this.weight = weight;
        this.bonus = bonus;
    }

    /** 세계별 가능한 종족 목록. */
    private static final Map<WorldKey, List<Race>> WORLD_RACES = new EnumMap<>(WorldKey.class);
    static {
        WORLD_RACES.put(WorldKey.FANTASY,
                List.of(HUMAN, HIGH_ELF, DARK_ELF, DWARF, HALFLING, ORC, HALF_DRAGON));
        WORLD_RACES.put(WorldKey.DEMON,
                List.of(LESSER_DEMON, SUCCUBUS, INCUBUS, DEMI_GOD));
        WORLD_RACES.put(WorldKey.HEAVEN,
                List.of(ANGEL_NOVICE, SERAPH_KIN));
        WORLD_RACES.put(WorldKey.SPIRIT,
                List.of(SPIRIT_HUMAN, HALF_SPIRIT));
        WORLD_RACES.put(WorldKey.MARTIAL,
                List.of(MARTIAL_HUMAN, HEAVENLY_BLOOD));
        WORLD_RACES.put(WorldKey.IMMORTAL,
                List.of(IMMORTAL_NOVICE, SPIRIT_CHILD));
        WORLD_RACES.put(WorldKey.YOKAI,
                List.of(KITSUNE, ONI, TANUKI, YOKAI_KING_BLOOD));
        WORLD_RACES.put(WorldKey.EARTH,
                List.of(AWAKENED, HUNTER_KIN));
        WORLD_RACES.put(WorldKey.MAGITECH,
                List.of(GNOME, GOLEM_BORN));
        WORLD_RACES.put(WorldKey.APOCALYPSE,
                List.of(SURVIVOR, MUTANT));
        WORLD_RACES.put(WorldKey.CYBERPUNK,
                List.of(NEUROBOOSTER, CHROME_BORN));
        WORLD_RACES.put(WorldKey.DRAGON,
                List.of(DRAGONLING, ANCIENT_DRAGON));
        WORLD_RACES.put(WorldKey.OCEAN,
                List.of(HUMAN_SAILOR, MERFOLK, KRAKEN_SPAWN, FROST_GIANT));
    }

    public static List<Race> availableFor(WorldKey w) {
        return WORLD_RACES.getOrDefault(w, java.util.Collections.emptyList());
    }
}
