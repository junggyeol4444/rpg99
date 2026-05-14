package kr.reborn.skill.def;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.skill.RebornSkill;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class SkillRegistry {

    private final RebornSkill plugin;
    private final Map<String, SkillDef> defs = new HashMap<>();

    public SkillRegistry(RebornSkill p) { this.plugin = p; }

    public void load() {
        defs.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("skills");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            WorldKey w;
            try { w = WorldKey.valueOf(s.getString("world", "FANTASY")); }
            catch (Exception e) { w = WorldKey.FANTASY; }
            ConfigurationSection cost = s.getConfigurationSection("cost");
            StatType ct = null;
            double ca = 0;
            if (cost != null) {
                try { ct = StatType.valueOf(cost.getString("type", "MANA")); }
                catch (Exception e) { ct = StatType.MANA; }
                ca = cost.getDouble("amount", 0);
            }
            defs.put(id, new SkillDef(
                    id,
                    s.getString("name", id),
                    w,
                    s.getString("category", "MISC"),
                    ct, ca,
                    s.getDouble("cooldown-seconds", 1),
                    s.getDouble("cast-seconds", 0),
                    s.getString("damage", null),
                    s.getString("element", "PHYSICAL"),
                    s.getString("learn", "AUTO")
            ));
        }
    }

    public SkillDef get(String id) { return defs.get(id); }
    public java.util.Collection<SkillDef> all() { return defs.values(); }
}
