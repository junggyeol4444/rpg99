package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.DemonGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /stabilize — 마계 거주자의 마기 안정화 의식.
 * 현재 마기 30% 감소 + 정신 +5 (NPC 의뢰 또는 운기 위험 회피용).
 */
public final class StabilizeCommand implements CommandExecutor {
    private final RebornStat plugin;
    public StabilizeCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        if (d.worldKey() != WorldKey.DEMON) {
            Msg.error(p, "마기 안정화는 마계 거주자만 가능.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.DEMON);
        if (!(strategy instanceof DemonGrowth demon)) {
            Msg.error(p, "마계 성장 strategy 없음.");
            return true;
        }
        demon.stabilize(p);
        return true;
    }
}
