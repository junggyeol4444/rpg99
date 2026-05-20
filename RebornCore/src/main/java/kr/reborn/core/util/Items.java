package kr.reborn.core.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public final class Items {
    private Items() {}

    public static ItemStack of(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(Msg.c(name));
            m.setLore(Arrays.asList(loreColor(lore)));
            it.setItemMeta(m);
        }
        return it;
    }

    private static String[] loreColor(String[] lore) {
        String[] out = new String[lore.length];
        for (int i = 0; i < lore.length; i++) out[i] = Msg.c(lore[i]);
        return out;
    }

    public static ItemStack tagged(Plugin plugin, ItemStack base, String key, String value) {
        ItemMeta m = base.getItemMeta();
        if (m == null) return base;
        m.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        base.setItemMeta(m);
        return base;
    }

    public static String tag(Plugin plugin, ItemStack stack, String key) {
        ItemMeta m = stack.getItemMeta();
        if (m == null) return null;
        return m.getPersistentDataContainer().get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }

    public static int customModelData(ItemStack s) {
        ItemMeta m = s.getItemMeta();
        return m != null && m.hasCustomModelData() ? m.getCustomModelData() : 0;
    }

    public static void setCustomModelData(ItemStack s, int id) {
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setCustomModelData(id);
            s.setItemMeta(m);
        }
    }

    public static void setLore(ItemStack s, List<String> lore) {
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setLore(lore.stream().map(Msg::c).toList());
            s.setItemMeta(m);
        }
    }
}
