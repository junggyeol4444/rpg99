package kr.reborn.craft.manager;

import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import kr.reborn.craft.data.Grade;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemRegistry {

    private final RebornCraft plugin;
    private final Map<String, CustomItem> defs = new HashMap<>();
    private final Map<Grade, String> colors = new HashMap<>();
    private Grade glowFrom = Grade.RARE;
    private Grade particleFrom = Grade.MYTHIC;

    public ItemRegistry(RebornCraft plugin) {
        this.plugin = plugin;
        loadColors();
        load();
    }

    private void loadColors() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("grade-colors");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try { colors.put(Grade.valueOf(k.toUpperCase()), sec.getString(k)); }
            catch (IllegalArgumentException ignored) {}
        }
        try { glowFrom = Grade.valueOf(plugin.getConfig().getString("grade-glow-from", "RARE")); }
        catch (Exception ignored) {}
        try { particleFrom = Grade.valueOf(plugin.getConfig().getString("particle-from", "MYTHIC")); }
        catch (Exception ignored) {}
    }

    private void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                Material mat = Material.matchMaterial(s.getString("base", "STONE"));
                if (mat == null) continue;
                Grade grade = Grade.valueOf(s.getString("grade", "COMMON"));
                CustomItem.Type type = CustomItem.Type.valueOf(s.getString("type", "MISC"));
                CustomItem ci = new CustomItem(id, mat, s.getInt("model", 0),
                        s.getString("name", id), grade, type);
                ci.lore.addAll(s.getStringList("lore"));
                ci.durability = s.getInt("durability", -1);
                ci.tradable = s.getBoolean("tradable", true);
                ci.droppable = s.getBoolean("droppable", true);
                ci.unique = s.getBoolean("unique", false);
                ci.skill = s.getString("skill", null);
                String acc = s.getString("accessory_slot", null);
                if (acc != null) {
                    try { ci.accessorySlot = CustomItem.AccessorySlot.valueOf(acc); }
                    catch (IllegalArgumentException ignored) {}
                }
                ConfigurationSection statSec = s.getConfigurationSection("stats");
                if (statSec != null) {
                    for (String k : statSec.getKeys(false)) {
                        try { ci.stats.put(StatType.valueOf(k.toUpperCase()), statSec.getDouble(k)); }
                        catch (IllegalArgumentException ignored) {}
                    }
                }
                ConfigurationSection cons = s.getConfigurationSection("consume");
                if (cons != null) {
                    try { ci.consumeType = CustomItem.ConsumeType.valueOf(cons.getString("type", "CUSTOM")); }
                    catch (IllegalArgumentException ignored) { ci.consumeType = CustomItem.ConsumeType.CUSTOM; }
                    ci.consumeValue = cons.get("value");
                    ci.consumeCooldownSeconds = cons.getInt("cooldown", 0);
                }
                defs.put(id, ci);
            } catch (Exception e) {
                plugin.getLogger().warning("아이템 로드 실패: " + id + " " + e.getMessage());
            }
        }
    }

    public CustomItem get(String id) { return defs.get(id); }
    public Collection<CustomItem> all() { return defs.values(); }
    public Grade glowFrom() { return glowFrom; }
    public Grade particleFrom() { return particleFrom; }

    public ItemStack render(CustomItem ci) {
        String color = colors.getOrDefault(ci.grade, "&7");
        String displayName = color + ci.name.replaceAll("&[0-9a-flonkr]", "");
        List<String> lore = new ArrayList<>();
        lore.add(Msg.c("&7[" + ci.grade + "]"));
        for (String l : ci.lore) lore.add(Msg.c(l));
        if (!ci.stats.isEmpty()) {
            lore.add("");
            lore.add(Msg.c("&8====== 스탯 ======"));
            for (var e : ci.stats.entrySet()) {
                lore.add(Msg.c("&7" + e.getKey().name() + ": &f+" + e.getValue()));
            }
        }
        if (ci.skill != null) lore.add(Msg.c("&d부여 스킬: " + ci.skill));
        if (ci.durability > 0) lore.add(Msg.c("&7내구도: &f" + ci.durability));
        else if (ci.durability < 0) lore.add(Msg.c("&6무한 내구도"));

        ItemStack s = Items.of(ci.base, displayName);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setLore(lore);
            if (ci.model > 0) m.setCustomModelData(ci.model);
            if (ci.grade.atLeast(glowFrom)) {
                m.addEnchant(Enchantment.UNBREAKING, 1, true);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            s.setItemMeta(m);
        }
        Items.tagged(plugin, s, "rcraft_id", ci.id);
        return s;
    }

    public CustomItem ofItem(ItemStack stack) {
        if (stack == null) return null;
        String id = Items.tag(plugin, stack, "rcraft_id");
        return id == null ? null : defs.get(id);
    }
}
