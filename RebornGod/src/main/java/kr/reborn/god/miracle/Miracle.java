package kr.reborn.god.miracle;

/**
 * 신이 신성(divinity)을 소비해 발동하는 기적 종류.
 *
 * 각 기적은 신성 비용·쿨다운·요구 등급(하급신~개념신)·효과를 가진다.
 * MiracleEngine.cast()가 실제 효과를 적용한다.
 */
public enum Miracle {

    // ─── 하급신 ───
    BLESS_FOLLOWERS(50,   60_000,   "하급신", "&a신도들에게 축복 — 5분간 +5% 스탯"),
    DIVINE_SHIELD(80,     120_000,  "하급신", "&b신성 방패 — 자신 또는 신도 1명 무적 30초"),
    GUIDING_LIGHT(30,     30_000,   "하급신", "&e인도의 빛 — 신도 1명 위치 표시·소환"),

    // ─── 중급신 ───
    MASS_HEAL(200,        180_000,  "중급신", "&a대규모 회복 — 반경 50 신도 풀회복"),
    SMITE_TARGET(150,     90_000,   "중급신", "&c천벌 — 적 1명에게 신성 대미지(낙뢰 + 위더)"),
    INSPIRE_NATION(300,   600_000,  "중급신", "&6국가 사기 — 한 세계 모든 신도 1시간 공격력 +30%"),
    WORLD_RAIN_BLESSING(100, 600_000, "중급신", "&3세계 강우의 축복 — 신도가 있는 세계에 비"),

    // ─── 상급신 ───
    JUDGMENT(1000,        1_800_000, "상급신", "&e&l심판 — 범죄자/적대자 광역 처단"),
    PILLAR_OF_FIRE(800,   900_000,  "상급신", "&c&l불기둥 — 좌표 강타, 반경 20 대AOE"),
    DIVINE_INTERVENTION(2000, 3_600_000, "상급신", "&6&l신적 개입 — 진행중 월드 퀘스트에 신탁"),

    // ─── 주신 ───
    AVATAR_DESCENT(5000,  43_200_000, "주신", "&5&l화신 강림 — 30분간 본체로 강림(모든 스탯 ×3)"),
    RESURRECT_FOLLOWER(3000, 7_200_000, "주신", "&a&l부활 — 사망한 신도 1명 즉시 부활(스탯 유지)"),

    // ─── 개념신 ───
    CONCEPT_DOMINION(10000, 604_800_000, "개념신", "&0&l개념 권능 — 자기 신역 안에서 절대 법칙"),
    REWRITE_REALITY(50000, -1, "개념신", "&6&l&n현실 재기록 — 1회 한정. 한 사건의 결과를 바꿈");

    public final double cost;
    public final long cooldownMs;
    public final String requiredTier;
    public final String description;

    Miracle(double cost, long cooldownMs, String requiredTier, String desc) {
        this.cost = cost;
        this.cooldownMs = cooldownMs;
        this.requiredTier = requiredTier;
        this.description = desc;
    }

    /** 등급 인덱스 — 신성 등급별 사용 가능 기적 필터링용. */
    public int tierIndex() {
        switch (requiredTier) {
            case "하급신": return 0;
            case "중급신": return 1;
            case "상급신": return 2;
            case "주신":   return 3;
            case "개념신": return 4;
            default: return 0;
        }
    }

    public static int tierOf(String tier) {
        switch (tier) {
            case "하급신": return 0;
            case "중급신": return 1;
            case "상급신": return 2;
            case "주신":   return 3;
            case "개념신": return 4;
            default: return 0;
        }
    }
}
