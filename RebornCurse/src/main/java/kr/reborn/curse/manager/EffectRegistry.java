package kr.reborn.curse.manager;

import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.EffectDef;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {

    private final RebornCurse plugin;
    private final Map<String, EffectDef> blessings = new HashMap<>();
    private final Map<String, EffectDef> curses = new HashMap<>();

    public EffectRegistry(RebornCurse plugin) {
        this.plugin = plugin;
        load("blessings", EffectDef.Kind.BLESSING, blessings);
        load("curses", EffectDef.Kind.CURSE, curses);
    }

    private void load(String key, EffectDef.Kind kind, Map<String, EffectDef> target) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(key);
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try { target.put(id, EffectDef.parse(id, kind, s)); }
            catch (Exception e) {
                plugin.getLogger().warning(key + " 로드 실패: " + id + " " + e.getMessage());
            }
        }
    }

    public EffectDef get(String id) {
        EffectDef d = blessings.get(id);
        return d != null ? d : curses.get(id);
    }

    public Map<String, EffectDef> blessings() { return blessings; }
    public Map<String, EffectDef> curses() { return curses; }
}
