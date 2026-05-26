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
            String name = s.getString("name", id);
            String category = s.getString("category", "MISC");
            String damage = s.getString("damage", null);
            String element = s.getString("element", "PHYSICAL");
            // 반경: aoe-radius(기존) 또는 radius 둘 다 인정
            double radius = s.getDouble("aoe-radius", s.getDouble("radius", 0));
            double range = s.getDouble("range", 0);
            double projSpeed = s.getDouble("projectile-speed", 0);
            int duration = s.getInt("duration-ticks", 0);
            String summon = s.getString("summon-mob", null);
            // type: 명시값이 있으면 사용, 없으면 기존 필드에서 자동 추론
            kr.reborn.skill.effect.SkillType type = kr.reborn.skill.effect.SkillType.infer(
                    s.getString("type", null), damage, radius, category, id, name);
            defs.put(id, new SkillDef(
                    id, name, w, category,
                    ct, ca,
                    s.getDouble("cooldown-seconds", 1),
                    s.getDouble("cast-seconds", 0),
                    damage, element,
                    s.getString("learn", "AUTO"),
                    type, radius, range, projSpeed, duration, summon
            ));
        }
    }

    public SkillDef get(String id) { return defs.get(id); }
    public java.util.Collection<SkillDef> all() { return defs.values(); }
}
