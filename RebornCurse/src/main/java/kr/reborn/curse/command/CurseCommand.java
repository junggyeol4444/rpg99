package kr.reborn.curse.command;

import kr.reborn.core.util.Msg;
import kr.reborn.curse.RebornCurse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CurseCommand implements CommandExecutor {
    private final RebornCurse plugin;
    public CurseCommand(RebornCurse plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!s.hasPermission("reborncurse.admin")) {
            Msg.error(s, "권한 없음"); return true;
        }
        if (args.length < 3) {
            Msg.warn(s, "/curse apply|cure <player> <id>"); return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { Msg.error(s, "오프라인"); return true; }
        if (args[0].equalsIgnoreCase("apply")) {
            plugin.effects().apply(target, args[2]);
        } else if (args[0].equalsIgnoreCase("cure")) {
            plugin.effects().cure(target, args[2]);
        } else {
            Msg.warn(s, "apply 또는 cure만 가능");
        }
        return true;
    }
}
