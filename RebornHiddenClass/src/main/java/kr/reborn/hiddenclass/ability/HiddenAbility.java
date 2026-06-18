package kr.reborn.hiddenclass.ability;

/**
 * 히든 클래스 고유 능력. 클래스마다 1~2개씩 배정.
 *
 * ACTIVE: /hiddenclass cast <abilityId>로 발동, 쿨다운·자원 소비.
 * PASSIVE: 보유만 해도 자동 적용, AbilityEngine.tickPassives()가 매 주기 처리.
 */
public enum HiddenAbility {

    // ─── 초기 조건형 (10) ───
    DIVINE_FAVOR        (true,  600_000,  "신의 후계자",        "&e행운 +30, 신성 미세 누적"),
    CHAOS_BURST         (false, 60_000,   "혼돈의 아이",        "&5모든 원소 동시 폭발 — 광역 강타"),
    DEMON_KI_OVERFLOW   (false, 90_000,   "천생마골",           "&4마기 폭주 — 30초간 데미지·속도 ×2"),
    IMMORTAL_REVIVE     (true,  0,        "불멸자",             "&6최초 1회 사망 시 즉시 부활 + 영구 10% 강화"),
    DRAGON_BREATH_AURA  (true,  -1,       "용의 피",            "&c공격에 화염 + 비행 가능"),
    NAMELESS_STEALTH    (true,  -1,       "무명인",             "&7랭킹 비표시 + NPC 무인식, 은신·암살 +50%"),
    GATE_RESONANCE      (true,  -1,       "게이트 적응자",      "&3게이트 내 전 스탯 +20%, 약점 자동 표시"),
    SPIRIT_SIGHT        (false, 30_000,   "천안통",             "&5타인 경지 + 적 약점 시각화 60초"),
    TRANSFORM_ROAR      (false, 120_000,  "요왕의 자질",        "&d완전 변신 + 광역 위압 (적 슬로우+위더)"),
    CYBER_IMMUNE        (true,  -1,       "기계와의 친화",      "&b사이버 사이코 면역 + 과도 개조 페널티 0"),

    // ─── 성장형 (15 핵심) ───
    DUAL_CAST           (false, 30_000,   "마검사",             "&6한 번에 마법 + 검술 동시 시전"),
    CULT_COMMAND        (false, 600_000,  "천마",               "&5주변 마교 NPC 30초 직속 지휘"),
    FORBIDDEN_POWER     (false, 300_000,  "마선",               "&5선기+마기 동시 폭주, 1분간 데미지 ×2.5"),
    DEMON_LORD_AURA     (true,  -1,       "마왕",               "&4반경 20 적에게 위더 + 약화 지속"),
    YOKAI_EMPEROR       (false, 600_000,  "요제",               "&5주변 요괴 모두 직속화, 광역 매혹"),
    DRAGON_LORD_ROAR    (false, 300_000,  "용왕",               "&6&l광역 위압 — 반경 30 적 기절 + 자기 강화"),
    APOCALYPSE_COMMAND  (false, 600_000,  "종말의 왕",          "&8적 모두 슬로우 + 광역 방사능 피해"),
    ABYSS_TOUCH         (false, 180_000,  "심연의 사도",        "&0대상 광역 위더 + 4초 무력화"),
    SOUL_TRAVEL         (false, 60_000,   "환혼자",             "&7눈에 보이는 곳으로 즉시 텔레포트"),
    TIME_REWIND         (false, 300_000,  "시간 수호자 견습생", "&e5초 전 위치·체력으로 되돌림"),
    DREAM_INTRUSION     (false, 240_000,  "꿈의 지배자",        "&d대상 5초간 수면(무방비)"),
    LABYRINTH_TELEPORT  (false, 30_000,   "미궁의 주인",        "&5미궁 임의 층으로 즉시 이동"),
    AI_COMMAND          (false, 300_000,  "메가코프 황제",      "&b반경 50 사이버 골렘/드론 직속 지휘"),
    DUNGEON_DECREE      (true,  -1,       "던전 마스터",        "&5던전 내 몹 스폰·트랩 조작권"),
    HERO_AURA           (true,  -1,       "신 후계자 (성장형)", "&6&l신성 +미세, 신도 자동 모임"),
    SEA_KING_COMMAND    (false, 300_000,  "해왕의 후예",        "&3해양 생물 모두 호의적 + 광역 수중 회복"),
    GENESIS_BLESSING    (true,  -1,       "창세신기 소유자",    "&6&l장착 시 추가 절대자급 보정"),
    MASTER_OF_TRADES    (true,  -1,       "만물의 장인",        "&7제작 재료 30% 절감 + 전 레시피 접근"),
    PRIMORDIAL_CONNECT  (false, 600_000,  "태초의 계승자",      "&f빛/어둠 절대 원소 1분 발현"),
    PIRATE_KING_FLAG    (true,  -1,       "해적왕",             "&6해적 NPC 직속 + 해양력 ×1.5"),
    DREAM_LORD_PASSIVE  (true,  -1,       "꿈의 지배자",        "&d수면 시간 = 수련 시간 (식사 게이지 낮을 때)");

    public final boolean passive;     // true = 자동 적용, false = ACTIVE
    public final long cooldownMs;     // -1 = 영구, 0 = 1회 한정
    public final String classId;       // 보유 조건 — 이 클래스 가진 자만 사용 가능
    public final String description;

    HiddenAbility(boolean passive, long cooldownMs, String classId, String description) {
        this.passive = passive;
        this.cooldownMs = cooldownMs;
        this.classId = classId;
        this.description = description;
    }
}
