package kr.reborn.title.command;

import kr.reborn.core.util.Msg;
import kr.reborn.title.RebornTitle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RankingCommand implements CommandExecutor {
    private final RebornTitle plugin;
    public RankingCommand(RebornTitle plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("refresh") && p.isOp()) {
            plugin.rankings().refresh();
            Msg.send(p, "&a강제 새로고침 완료");
            return true;
        }
        plugin.rankings().open(p);
        return true;
    }
}
