package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class AuctionCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public AuctionCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length == 0) {
            plugin.auctions().open(p);
            return true;
        }
        // /auction sell <currency> <startPrice> [buyout] [hours]
        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 3) {
                Msg.warn(p, "/auction sell <currency> <startPrice> [buyout] [hours]");
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                Msg.error(p, "손에 든 아이템이 없습니다.");
                return true;
            }
            String currency = args[1].toUpperCase();
            long start, buyout = 0, hours;
            try {
                start = Long.parseLong(args[2]);
                if (args.length >= 4) buyout = Long.parseLong(args[3]);
                hours = args.length >= 5 ? Long.parseLong(args[4])
                        : plugin.getConfig().getLong("auction.default-duration-hours", 24L);
            } catch (NumberFormatException e) {
                Msg.error(p, "숫자 형식 오류"); return true;
            }
            if (plugin.auctions().register(p, hand, currency, start, buyout, hours)) {
                p.getInventory().setItemInMainHand(null);
            }
            return true;
        }
        Msg.warn(p, "/auction · /auction sell <화폐> <시작가> [즉구가] [시간]");
        return true;
    }
}
