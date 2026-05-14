package kr.reborn.hiddenclass.data;

import kr.reborn.core.data.StatType;

/** 단일 조건 (AND 조합은 HiddenClass.conditions 리스트). */
public final class Condition {
    public enum Type {
        RANDOM_ON_SPAWN, STAT_MIN, TIER_REACHED, WORLDS_VISITED,
        QUEST_COMPLETE, ITEM_OWNED, NPC_FAVOR, KILL_COUNT,
        CLAN_RANK, TRADE_COUNT, MULTI, ADMIN_GRANT
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
