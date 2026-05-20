package kr.reborn.god.command;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GodCommand implements CommandExecutor {
    private final RebornGod plugin;
    public GodCommand(RebornGod p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/god ascend | challenge | domain create | enter | religion create <id> <name> | pray <religion>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "ascend":
                plugin.gods().ascend(p);
                break;
            case "challenge":
                Msg.send(p, "&7신 도전 — 시련의 장 (TODO: 3시련 미니게임)");
                break;
            case "domain":
                if (a.length >= 2 && "create".equalsIgnoreCase(a[1])) plugin.domains().create(p);
                else plugin.domains().enter(p);
                break;
            case "religion":
                if (a.length >= 4 && "create".equalsIgnoreCase(a[1])) plugin.religions().create(p, a[2], a[3]);
                break;
            case "pray":
                if (a.length >= 2) plugin.religions().pray(p, a[1]);
                break;
            case "info":
                God g = plugin.gods().of(p.getUniqueId());
                if (g == null) { Msg.warn(p, "필멸자"); return true; }
                Msg.send(p, "&6신성: " + g.divinity + "  신도: " + g.followers.size());
                break;
        }
        return true;
    }
}
