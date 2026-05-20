package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TierUpCommand implements CommandExecutor {
    private final RebornStat plugin;
    public TierUpCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d.worldKey() == WorldKey.IMMORTAL) {
            plugin.minigames().startTribulation(p, d.tier());
        } else if (d.worldKey() == WorldKey.MARTIAL && "생사경".equals(d.tier())) {
            plugin.minigames().startEnlightenment(p);
        } else {
            // 일반 자동 승급
            var t = RebornCore.get().tierManager().checkAndAdvance(p, d);
            if (t != null) Msg.send(p, "&6경지: " + t.name);
            else Msg.warn(p, "더 이상 돌파할 수 없다.");
        }
        return true;
    }
}
