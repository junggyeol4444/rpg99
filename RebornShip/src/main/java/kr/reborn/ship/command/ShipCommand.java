package kr.reborn.ship.command;

import kr.reborn.core.util.Msg;
import kr.reborn.ship.RebornShip;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ShipCommand implements CommandExecutor {
    private final RebornShip plugin;
    public ShipCommand(RebornShip p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/ship register <name> | list | dismantle <name> | join <name>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "register":
                if (a.length < 2) return true;
                plugin.ships().register(p, a[1]);
                break;
            case "list":
                Msg.send(p, "&6내 배:");
                plugin.ships().ofOwner(p.getUniqueId()).forEach(sh ->
                        p.sendMessage("§e" + sh.name + " §7등급 " + sh.grade + " HP " + sh.hp + "/" + sh.maxHp));
                break;
            case "dismantle":
                Msg.warn(p, "해체 (TODO: 블록 환수)");
                break;
            case "join":
                Msg.send(p, "&7선원으로 승선");
                break;
        }
        return true;
    }
}
