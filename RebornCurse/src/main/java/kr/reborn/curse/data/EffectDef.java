package kr.reborn.curse.data;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 축복 또는 저주 정의 (config 매핑). */
public final class EffectDef {
    public enum Kind { BLESSING, CURSE }

    public final String id;
    public final Kind kind;
    public final String name;
    public final String description;
    /** -1 = 영구. 단위: 초. */
    public final long durationSeconds;
    public final int maxStacks;
    public final int tickIntervalSeconds;
    public final WorldKey worldRestriction;
    /** 즉시 적용 (영구) 스탯 보정 */
    public final Map<StatType, Double> staticStats = new EnumMap<>(StatType.class);
    /** tick 마다 적용 스탯 변동 */
    public final Map<StatType, Double> tickStats = new EnumMap<>(StatType.class);
    /** 모든 8공통 스탯에 적용되는 tick 변동 */
    public final double tickStatsCommon;
    /** 광폭화 발동 확률 (0~1) */
    public final double berserkChance;
    /** 비율 보정 (예: -0.5 = 50% 감소) */
    public final Map<StatType, Double> percentStats = new EnumMap<>(StatType.class);
    public final List<String> cureMethods = new ArrayList<>();
    public final String special;

    public EffectDef(String id, Kind kind, String name, String description,
                     long durationSeconds, int maxStacks, int tickIntervalSeconds,
                     WorldKey worldRestriction, double tickStatsCommon, double berserkChance,
                     String special) {
        this.id = id; this.kind = kind; this.name = name; this.description = description;
        this.durationSeconds = durationSeconds; this.maxStacks = maxStacks;
        this.tickIntervalSeconds = tickIntervalSeconds; this.worldRestriction = worldRestriction;
        this.tickStatsCommon = tickStatsCommon; this.berserkChance = berserkChance;
        this.special = special;
    }

    public boolean permanent() { return durationSeconds < 0; }

    public static EffectDef parse(String id, Kind kind, ConfigurationSection s) {
        long dur = s.getLong("duration", -1);
        int maxStacks = s.getInt("max_stacks", 1);
        int tick = s.getInt("tick_interval_seconds", 0);
        WorldKey w = null;
        try { w = WorldKey.valueOf(s.getString("world", "")); }
        catch (IllegalArgumentException ignored) {}
        ConfigurationSection eff = s.getConfigurationSection("effects");
        double tickCommon = eff != null ? eff.getDouble("stats_tick_common", 0) : 0;
        double berserk = eff != null ? eff.getDouble("berserk_chance", 0) : 0;
        String special = eff != null ? eff.getString("special", null) : null;
        EffectDef d = new EffectDef(id, kind, s.getString("name", id),
                s.getString("description", ""), dur, maxStacks, tick, w,
                tickCommon, berserk, special);
        if (eff != null) {
            mapInto(eff.getConfigurationSection("stats"), d.staticStats);
            mapInto(eff.getConfigurationSection("stats_tick"), d.tickStats);
            mapInto(eff.getConfigurationSection("stats_percent"), d.percentStats);
        }
        d.cureMethods.addAll(s.getStringList("cure_methods"));
        return d;
    }

    private static void mapInto(ConfigurationSection sec, Map<StatType, Double> target) {
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try { target.put(StatType.valueOf(k.toUpperCase()), sec.getDouble(k)); }
            catch (IllegalArgumentException ignored) {}
        }
    }
}
