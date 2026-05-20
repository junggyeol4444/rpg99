package kr.reborn.title.data;

import kr.reborn.core.data.StatType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;

/** 칭호 정의. */
public final class Title {
    public enum Type { ACHIEVEMENT, TIER, CLAN, NPC, WORLD_QUEST, AI }
    public enum ReqType { KILL_COUNT, WORLDS_VISITED, TIER, CLAN_RANK, NPC_FAVOR, ADMIN_GRANT }

    public final String id;
    public final Type type;
    public final String name;
    public final String description;
    public final ReqType reqType;
    public final Object reqValue;
    public final Map<StatType, Double> statBonuses = new EnumMap<>(StatType.class);
    public final String special;  // 코드로 별도 처리되는 효과 키

    public Title(String id, Type type, String name, String description,
                 ReqType reqType, Object reqValue, String special) {
        this.id = id; this.type = type; this.name = name; this.description = description;
        this.reqType = reqType; this.reqValue = reqValue; this.special = special;
    }

    public static Title fromConfig(String id, ConfigurationSection sec) {
        Type type = Type.valueOf(sec.getString("type", "ACHIEVEMENT"));
        String name = sec.getString("name", id);
        String desc = sec.getString("description", "");
        ConfigurationSection req = sec.getConfigurationSection("requirement");
        ReqType reqType = req == null ? ReqType.ADMIN_GRANT
                : ReqType.valueOf(req.getString("type", "ADMIN_GRANT"));
        Object reqValue = req == null ? null : req.get("value");
        String special = sec.getString("effects.special", null);
        Title t = new Title(id, type, name, desc, reqType, reqValue, special);
        ConfigurationSection statSec = sec.getConfigurationSection("effects.stats");
        if (statSec != null) {
            for (String key : statSec.getKeys(false)) {
                try {
                    t.statBonuses.put(StatType.valueOf(key.toUpperCase()), statSec.getDouble(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return t;
    }
}
