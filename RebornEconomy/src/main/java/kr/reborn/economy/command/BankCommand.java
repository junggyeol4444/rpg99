package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.bank.BankAccount;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BankCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public BankCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/bank open <currency>          - 계좌 개설");
            Msg.send(p, "&7/bank deposit <cur> <amount>   - 예금");
            Msg.send(p, "&7/bank withdraw <cur> <amount>  - 인출");
            Msg.send(p, "&7/bank maturity <cur> <7|30|90> - 정기예금 설정");
            Msg.send(p, "&7/bank loan <cur> <amount>      - 대출");
            Msg.send(p, "&7/bank repay <cur> <amount>     - 상환");
            Msg.send(p, "&7/bank status                   - 내 계좌");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "open" -> {
                if (a.length < 2) { Msg.warn(p, "/bank open <currency>"); return true; }
                plugin.bank().open(p, a[1].toUpperCase());
            }
            case "deposit" -> {
                if (a.length < 3) { Msg.warn(p, "/bank deposit <cur> <amount>"); return true; }
                plugin.bank().deposit(p, a[1].toUpperCase(), Long.parseLong(a[2]));
            }
            case "withdraw" -> {
                if (a.length < 3) { Msg.warn(p, "/bank withdraw <cur> <amount>"); return true; }
                plugin.bank().withdraw(p, a[1].toUpperCase(), Long.parseLong(a[2]));
            }
            case "maturity" -> {
                if (a.length < 3) { Msg.warn(p, "/bank maturity <cur> <7|30|90>"); return true; }
                plugin.bank().setMaturity(p, a[1].toUpperCase(), Integer.parseInt(a[2]));
            }
            case "loan" -> {
                if (a.length < 3) { Msg.warn(p, "/bank loan <cur> <amount>"); return true; }
                plugin.bank().takeLoan(p, a[1].toUpperCase(), Long.parseLong(a[2]));
            }
            case "repay" -> {
                if (a.length < 3) { Msg.warn(p, "/bank repay <cur> <amount>"); return true; }
                plugin.bank().repay(p, a[1].toUpperCase(), Long.parseLong(a[2]));
            }
            case "status" -> {
                var map = plugin.bank().accountsOf(p.getUniqueId());
                if (map.isEmpty()) { Msg.send(p, "&7계좌 없음."); return true; }
                Msg.send(p, "&6=== 내 은행 계좌 ===");
                for (BankAccount ba : map.values()) {
                    p.sendMessage("§7• §f" + ba.currency
                            + " §a예금 " + ba.deposit
                            + " §c대출 " + ba.loan
                            + " §e신용 " + ba.credit
                            + (ba.maturityAt > 0 ? " §6만기 " + ((ba.maturityAt - System.currentTimeMillis()) / 86_400_000L) + "일 남음" : ""));
                }
            }
            default -> Msg.warn(p, "알 수 없는 하위 명령.");
        }
        return true;
    }
}
