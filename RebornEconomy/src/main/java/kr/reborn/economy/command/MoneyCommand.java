package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.data.Currency;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class MoneyCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public MoneyCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        Msg.send(p, "&6===== 보유 화폐 =====");
        Map<String, Long> bal = plugin.currencies().all(p.getUniqueId());
        if (bal.isEmpty()) {
            Msg.send(p, "&7보유한 화폐가 없습니다.");
            return true;
        }
        for (var e : bal.entrySet()) {
            Currency cur = plugin.currencies().get(e.getKey());
            String name = cur != null ? cur.displayName : e.getKey();
            p.sendMessage(" " + Msg.c(name + " &f: " + e.getValue()));
        }
        return true;
    }
}
