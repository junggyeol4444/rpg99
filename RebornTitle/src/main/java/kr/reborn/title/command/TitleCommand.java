package kr.reborn.title.command;

import kr.reborn.core.util.Msg;
import kr.reborn.title.RebornTitle;
import kr.reborn.title.data.Title;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TitleCommand implements CommandExecutor {
    private final RebornTitle plugin;
    public TitleCommand(RebornTitle plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.titles().openListGui(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) { Msg.warn(p, "/title set <id>"); return true; }
            plugin.titles().setActive(p, args[1]);
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) { Msg.warn(p, "/title info <id>"); return true; }
            Title t = plugin.titles().get(args[1]);
            if (t == null) { Msg.error(p, "그런 칭호 없음"); return true; }
            Msg.send(p, "&6===== " + t.name + " &6=====");
            Msg.send(p, "&7" + t.description);
            Msg.send(p, "&7유형: " + t.type + " / 요구: " + t.reqType + " " + t.reqValue);
            return true;
        }
        if (args[0].equalsIgnoreCase("grant") && p.isOp()) {
            if (args.length < 2) { Msg.warn(p, "/title grant <id>"); return true; }
            plugin.titles().grant(p, args[1]);
            return true;
        }
        Msg.warn(p, "/title list|set <id>|info <id>");
        return true;
    }
}
