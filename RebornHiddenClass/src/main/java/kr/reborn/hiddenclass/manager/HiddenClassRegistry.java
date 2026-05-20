package kr.reborn.hiddenclass.manager;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.hiddenclass.RebornHiddenClass;
import kr.reborn.hiddenclass.data.Condition;
import kr.reborn.hiddenclass.data.HiddenClass;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class HiddenClassRegistry {

    private final RebornHiddenClass plugin;
    private final Map<String, HiddenClass> classes = new HashMap<>();

    public HiddenClassRegistry(RebornHiddenClass plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("classes");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try { classes.put(id, parse(id, s)); }
            catch (Exception e) {
                plugin.getLogger().warning("히든 클래스 로드 실패: " + id + " " + e.getMessage());
            }
        }
    }

    private HiddenClass parse(String id, ConfigurationSection s) {
        HiddenClass.Type type = HiddenClass.Type.valueOf(s.getString("type", "ACHIEVEMENT"));
        WorldKey w = null;
        String worldStr = s.getString("world", "ANY");
        if (!"ANY".equals(worldStr)) {
            try { w = WorldKey.valueOf(worldStr); } catch (Exception ignored) {}
        }
        double chance = s.getDouble("chance", 0);
        ConfigurationSection eff = s.getConfigurationSection("effects");
        String passive = eff != null ? eff.getString("passive", null) : null;
        HiddenClass hc = new HiddenClass(id, type, s.getString("name", id),
                s.getString("description", ""), w, chance, passive);

        if (eff != null) {
            ConfigurationSection stats = eff.getConfigurationSection("stats");
            if (stats != null) {
                for (String k : stats.getKeys(false)) {
                    try { hc.statBonuses.put(StatType.valueOf(k.toUpperCase()), stats.getDouble(k)); }
                    catch (Exception ignored) {}
                }
            }
            ConfigurationSection ov = eff.getConfigurationSection("stats_override");
            if (ov != null) {
                for (String k : ov.getKeys(false)) {
                    try { hc.statOverrides.put(StatType.valueOf(k.toUpperCase()), ov.getDouble(k)); }
                    catch (Exception ignored) {}
                }
            }
            hc.skills.addAll(eff.getStringList("skills"));
            hc.resistances.addAll(eff.getStringList("resistance"));
        }
        // 조건 파싱
        for (Map<?, ?> raw : s.getMapList("conditions")) {
            Condition.Type ct;
            try { ct = Condition.Type.valueOf(String.valueOf(raw.get("type"))); }
            catch (Exception ex) { continue; }
            StatType stat = null;
            Object statRaw = raw.get("stat");
            if (statRaw != null) {
                try { stat = StatType.valueOf(String.valueOf(statRaw).toUpperCase()); }
                catch (Exception ignored) {}
            }
            String tier = String.valueOf(raw.getOrDefault("tier", ""));
            double value = raw.get("value") instanceof Number n ? n.doubleValue() : 0;
            hc.conditions.add(new Condition(ct, stat, tier, value));
        }
        return hc;
    }

    public HiddenClass get(String id) { return classes.get(id); }
    public Collection<HiddenClass> all() { return classes.values(); }
}
