package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PayCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public PayCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length < 3) { Msg.warn(p, "/pay <player> <currency> <amount>"); return true; }
        Player to = Bukkit.getPlayer(args[0]);
        if (to == null) { Msg.error(p, "대상이 오프라인입니다."); return true; }
        String currency = args[1].toUpperCase();
        long amount;
        try { amount = Long.parseLong(args[2]); }
        catch (NumberFormatException e) { Msg.error(p, "숫자가 잘못되었습니다."); return true; }
        if (amount <= 0) { Msg.error(p, "양수만 가능합니다."); return true; }
        if (!plugin.currencies().withdraw(p.getUniqueId(), currency, amount)) {
            Msg.error(p, "잔액이 부족합니다.");
            return true;
        }
        plugin.currencies().deposit(to.getUniqueId(), currency, amount);
        Msg.send(p, "&a송금: " + to.getName() + " &f" + amount + " " + currency);
        Msg.send(to, "&a입금: " + p.getName() + " &f" + amount + " " + currency);
        return true;
    }
}
