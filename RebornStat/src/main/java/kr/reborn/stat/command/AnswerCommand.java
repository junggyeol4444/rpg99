package kr.reborn.stat.command;

import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AnswerCommand implements CommandExecutor {
    private final RebornStat plugin;
    public AnswerCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length < 1) return true;
        try {
            int choice = Integer.parseInt(a[0]);
            plugin.minigames().onAnswer(p, choice);
        } catch (NumberFormatException e) {
            Msg.error(p, "/answer <번호>");
        }
        return true;
    }
}
