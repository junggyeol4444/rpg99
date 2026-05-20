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
            Msg.send(p, "&7/kingdom create <id> <name> | ally <id> | war <id> | treaty <id> | marry <id> <npc>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "create":
                if (a.length < 3) return true;
                plugin.kingdoms().create(p, a[1], a[2]);
                break;
            case "ally":
                if (a.length < 2) return true;
                plugin.kingdoms().ally(p, a[1]);
                break;
            case "war":
                if (a.length < 2) return true;
                plugin.kingdoms().declareWar(p, a[1]);
                break;
            case "treaty":
                if (a.length < 2) return true;
                plugin.kingdoms().treaty(p, a[1]);
                break;
            case "marry":
                if (a.length < 3) return true;
                plugin.kingdoms().politicalMarriage(p, a[1], a[2]);
                break;
            case "info":
                var k = plugin.kingdoms().ofPlayer(p.getUniqueId());
                if (k == null) Msg.warn(p, "왕국 소속 없음");
                else Msg.send(p, "&6왕국: " + k.name + " (가문 " + k.clans.size() + "개)");
                break;
        }
        return true;
    }
}
