package kr.reborn.craft.listener;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import kr.reborn.craft.event.RebornItemConsumeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ConsumeListener implements Listener {

    private final RebornCraft plugin;
    /** uuid_itemId → 마지막 사용 시각 ms */
    private final Map<String, Long> cooldowns = new HashMap<>();

    public ConsumeListener(RebornCraft plugin) { this.plugin = plugin; }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick()) return;
        ItemStack stack = e.getItem();
        if (stack == null) return;
        CustomItem ci = plugin.items().ofItem(stack);
        if (ci == null || ci.type != CustomItem.Type.CONSUMABLE) return;

        Player p = e.getPlayer();
        String key = p.getUniqueId() + "_" + ci.id;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(key);
        if (last != null && ci.consumeCooldownSeconds > 0
                && now - last < ci.consumeCooldownSeconds * 1000L) {
            Msg.warn(p, "쿨타임이 끝나지 않았습니다.");
            e.setCancelled(true);
            return;
        }
        cooldowns.put(key, now);

        if (ci.consumeType == CustomItem.ConsumeType.HEAL) {
            double v = ci.consumeValue instanceof Number n ? n.doubleValue() : 0;
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + v));
            Msg.send(p, "&a회복: +" + v);
        } else if (ci.consumeType == CustomItem.ConsumeType.STAT_BOOST) {
            // 단순 모든 공통 스탯 +값
            double v = ci.consumeValue instanceof Number n ? n.doubleValue() : 1;
            for (StatType st : StatType.COMMON_8) {
                RebornCore.get().api().addStat(p.getUniqueId(), st, v, "POTION:" + ci.id);
            }
            Msg.send(p, "&b전 스탯 +" + v);
        } else if (ci.consumeType == CustomItem.ConsumeType.LEARN_SKILL) {
            String skill = String.valueOf(ci.consumeValue);
            Msg.send(p, "&d비급 습득: " + skill + " (RebornSkill 호출 예정)");
            // TODO: RebornSkill API hook
        }

        // 1개 소모
        stack.setAmount(stack.getAmount() - 1);
        Bukkit.getPluginManager().callEvent(new RebornItemConsumeEvent(p, ci));
        e.setCancelled(true);
    }
}
