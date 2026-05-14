package kr.reborn.mob.def;

import kr.reborn.core.data.WorldKey;
import kr.reborn.mob.RebornMob;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobRegistry {

    private final RebornMob plugin;
    private final Map<String, MobDef> defs = new HashMap<>();

    public MobRegistry(RebornMob p) { this.plugin = p; }

    public void load() {
        defs.clear();
        loadFrom("monsters", false);
        loadFrom("bosses", true);
        plugin.getLogger().info("몬스터 정의 " + defs.size() + "종 로드");
    }

    private void loadFrom(String key, boolean boss) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection(key);
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            EntityType base;
            try { base = EntityType.valueOf(s.getString("base", "ZOMBIE").toUpperCase()); }
            catch (Exception e) { base = EntityType.ZOMBIE; }
            WorldKey w;
            try { w = WorldKey.valueOf(s.getString("world", "FANTASY").toUpperCase()); }
            catch (Exception e) { w = WorldKey.FANTASY; }
            MobAI ai;
            try { ai = MobAI.valueOf(s.getString("ai", "BASIC").toUpperCase()); }
            catch (Exception e) { ai = MobAI.BASIC; }
            MobDef def = new MobDef(id, base, s.getString("name", id), w,
                    s.getDouble("hp", 20), s.getDouble("damage", 4), s.getDouble("speed", 0.25),
                    ai, boss, s.getInt("phases", 1));
            List<Map<?, ?>> drops = s.getMapList("drops");
            for (Map<?, ?> m : drops) {
                String item = String.valueOf(m.get("item"));
                double chance = m.get("chance") == null ? 1.0 : ((Number) m.get("chance")).doubleValue();
                int min = 1, max = 1;
                Object amt = m.get("amount");
                if (amt instanceof List<?> arr && arr.size() == 2) {
                    min = ((Number) arr.get(0)).intValue();
                    max = ((Number) arr.get(1)).intValue();
                }
                def.drops.add(new MobDef.DropEntry(item, chance, min, max));
            }
            defs.put(id, def);
        }
    }

    public MobDef get(String id) { return defs.get(id); }
    public java.util.Collection<MobDef> all() { return defs.values(); }
}
