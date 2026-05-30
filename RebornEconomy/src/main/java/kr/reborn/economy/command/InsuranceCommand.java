package kr.reborn.economy.command;

import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.insurance.InsuranceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class InsuranceCommand implements CommandExecutor {
    private final RebornEconomy plugin;
    public InsuranceCommand(RebornEconomy plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/insurance subscribe <BASIC|PREMIUM|VIP> - 가입");
            Msg.send(p, "&7/insurance cancel                       - 해지");
            Msg.send(p, "&7/insurance status                       - 내 보험");
            Msg.send(p, "&7/insurance list                         - 등급 정보");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "subscribe" -> {
                if (a.length < 2) { Msg.warn(p, "/insurance subscribe <BASIC|PREMIUM|VIP>"); return true; }
                try {
                    InsuranceManager.Grade g = InsuranceManager.Grade.valueOf(a[1].toUpperCase());
                    plugin.insurance().subscribe(p, g);
                } catch (Exception e) { Msg.error(p, "잘못된 등급."); }
            }
            case "cancel" -> plugin.insurance().cancel(p);
            case "status" -> {
                InsuranceManager.Policy pol = plugin.insurance().of(p.getUniqueId());
                if (pol == null) { Msg.send(p, "&7가입 안 됨."); return true; }
                Msg.send(p, "&6보험 정보:");
                p.sendMessage("§7등급: §e" + pol.grade);
                p.sendMessage("§7월 보험료: §c" + pol.grade.monthlyPremium + " GOLD");
                p.sendMessage("§7보장: §a" + pol.grade.coverage + " GOLD");
                p.sendMessage("§7누적 납부: §f" + pol.monthsPaid + "개월");
                p.sendMessage("§7지급 횟수: §f" + pol.payouts);
                p.sendMessage("§7상태: " + (pol.active() ? "§a활성" : "§c미납"));
            }
            case "list" -> {
                Msg.send(p, "&6=== 보험 등급 ===");
                for (var g : InsuranceManager.Grade.values()) {
                    p.sendMessage("§e" + g.name() + " §7월 §c" + g.monthlyPremium
                            + "g §7→ 보장 §a" + g.coverage + "g");
                }
            }
            default -> Msg.warn(p, "/insurance subscribe|cancel|status|list");
        }
        return true;
    }
}
