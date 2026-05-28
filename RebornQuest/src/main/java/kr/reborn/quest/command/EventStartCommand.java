package kr.reborn.quest.command;

import kr.reborn.core.util.Msg;
import kr.reborn.quest.RebornQuest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EventStartCommand implements CommandExecutor {
    private final RebornQuest plugin;
    public EventStartCommand(RebornQuest p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length < 1) {
            Msg.send(s, "&7/event <tree-id>");
            Msg.send(s, "&7  사용 가능: human_alchemist_trade · demon_border · marwang_invasion_branches");
            Msg.send(s, "&7              spirit_king_rage_branches · megacorp_war · hundred_demon_night");
            return true;
        }
        if ("hundred_demon_night".equalsIgnoreCase(a[0])) {
            plugin.events().triggerHundredDemonNight(kr.reborn.core.data.WorldKey.YOKAI);
        } else {
            plugin.events().start(p, a[0]);
        }
        return true;
    }
}
