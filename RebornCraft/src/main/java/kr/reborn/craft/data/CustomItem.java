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

    public enum ConsumeType { HEAL, STAT_BOOST, CURE, BLESS, ENERGY, LEARN_SKILL, CUSTOM }

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

    public CustomItem(String id, Material base, int model, String name,
                      Grade grade, Type type) {
        this.id = id; this.base = base; this.model = model;
        this.name = name; this.grade = grade; this.type = type;
        this.durability = -1;
    }
}
