package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MarryCommand implements CommandExecutor {
    private final RebornClan plugin;
    public MarryCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length == 0) return true;
        if ("accept".equalsIgnoreCase(a[0])) {
            plugin.marriages().accept(p);
            return true;
        }
        if ("npc".equalsIgnoreCase(a[0]) && a.length > 1) {
            plugin.marriages().marryNpc(p, a[1]);
            return true;
        }
        Player tg = Bukkit.getPlayerExact(a[0]);
        if (tg != null) plugin.marriages().propose(p, tg);
        return true;
    }
}
