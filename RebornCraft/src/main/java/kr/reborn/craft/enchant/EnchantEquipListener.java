package kr.reborn.craft.enchant;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.craft.RebornCraft;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 강화 아이템 장착 시 스탯 보너스 적용/해제.
 *
 * 손에 든 아이템이 강화 +N → STR/INT/ETC +N×3 (런타임 보너스).
 * 슬롯 전환 시 재계산. 로그아웃 시 정리.
 *
 * 보너스는 source="enchant-equip:<uuid>"로 RebornCore에 기록되어
 * 같은 source의 보너스는 중복되지 않음.
 */
public final class EnchantEquipListener implements Listener {

    private final RebornCraft plugin;
    /** uuid → 마지막 적용된 보너스 양 */
    private final Map<UUID, Integer> lastApplied = new ConcurrentHashMap<>();

    public EnchantEquipListener(RebornCraft plugin) { this.plugin = plugin; }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack newItem = p.getInventory().getItem(e.getNewSlot());
        reapplyBonus(p, newItem);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        reapplyBonus(e.getPlayer(), e.getPlayer().getInventory().getItemInMainHand());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Integer last = lastApplied.remove(e.getPlayer().getUniqueId());
        if (last != null && last > 0) {
            applyDelta(e.getPlayer().getUniqueId(), -last);
        }
    }

    private void reapplyBonus(Player p, ItemStack item) {
        int newLv = plugin.enchant().levelOf(item);
        int newBonus = newLv * 3;
        Integer oldBonus = lastApplied.getOrDefault(p.getUniqueId(), 0);
        int delta = newBonus - oldBonus;
        if (delta == 0) return;
        applyDelta(p.getUniqueId(), delta);
        lastApplied.put(p.getUniqueId(), newBonus);
    }

    private void applyDelta(UUID uuid, int delta) {
        try {
            RebornCore.get().api().addStat(uuid, StatType.STRENGTH, delta, "enchant-equip");
            RebornCore.get().api().addStat(uuid, StatType.AGILITY, delta, "enchant-equip");
            RebornCore.get().api().addStat(uuid, StatType.ENDURANCE, delta, "enchant-equip");
        } catch (Throwable ignored) {}
    }
}
