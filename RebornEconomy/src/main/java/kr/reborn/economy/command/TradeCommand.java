package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TradeCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public TradeCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length == 0) {
            Msg.warn(p, "/trade <player>  /trade ready  /trade cancel");
            return true;
        }
        if (args[0].equalsIgnoreCase("ready")) { plugin.trades().ready(p); return true; }
        if (args[0].equalsIgnoreCase("cancel")) { plugin.trades().cancel(p); return true; }
        Player to = Bukkit.getPlayer(args[0]);
        if (to == null) { Msg.error(p, "오프라인 플레이어입니다."); return true; }
        plugin.trades().open(p, to);
        return true;
    }
}
