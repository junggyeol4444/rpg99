package kr.reborn.skill.manual;

import kr.reborn.core.data.WorldKey;

/**
 * 비급(秘笈) 정의 — 발견·연구·도난·전수 가능한 무공/마법서.
 *
 * 비급은 본 SkillRegistry의 스킬과 1:1 매핑되지만, 별도로:
 * - 발견 가능 (위치·NPC·드롭)
 * - 도난 가능 (NPC 인벤토리에서)
 * - 연구 시간 필요 (즉시 학습 안 됨)
 * - 미공개 비급 (rarity LEGENDARY 등)
 */
public final class SecretManual {

    public enum Rarity {
        COMMON   (0.5, 100, "일반"),
        RARE     (0.2, 500, "희귀"),
        EPIC     (0.05, 2000, "영웅"),
        LEGENDARY(0.01, 10000, "전설"),
        MYTHIC   (0.001, 50000, "신화");

        public final double dropChance;
        public final long marketValue;
        public final String koreanName;
        Rarity(double d, long v, String k) {
            this.dropChance = d; this.marketValue = v; this.koreanName = k;
        }
    }

    public final String id;
    public final String name;
    public final String skillId;
    public final Rarity rarity;
    public final WorldKey world;
    /** 연구 시간 (분) */
    public final int researchMinutes;
    /** 입수 조건 (NPC id 또는 위치 tag) */
    public final String foundAt;
    public final String description;

    public SecretManual(String id, String name, String skillId, Rarity rarity,
                        WorldKey world, int researchMinutes, String foundAt, String description) {
        this.id = id; this.name = name; this.skillId = skillId;
        this.rarity = rarity; this.world = world;
        this.researchMinutes = researchMinutes;
        this.foundAt = foundAt;
        this.description = description;
    }
}
