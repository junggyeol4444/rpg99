package kr.reborn.craft.command;

import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CraftItemCommand implements CommandExecutor {
    private final RebornCraft plugin;
    public CraftItemCommand(RebornCraft plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] args) {
        if (!s.hasPermission("reborncraft.admin")) { Msg.error(s, "권한 없음"); return true; }
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length < 1) { Msg.warn(p, "/craftitem <id>"); return true; }
        CustomItem ci = plugin.items().get(args[0]);
        if (ci == null) { Msg.error(p, "아이템 없음"); return true; }
        p.getInventory().addItem(plugin.items().render(ci));
        Msg.send(p, "&a지급: " + ci.name);
        return true;
    }
}
