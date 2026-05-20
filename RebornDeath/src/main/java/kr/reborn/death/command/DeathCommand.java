package kr.reborn.death.command;

import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DeathCommand implements CommandExecutor {
    private final RebornDeath plugin;
    public DeathCommand(RebornDeath p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var loc = plugin.underworld().deathPointOf(p.getUniqueId());
        if (loc == null) Msg.send(p, "&7최근 사망 지점 없음.");
        else Msg.send(p, "&7최근 사망: " + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        return true;
    }
}
