package kr.reborn.time.command;

import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ChamberCommand implements CommandExecutor {
    private final RebornTime plugin;
    public ChamberCommand(RebornTime p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) { Msg.send(p, "&7/chamber enter <id> | exit"); return true; }
        switch (a[0].toLowerCase()) {
            case "enter":
                if (a.length < 2) return true;
                plugin.chamber().enter(p, a[1]);
                break;
            case "exit":
                plugin.chamber().exit(p);
                break;
        }
        return true;
    }
}
