package kr.reborn.craft.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 장신구 4슬롯 (반지×2, 목걸이, 귀걸이). */
public final class AccessoryManager {

    public enum Slot { RING_1, RING_2, NECKLACE, EARRING }

    private static final String NS = "RebornCraft.accessory";

    private final RebornCraft plugin;
    private final Map<UUID, EnumMap<Slot, ItemStack>> equipped = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public AccessoryManager(RebornCraft plugin) { this.plugin = plugin; }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            var all = RebornCore.get().kv().loadAll(NS, p);
            EnumMap<Slot, ItemStack> map = new EnumMap<>(Slot.class);
            for (Slot s : Slot.values()) {
                String enc = all.get(s.name());
                if (enc != null && !enc.isEmpty()) {
                    ItemStack it = kr.reborn.core.util.ItemSerializer.fromBase64(enc);
                    if (it != null) map.put(s, it);
                }
            }
            if (!map.isEmpty()) equipped.put(p, map);
        }
    }

    public EnumMap<Slot, ItemStack> of(UUID p) {
        ensureLoaded(p);
        return equipped.computeIfAbsent(p, k -> new EnumMap<>(Slot.class));
    }

    public void open(Player p) {
        var b = plugin.gui().builder("&5장신구", 3);
        EnumMap<Slot, ItemStack> map = of(p.getUniqueId());

        Slot[] order = { Slot.RING_1, Slot.RING_2, Slot.NECKLACE, Slot.EARRING };
        int[] slots = { 11, 12, 14, 15 };
        for (int i = 0; i < order.length; i++) {
            Slot s = order[i];
            ItemStack equipped = map.get(s);
            ItemStack icon = equipped != null ? equipped.clone()
                    : Items.of(Material.GRAY_STAINED_GLASS_PANE, "&7" + s + " &8(비어있음)",
                    "&7손에 든 장신구로 클릭하여 장착");
            b.set(slots[i], icon, e -> handle(p, s));
        }
        b.open(p);
    }

    private void handle(Player p, Slot s) {
        EnumMap<Slot, ItemStack> map = of(p.getUniqueId());
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItem ci = plugin.items().ofItem(hand);
        if (ci != null && ci.type == CustomItem.Type.ACCESSORY) {
            // 슬롯 검증 — 목걸이를 반지 슬롯에 끼우거나 반지를 귀걸이 슬롯에 끼우는 등의
            // 슬롯 우회로 동일 효과 4중 적용을 막음
            if (ci.accessorySlot != null && !matchesSlot(ci.accessorySlot, s)) {
                Msg.error(p, "이 슬롯에 맞지 않는 장신구입니다 ("
                        + ci.accessorySlot + " 전용).");
                return;
            }
            // 장착
            ItemStack equip = hand.clone();
            ItemStack prev = map.put(s, equip);
            applyStats(p, ci, +1);
            if (prev != null) {
                p.getInventory().addItem(prev);
                CustomItem old = plugin.items().ofItem(prev);
                if (old != null) applyStats(p, old, -1);
            }
            p.getInventory().setItemInMainHand(null);
            // 영속화
            String enc = kr.reborn.core.util.ItemSerializer.toBase64(equip);
            if (enc != null) RebornCore.get().kv().put(NS, p.getUniqueId(), s.name(), enc);
            Msg.send(p, "&a장신구 장착: " + ci.name);
        } else {
            ItemStack removed = map.remove(s);
            if (removed != null) {
                p.getInventory().addItem(removed);
                CustomItem old = plugin.items().ofItem(removed);
                if (old != null) applyStats(p, old, -1);
                RebornCore.get().kv().remove(NS, p.getUniqueId(), s.name());
                Msg.send(p, "&7장신구 해제");
            }
        }
        p.closeInventory();
    }

    private void applyStats(Player p, CustomItem ci, int sign) {
        for (var e : ci.stats.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), e.getValue() * sign,
                    "ACC:" + ci.id);
        }
    }

    /** ItemSlot이 UI Slot과 호환되는지 (RING은 RING_1·RING_2 모두 허용). */
    private boolean matchesSlot(CustomItem.AccessorySlot itemSlot, Slot uiSlot) {
        return switch (itemSlot) {
            case RING -> uiSlot == Slot.RING_1 || uiSlot == Slot.RING_2;
            case NECKLACE -> uiSlot == Slot.NECKLACE;
            case EARRING -> uiSlot == Slot.EARRING;
        };
    }
}
