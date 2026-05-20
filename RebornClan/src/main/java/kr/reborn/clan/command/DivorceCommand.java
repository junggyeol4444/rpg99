package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DivorceCommand implements CommandExecutor {
    private final RebornClan plugin;
    public DivorceCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (s instanceof Player p) plugin.marriages().divorce(p);
        return true;
    }
}
