package kr.reborn.time.command;

import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.ZoneId;

public final class TimeCommand implements CommandExecutor {
    private final RebornTime plugin;
    public TimeCommand(RebornTime p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var t = LocalTime.now(ZoneId.of(plugin.getConfig().getString("timezone", "Asia/Seoul")));
        Msg.send(p, "&6현실: " + t + "  &6마크: " + p.getWorld().getTime());
        return true;
    }
}
