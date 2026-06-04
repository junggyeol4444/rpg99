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
 * /moonritual — 요계 거주자 보름달 의식.
 * 보름달이면 요기 ×10 누적 (timeMult), 평일 밤 ×3, 낮 ×0.5.
 */
public final class MoonRitualCommand implements CommandExecutor {
    private final RebornStat plugin;
    public MoonRitualCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        if (d.worldKey() != WorldKey.YOKAI) {
            Msg.error(p, "보름달 의식은 요계 거주자만 가능.");
            return true;
        }
        // 밤(13000~23000) 외에는 의미 없음 — 알림만 표시
        long t = p.getWorld().getTime();
        if (t < 13000 || t > 23000) {
            Msg.warn(p, "&7낮에는 의식 효과가 미미하다 (밤에 시도하라).");
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.YOKAI);
        if (!(strategy instanceof YokaiGrowth yokai)) {
            Msg.error(p, "요계 성장 strategy 없음.");
            return true;
        }
        yokai.onMoonRitual(p);
        return true;
    }
}
