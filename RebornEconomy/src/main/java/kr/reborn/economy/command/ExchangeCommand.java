package kr.reborn.economy.command;

import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.data.Currency;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ExchangeCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public ExchangeCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        // /exchange : GUI 표시
        if (args.length == 0) {
            openGui(p);
            return true;
        }
        // /exchange <from> <to> <amount>
        if (args.length < 3) {
            Msg.warn(p, "/exchange <from> <to> <amount> 또는 /exchange (GUI)");
            return true;
        }
        String from = args[0].toUpperCase();
        String to = args[1].toUpperCase();
        long amount;
        try { amount = Long.parseLong(args[2]); }
        catch (NumberFormatException e) { Msg.error(p, "숫자 오류"); return true; }
        boolean exempt = plugin.exchange().isFeeExempt(p.getUniqueId());
        long received = plugin.exchange().exchange(p.getUniqueId(), from, to, amount, exempt);
        if (received <= 0) Msg.error(p, "환전 실패. 환율/잔액 확인.");
        return true;
    }

    private void openGui(Player p) {
        var b = plugin.gui().builder("&6환전소", 6);
        int slot = 0;
        for (Currency cur : plugin.currencies().all()) {
            if (slot >= 54) break;
            long bal = plugin.currencies().balance(p.getUniqueId(), cur.id);
            var icon = Items.of(cur.icon, cur.displayName,
                    "&7잔액: &f" + bal,
                    "&7세계: " + cur.world);
            b.set(slot++, icon, e -> {
                Msg.send(p, "&7명령: &f/exchange " + cur.id + " <대상> <양>");
                p.closeInventory();
            });
        }
        b.set(53, Items.of(Material.BARRIER, "&c닫기"), e -> p.closeInventory());
        b.open(p);
    }
}
