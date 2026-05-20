package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Clan;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ClanCommand implements CommandExecutor {
    private final RebornClan plugin;
    public ClanCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/clan create <id> <name> | join <id> | leave | invite <player> | info | list | treasury");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "create":
                if (a.length < 3) return true;
                plugin.clans().create(p, a[1], a[2]);
                break;
            case "join":
                if (a.length < 2) return true;
                Clan target = plugin.clans().get(a[1]);
                if (target == null) { Msg.error(p, "가문 없음"); break; }
                plugin.clans().join(target, p);
                Msg.send(p, "&a가문 가입: " + target.name);
                break;
            case "leave":
                plugin.clans().leave(p);
                Msg.send(p, "&7가문 탈퇴");
                break;
            case "info":
                Clan mine = plugin.clans().ofPlayer(p.getUniqueId());
                if (mine == null) { Msg.warn(p, "소속 가문 없음"); break; }
                Msg.send(p, "&6가문: " + mine.name + " (Lv " + mine.level + ")");
                Msg.send(p, "&7인원: " + mine.members.size() + "  보고: " + mine.treasury);
                break;
            case "list":
                Msg.send(p, "&6전체 가문:");
                plugin.clans().all().forEach(cn -> p.sendMessage("§e" + cn.id + " §7- " + cn.name + " (Lv " + cn.level + ")"));
                break;
            case "treasury":
                Clan g = plugin.clans().ofPlayer(p.getUniqueId());
                if (g != null) Msg.send(p, "&6금고: " + g.treasury);
                break;
            case "invite":
                if (a.length < 2) return true;
                Player tg = Bukkit.getPlayerExact(a[1]);
                if (tg != null) Msg.send(tg, "&d" + p.getName() + "이(가) 가문에 초대했다.");
                break;
        }
        return true;
    }
}
