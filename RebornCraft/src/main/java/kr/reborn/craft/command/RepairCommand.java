package kr.reborn.craft.command;

import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class RepairCommand implements CommandExecutor {
    private final RebornCraft plugin;
    public RepairCommand(RebornCraft plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItem ci = plugin.items().ofItem(hand);
        if (ci == null) {
            Msg.error(p, "수리 가능한 커스텀 아이템이 없습니다.");
            return true;
        }
        if (ci.durability < 0) {
            Msg.warn(p, "이 아이템은 무한 내구도입니다.");
            return true;
        }
        // 바닐라 durability damage 회복 (Damageable meta 사용)
        var meta = hand.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Damageable dm) {
            int prev = dm.getDamage();
            if (prev <= 0) {
                Msg.warn(p, "이미 만료 내구도입니다.");
                return true;
            }
            dm.setDamage(0);
            hand.setItemMeta(meta);
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            Msg.send(p, "&a" + ci.name + " &7수리 완료 (이전 손상 " + prev + ")");
        } else {
            Msg.warn(p, "&7" + ci.name + "은(는) 손상되지 않는 아이템입니다.");
        }
        return true;
    }
}
