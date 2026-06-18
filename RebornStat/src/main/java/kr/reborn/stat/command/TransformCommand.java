package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.YokaiGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /transform — 요계 거주자만 사용 가능, 요기 100 소모 + 120초 쿨다운 후 30초 변신.
 */
public final class TransformCommand implements CommandExecutor {
    private final RebornStat plugin;
    public TransformCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        if (d.worldKey() != WorldKey.YOKAI) {
            Msg.error(p, "변신술은 요계 거주자만 사용 가능.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.YOKAI);
        if (!(strategy instanceof YokaiGrowth yokai)) {
            Msg.error(p, "요계 성장 strategy 없음.");
            return true;
        }
        yokai.onTransform(p);
        return true;
    }
}
