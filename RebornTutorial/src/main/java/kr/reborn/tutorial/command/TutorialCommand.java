package kr.reborn.tutorial.command;

import kr.reborn.core.util.Msg;
import kr.reborn.tutorial.RebornTutorial;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TutorialCommand implements CommandExecutor {
    private final RebornTutorial plugin;
    public TutorialCommand(RebornTutorial p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            var sess = plugin.manager().sessionOf(p.getUniqueId());
            Msg.send(p, sess == null ? "튜토리얼 미진행" : "단계 " + sess.stage);
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "exit": plugin.manager().exitToMain(p.getUniqueId()); break;
            case "start": plugin.manager().start(p.getUniqueId()); break;
            case "train": plugin.manager().train(p); break;
            case "choose":
                if (a.length < 2) { Msg.send(p, "&7/rtut choose errand|underworld"); break; }
                plugin.manager().resolveChoice(p.getUniqueId(), a[1]);
                break;
        }
        return true;
    }
}
