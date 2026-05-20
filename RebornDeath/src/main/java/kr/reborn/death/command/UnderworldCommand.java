package kr.reborn.death.command;

import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UnderworldCommand implements CommandExecutor {
    private final RebornDeath plugin;
    public UnderworldCommand(RebornDeath p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/underworld revive | reincarnate | stay");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "revive": plugin.underworld().revive(p); break;
            case "reincarnate": plugin.underworld().reincarnate(p); break;
            case "stay": plugin.underworld().stay(p); break;
        }
        return true;
    }
}
