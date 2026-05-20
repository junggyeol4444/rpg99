package kr.reborn.pet.command;

import kr.reborn.pet.RebornPet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MountCommand implements CommandExecutor {
    private final RebornPet plugin;
    public MountCommand(RebornPet p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length < 1) return true;
        plugin.mounts().summon(p, a[0]);
        return true;
    }
}
