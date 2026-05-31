package kr.reborn.death.command;

import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DuelCommand implements CommandExecutor {
    private final RebornDeath plugin;
    public DuelCommand(RebornDeath plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/duel challenge <player>  - 결투 신청");
            Msg.send(p, "&7/duel accept              - 도전 수락");
            Msg.send(p, "&7/duel reject              - 거절");
            Msg.send(p, "&7/duel honor               - 명예 확인");
            Msg.send(p, "&7/duel top                 - 명예 순위");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "challenge", "ch" -> {
                if (a.length < 2) { Msg.warn(p, "/duel challenge <player>"); return true; }
                Player tgt = Bukkit.getPlayerExact(a[1]);
                if (tgt == null) { Msg.error(p, "오프라인."); return true; }
                plugin.duels().challenge(p, tgt);
            }
            case "accept" -> plugin.duels().accept(p);
            case "reject" -> plugin.duels().reject(p);
            case "honor" -> Msg.send(p, "&6내 명예: " + plugin.duels().honorOf(p.getUniqueId()));
            case "top" -> {
                Msg.send(p, "&6=== 명예 순위 ===");
                plugin.duels().honorAll().entrySet().stream()
                        .sorted((x, y) -> Integer.compare(y.getValue(), x.getValue()))
                        .limit(10)
                        .forEach(en -> {
                            Player ply = Bukkit.getPlayer(en.getKey());
                            String name = ply != null ? ply.getName() : "?";
                            p.sendMessage("§7• §e" + name + " §6" + en.getValue());
                        });
            }
            default -> Msg.warn(p, "/duel challenge|accept|reject|honor|top");
        }
        return true;
    }
}
