package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.HeavenGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /pray [npcId] — 천계 신전 기도.
 * quality는 시간대 + 임의 보너스로 결정 (0.5~1.0).
 */
public final class PrayCommand implements CommandExecutor {
    private final RebornStat plugin;
    /** 플레이어별 마지막 기도 시각 — 스팸 방지 (HEAVEN_KI/DIVINITY 무한 누적 차단). */
    private final java.util.Map<java.util.UUID, Long> lastPray = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long PRAY_COOLDOWN_MS = 60_000L;  // 1분

    public PrayCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        if (d.worldKey() != WorldKey.HEAVEN) {
            Msg.error(p, "기도는 천계 거주자만 가능.");
            return true;
        }
        long now = System.currentTimeMillis();
        Long last = lastPray.get(p.getUniqueId());
        if (last != null && now - last < PRAY_COOLDOWN_MS) {
            Msg.warn(p, "&7기도 쿨다운 " + ((PRAY_COOLDOWN_MS - (now - last)) / 1000) + "초 남음.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.HEAVEN);
        if (!(strategy instanceof HeavenGrowth heaven)) {
            Msg.error(p, "천계 성장 strategy 없음.");
            return true;
        }
        lastPray.put(p.getUniqueId(), now);
        String npcId = a.length > 0 ? a[0] : "self_altar";
        // quality: 낮(0~12000)이면 1.0, 밤이면 0.6
        long t = p.getWorld().getTime();
        double quality = (t < 12000) ? 0.9 + kr.reborn.core.util.Rand.rangeD(0, 0.1)
                                      : 0.5 + kr.reborn.core.util.Rand.rangeD(0, 0.2);
        heaven.onPrayer(p, npcId, quality);
        return true;
    }
}
