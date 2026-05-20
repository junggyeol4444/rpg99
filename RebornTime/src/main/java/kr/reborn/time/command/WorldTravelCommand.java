package kr.reborn.time.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WorldTravelCommand implements CommandExecutor {
    private final RebornTime plugin;
    public WorldTravelCommand(RebornTime p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length < 1) {
            Msg.send(s, "&7/worldtravel <WORLD>"); return true;
        }
        try {
            WorldKey w = WorldKey.valueOf(a[0].toUpperCase());
            plugin.travel().travel(p, w);
        } catch (Exception e) { Msg.error(p, "잘못된 세계 키"); }
        return true;
    }
}
