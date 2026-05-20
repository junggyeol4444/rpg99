package kr.reborn.death.command;

import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BountyCommand implements CommandExecutor {
    private final RebornDeath plugin;
    public BountyCommand(RebornDeath p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        Msg.send(p, "&6범죄: " + plugin.crime().crime(p.getUniqueId())
                + "  명성: " + plugin.crime().fame(p.getUniqueId())
                + "  등급: " + plugin.crime().label(p.getUniqueId()));
        return true;
    }
}
