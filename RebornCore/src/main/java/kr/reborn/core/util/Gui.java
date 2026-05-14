package kr.reborn.core.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 매우 가벼운 GUI 헬퍼. 슬롯별 콜백.
 */
public final class Gui implements Listener {
    private final Plugin plugin;
    private final Map<UUID, Map<Integer, Consumer<InventoryClickEvent>>> handlers = new HashMap<>();
    private final Map<UUID, Inventory> active = new HashMap<>();

    public Gui(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Builder builder(String title, int rows) {
        return new Builder(title, rows);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        UUID id = e.getWhoClicked().getUniqueId();
        Inventory inv = active.get(id);
        if (inv == null || inv != e.getInventory()) return;
        e.setCancelled(true);
        Map<Integer, Consumer<InventoryClickEvent>> h = handlers.get(id);
        if (h == null) return;
        Consumer<InventoryClickEvent> c = h.get(e.getRawSlot());
        if (c != null) c.accept(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        active.remove(id);
        handlers.remove(id);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
    }

    public final class Builder {
        private final String title;
        private final int rows;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, Consumer<InventoryClickEvent>> clicks = new HashMap<>();

        public Builder(String title, int rows) {
            this.title = title; this.rows = rows;
        }

        public Builder set(int slot, ItemStack item, Consumer<InventoryClickEvent> click) {
            items.put(slot, item);
            if (click != null) clicks.put(slot, click);
            return this;
        }

        public void open(Player p) {
            Inventory inv = Bukkit.createInventory(p, rows * 9, Msg.c(title));
            for (var e : items.entrySet()) inv.setItem(e.getKey(), e.getValue());
            active.put(p.getUniqueId(), inv);
            handlers.put(p.getUniqueId(), clicks);
            p.openInventory(inv);
        }
    }
}
