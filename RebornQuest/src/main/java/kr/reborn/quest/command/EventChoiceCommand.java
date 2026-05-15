package kr.reborn.quest.command;

import kr.reborn.quest.RebornQuest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EventChoiceCommand implements CommandExecutor {
    private final RebornQuest plugin;
    public EventChoiceCommand(RebornQuest p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length < 1) return true;
        try {
            int choice = Integer.parseInt(a[0]) - 1;
            plugin.events().choose(p, choice);
        } catch (NumberFormatException ignored) {}
        return true;
    }
}
