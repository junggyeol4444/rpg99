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
                Msg.warn(p, "영토 전쟁 선포 — 준비 시간 1시간 (TODO: 실시간 PvP 활성 처리)");
                break;
        }
        return true;
    }
}
