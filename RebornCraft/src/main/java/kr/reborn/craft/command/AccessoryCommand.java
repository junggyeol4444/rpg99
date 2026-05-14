package kr.reborn.craft.command;

import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AccessoryCommand implements CommandExecutor {
    private final RebornCraft plugin;
    public AccessoryCommand(RebornCraft plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        plugin.accessories().open(p);
        return true;
    }
}
