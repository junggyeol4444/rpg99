package kr.reborn.craft.data;

/** 7단계 아이템 등급. */
public enum Grade {
    COMMON, UNCOMMON, RARE, HEROIC, LEGENDARY, MYTHIC, GENESIS;

    public boolean atLeast(Grade g) {
        return this.ordinal() >= g.ordinal();
    }
}
