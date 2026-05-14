package kr.reborn.skill.def;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;

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

    public SkillDef(String id, String name, WorldKey world, String category,
                    StatType costType, double costAmount,
                    double cooldown, double cast, String damage,
                    String element, String learn) {
        this.id = id; this.name = name; this.world = world; this.category = category;
        this.costType = costType; this.costAmount = costAmount;
        this.cooldownSeconds = cooldown; this.castSeconds = cast;
        this.damageFormula = damage; this.element = element;
        this.learnMethod = learn;
    }
}
