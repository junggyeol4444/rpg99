package kr.reborn.spawn.command;

import kr.reborn.spawn.RebornSpawn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GymCommand implements CommandExecutor {
    private final RebornSpawn plugin;
    public GymCommand(RebornSpawn p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        plugin.gym().manualFinalize(p.getUniqueId());
        return true;
    }
}
