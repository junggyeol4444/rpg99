package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TerritoryCommand implements CommandExecutor {
    private final RebornClan plugin;
    public TerritoryCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/territory claim | unclaim | info | war <id>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "claim": plugin.territories().claim(p); break;
            case "unclaim": plugin.territories().unclaim(p); break;
            case "info":
                var t = plugin.territories().at(p.getLocation().getChunk());
                Msg.send(p, t == null ? "&7중립 지대" : "&6소유자: " + t.owner + " 가문: " + t.clanId);
                break;
            case "war":
                var t = plugin.territories().at(p.getLocation().getChunk());
                if (t == null) { Msg.error(p, "여기에 영토가 없다."); break; }
                if (t.owner.equals(p.getUniqueId())) { Msg.error(p, "자기 영토는 침공 불가."); break; }
                plugin.territories().declareWar(p, t);
                break;
        }
        return true;
    }
}
