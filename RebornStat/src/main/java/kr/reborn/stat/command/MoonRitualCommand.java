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
    /** 보름달 의식 쿨다운 — 풀문+밤 단 1회로 +5000 요기 (=5 꼬리) 가능했음. 10분 쿨다운. */
    private final java.util.Map<java.util.UUID, Long> last = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 600_000L;

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
        long now = System.currentTimeMillis();
        Long lt = last.get(p.getUniqueId());
        if (lt != null && now - lt < COOLDOWN_MS) {
            Msg.warn(p, "&7보름달 의식 쿨다운 " + ((COOLDOWN_MS - (now - lt)) / 1000) + "초 남음.");
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
        last.put(p.getUniqueId(), now);
        yokai.onMoonRitual(p);
        return true;
    }
}
