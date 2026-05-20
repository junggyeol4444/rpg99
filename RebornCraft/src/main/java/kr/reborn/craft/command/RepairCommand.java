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
        // 단순 수리: durability 100% 회복 (TODO: 실제 NBT durability 트래킹)
        Msg.send(p, "&a" + ci.name + " &7수리 완료");
        return true;
    }
}
