package kr.reborn.hiddenclass.data;

import kr.reborn.core.data.StatType;

/** 단일 조건 (AND 조합은 HiddenClass.conditions 리스트). */
public final class Condition {
    public enum Type {
        RANDOM_ON_SPAWN,
        STAT_MIN,
        TIER_REACHED,
        WORLDS_VISITED,
        QUEST_COMPLETE,
        ITEM_OWNED,           // stringValue = material name, numericValue = required count
        NPC_FAVOR,
        KILL_COUNT,
        CLAN_RANK,            // stringValue = required rank id (e.g. "ELDER"), numericValue = min member count
        TRADE_COUNT,
        MULTI,                // stringValue = "X|Y|Z" condition group ids — at least N of them must hold (numericValue=N)
        REINCARNATION_MIN,    // numericValue = required reincarnations
        DEATH_COUNT,          // numericValue = required deaths (e.g. 환혼자)
        SKILL_LEARNED,        // stringValue = skill id (RebornSkill reflection)
        PLAYTIME_MIN,         // numericValue = required playtime in seconds
        AGE_MIN,              // numericValue = required dragon age (DRAGON world)
        WORLD,                // stringValue = WorldKey name (현재 거주 세계 일치)
        CRAFT_MASTERY,        // numericValue = 마스터한 직업 분야 수
        ADMIN_GRANT
    }

    public final Type type;
    public final StatType stat;
    public final String stringValue;
    public final double numericValue;

    public Condition(Type type, StatType stat, String stringValue, double numericValue) {
        this.type = type; this.stat = stat;
        this.stringValue = stringValue; this.numericValue = numericValue;
    }
}
