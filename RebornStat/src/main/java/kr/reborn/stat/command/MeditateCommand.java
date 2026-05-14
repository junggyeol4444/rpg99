package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.stat.RebornStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MeditateCommand implements CommandExecutor {
    private final RebornStat plugin;
    public MeditateCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        int tierIdx = Math.max(0, d.tier() == null ? 0 : d.tier().length() / 2);
        plugin.minigames().startMeditation(p, tierIdx);
        return true;
    }
}
