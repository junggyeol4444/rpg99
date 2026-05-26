package kr.reborn.skill.def;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.skill.effect.SkillType;

public final class SkillDef {

    public final String id;
    public final String name;
    public final WorldKey world;
    public final String category;
    public final StatType costType;
    public final double costAmount;
    public final double cooldownSeconds;
    public final double castSeconds;
    public final String damageFormula;   // 변수: stat 이름 (소문자)
    public final String element;
    public final String learnMethod;     // AUTO / BOOK / NPC / ACHIEVEMENT / CREATE

    // ─── Step 2: 효과 동작 정의 ───
    public final SkillType type;
    public final double radius;          // AOE/CHAIN 반경
    public final double range;           // MELEE/BLINK 사거리 (0=타입별 기본값)
    public final double projectileSpeed; // PROJECTILE/DASH 속도 (0=기본)
    public final int durationTicks;      // BUFF/DOT 지속 (0=타입별 기본)
    public final String summonMob;       // SUMMON 대상 (EntityType 또는 RebornMob id)

    public SkillDef(String id, String name, WorldKey world, String category,
                    StatType costType, double costAmount,
                    double cooldown, double cast, String damage,
                    String element, String learn,
                    SkillType type, double radius, double range,
                    double projectileSpeed, int durationTicks, String summonMob) {
        this.id = id; this.name = name; this.world = world; this.category = category;
        this.costType = costType; this.costAmount = costAmount;
        this.cooldownSeconds = cooldown; this.castSeconds = cast;
        this.damageFormula = damage; this.element = element;
        this.learnMethod = learn;
        this.type = type; this.radius = radius; this.range = range;
        this.projectileSpeed = projectileSpeed; this.durationTicks = durationTicks;
        this.summonMob = summonMob;
    }
}
