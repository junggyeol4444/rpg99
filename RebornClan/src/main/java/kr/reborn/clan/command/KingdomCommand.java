package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class KingdomCommand implements CommandExecutor {
    private final RebornClan plugin;
    public KingdomCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/kingdom create <id> <name> | info | law | ally | war"); return true;
        }
        if ("create".equalsIgnoreCase(a[0]) && a.length >= 3) {
            plugin.kingdoms().create(p, a[1], a[2]);
        } else {
            Msg.warn(p, "왕국 명령 (TODO: 외교·법률·정략 결혼 확장)");
        }
        return true;
    }
}
