package kr.reborn.craft.manager;

import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.Recipe;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class RecipeRegistry {

    private final RebornCraft plugin;
    private final Map<String, Recipe> recipes = new HashMap<>();

    public RecipeRegistry(RebornCraft plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("recipes");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            Recipe r = new Recipe(id,
                    s.getString("profession", ""),
                    s.getInt("min_proficiency", 0),
                    s.getString("result", ""),
                    s.getInt("cast_seconds", 3),
                    s.getDouble("success_rate", 1.0),
                    s.getDouble("higher_grade_chance", 0),
                    s.getInt("exp_gain", 10));
            for (Map<?, ?> raw : s.getMapList("materials")) {
                Material m = Material.matchMaterial(String.valueOf(raw.get("material")));
                if (m == null) continue;
                int amount = raw.get("amount") instanceof Number n ? n.intValue() : 1;
                r.materials.add(new Recipe.Mat(m, amount));
            }
            recipes.put(id, r);
        }
    }

    public Recipe get(String id) { return recipes.get(id); }
    public Collection<Recipe> all() { return recipes.values(); }
}
