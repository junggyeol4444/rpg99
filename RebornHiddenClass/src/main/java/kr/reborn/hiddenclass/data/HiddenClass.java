package kr.reborn.hiddenclass.data;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HiddenClass {
    public enum Type { INITIAL, ACHIEVEMENT }

    public final String id;
    public final Type type;
    public final String name;
    public final String description;
    public final WorldKey worldRestriction;   // null = ANY
    public final double initialChance;         // INITIAL 전용
    public final List<Condition> conditions = new ArrayList<>();
    public final Map<StatType, Double> statBonuses = new EnumMap<>(StatType.class);
    public final Map<StatType, Double> statOverrides = new EnumMap<>(StatType.class);
    public final List<String> skills = new ArrayList<>();
    public final List<String> resistances = new ArrayList<>();
    public final String passive;

    public HiddenClass(String id, Type type, String name, String description,
                       WorldKey w, double chance, String passive) {
        this.id = id; this.type = type; this.name = name; this.description = description;
        this.worldRestriction = w; this.initialChance = chance; this.passive = passive;
    }
}
