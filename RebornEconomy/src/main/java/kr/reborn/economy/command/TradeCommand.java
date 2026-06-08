package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TradeCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public TradeCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length == 0) {
            Msg.warn(p, "/trade <player>   - 거래 시작");
            Msg.send(p, "&7/trade offer item        — 손에 든 아이템 제안");
            Msg.send(p, "&7/trade offer money <통화> <금액> — 화폐 제안");
            Msg.send(p, "&7/trade ready             — 준비 완료");
            Msg.send(p, "&7/trade cancel            — 취소");
            return true;
        }
        if (args[0].equalsIgnoreCase("ready")) { plugin.trades().ready(p); return true; }
        if (args[0].equalsIgnoreCase("cancel")) { plugin.trades().cancel(p); return true; }
        if (args[0].equalsIgnoreCase("offer")) {
            if (args.length < 2) { Msg.warn(p, "/trade offer item | money <통화> <금액>"); return true; }
            if (args[1].equalsIgnoreCase("item")) {
                plugin.trades().offerItem(p);
            } else if (args[1].equalsIgnoreCase("money")) {
                if (args.length < 4) { Msg.warn(p, "/trade offer money <통화> <금액>"); return true; }
                long amount;
                try { amount = Long.parseLong(args[3]); }
                catch (NumberFormatException ex) { Msg.error(p, "금액은 숫자"); return true; }
                plugin.trades().offerCurrency(p, args[2].toUpperCase(), amount);
            }
            return true;
        }
        Player to = Bukkit.getPlayer(args[0]);
        if (to == null) { Msg.error(p, "오프라인 플레이어입니다."); return true; }
        plugin.trades().open(p, to);
        return true;
    }
}
