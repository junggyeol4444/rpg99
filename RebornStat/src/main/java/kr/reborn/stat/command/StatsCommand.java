package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        Msg.send(p, "&6===== 스탯 =====");
        for (StatType t : StatType.COMMON_8) {
            p.sendMessage(" &e" + t.name() + ": &f" + d.getStat(t));
        }
        Msg.send(p, "&7-----");
        Msg.send(p, "&6세계: &f" + d.worldKey() + "  &6경지: &f" + d.tier());
        Msg.send(p, "&6총합: &f" + RebornCore.get().api().getTotalStats(p.getUniqueId()));
        return true;
    }
}
