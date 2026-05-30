package kr.reborn.death.command;

import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BountyCommand implements CommandExecutor {
    private final RebornDeath plugin;
    public BountyCommand(RebornDeath p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&6범죄: " + (int) plugin.crime().crime(p.getUniqueId())
                    + "  명성: " + (int) plugin.crime().fame(p.getUniqueId())
                    + "  등급: " + plugin.crime().label(p.getUniqueId()));
            Msg.send(p, "&c내 현상금: " + plugin.bounty().bountyOf(p.getUniqueId()));
            Msg.send(p, "&7/bounty place <player> <amount> [currency]");
            Msg.send(p, "&7/bounty top [N]");
            Msg.send(p, "&7/bounty check <player>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "place" -> {
                if (a.length < 3) { Msg.warn(p, "/bounty place <player> <amount> [currency]"); return true; }
                Player tgt = Bukkit.getPlayerExact(a[1]);
                if (tgt == null) { Msg.error(p, "오프라인."); return true; }
                long amount;
                try { amount = Long.parseLong(a[2]); }
                catch (NumberFormatException e) { Msg.error(p, "금액 숫자."); return true; }
                String currency = a.length >= 4 ? a[3].toUpperCase() : "GOLD_COIN";
                plugin.bounty().place(p, tgt.getUniqueId(), amount, currency);
            }
            case "top" -> {
                int n = a.length >= 2 ? Integer.parseInt(a[1]) : 10;
                Msg.send(p, "&6=== 현상금 순위 ===");
                int i = 1;
                for (var e : plugin.bounty().top(n)) {
                    Player tp = Bukkit.getPlayer(e.getKey());
                    String name = tp != null ? tp.getName() : e.getKey().toString().substring(0, 8);
                    p.sendMessage("§e" + (i++) + ". §f" + name + " §c" + e.getValue());
                }
            }
            case "check" -> {
                if (a.length < 2) { Msg.warn(p, "/bounty check <player>"); return true; }
                Player tgt = Bukkit.getPlayerExact(a[1]);
                if (tgt == null) { Msg.error(p, "오프라인."); return true; }
                Msg.send(p, "&c" + tgt.getName() + " 현상금: " + plugin.bounty().bountyOf(tgt.getUniqueId()));
            }
            default -> Msg.send(p, "&7/bounty place|top|check");
        }
        return true;
    }
}
