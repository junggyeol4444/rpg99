package kr.reborn.craft.data;

import kr.reborn.core.data.StatType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 커스텀 아이템 정의 (config). */
public final class CustomItem {
    public enum Type {
        WEAPON, ARMOR, ACCESSORY, CONSUMABLE, SKILL_BOOK,
        MATERIAL, TOOL, MOUNT_ITEM, PET_ITEM, CURRENCY_ITEM, QUEST_ITEM, MISC
    }

    public enum AccessorySlot { RING, NECKLACE, EARRING }

    public enum ConsumeType {
        HEAL, STAT_BOOST, CURE, BLESS, ENERGY, LEARN_SKILL, CUSTOM,
        ADD_STAT, ADD_ALL_COMMON, ADD_MULTI, ADD_RANDOM_COMMON,
        REVIVE, BUFF, DEBUFF, CURE_CURSE, ANTI_PARANOIA,
        RESTORE_MERIDIAN, BUFF_RECIPE, BUFF_TRAIN,
        ALL_ELEMENTS, TIER_UP, TIER_UP_CIRCLE,
        STOP_AGING, TRIBULATION_BOOST,
        LEARN_RANDOM_SPIRIT_SKILL
    }

    public final String id;
    public final Material base;
    public final int model;
    public final String name;
    public final List<String> lore = new ArrayList<>();
    public final Grade grade;
    public final Type type;
    public final Map<StatType, Double> stats = new EnumMap<>(StatType.class);
    public String skill;
    public int durability;       // -1 = 무한
    public boolean tradable = true;
    public boolean droppable = true;
    public boolean unique = false;
    public AccessorySlot accessorySlot;
    public ConsumeType consumeType;
    public Object consumeValue;
    public int consumeCooldownSeconds;
    /** 단일 스탯 (ADD_STAT, BUFF 등) */
    public StatType consumeStat;
    /** 다중 스탯 (ADD_MULTI) — stat → value */
    public final Map<StatType, Double> consumeMultiStats = new EnumMap<>(StatType.class);
    /** 효과 지속시간 초 (BUFF, DEBUFF) */
    public int consumeDuration;
    /** 최소/최대 범위 (ADD_RANDOM_COMMON) */
    public double consumeMin;
    public double consumeMax;

    public CustomItem(String id, Material base, int model, String name,
                      Grade grade, Type type) {
        this.id = id; this.base = base; this.model = model;
        this.name = name; this.grade = grade; this.type = type;
        this.durability = -1;
    }
}
