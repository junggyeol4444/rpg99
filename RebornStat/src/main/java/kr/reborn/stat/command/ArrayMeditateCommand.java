package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.MartialGrowth;
import kr.reborn.stat.growth.impl.ImmortalGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /arraymeditate — 무협계 진법 합동 수련 / 선계 도반 합동 양생.
 * 30블록 내 같은 세계 거주자 N명 카운트 후 boost된 효과.
 */
public final class ArrayMeditateCommand implements CommandExecutor {
    private final RebornStat plugin;
    /** 진법·도반 합동 쿨다운 — 5명 동료 시 +100% 즉시 보너스. 스팸 시 무한 누적. */
    private final java.util.Map<java.util.UUID, Long> last = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 180_000L;  // 3분

    public ArrayMeditateCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        long now = System.currentTimeMillis();
        Long lt = last.get(p.getUniqueId());
        if (lt != null && now - lt < COOLDOWN_MS) {
            Msg.warn(p, "&7합동 수련 쿨다운 " + ((COOLDOWN_MS - (now - lt)) / 1000) + "초 남음.");
            return true;
        }
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        WorldKey w = d.worldKey();
        // 같은 세계 30블록 내 동료 수 계산
        int companions = 0;
        for (Player other : p.getWorld().getNearbyEntities(p.getLocation(), 30, 30, 30)
                .stream().filter(e -> e instanceof Player).map(e -> (Player) e).toList()) {
            if (other.getUniqueId().equals(p.getUniqueId())) continue;
            var od = RebornCore.get().api().getPlayerData(other.getUniqueId());
            if (od == null || od.worldKey() != w) continue;
            companions++;
        }
        if (companions == 0) {
            Msg.warn(p, "근처(30블록)에 같은 세계 동료가 없다.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(w);
        if (w == WorldKey.MARTIAL && strategy instanceof MartialGrowth m) {
            last.put(p.getUniqueId(), now);
            m.onArrayMeditate(p, companions);
        } else if (w == WorldKey.IMMORTAL && strategy instanceof ImmortalGrowth im) {
            last.put(p.getUniqueId(), now);
            im.onDaoCompanionMeditate(p, companions);
        } else {
            Msg.error(p, "진법·도반 합동은 무협계·선계 거주자만 가능.");
            return true;
        }
        return true;
    }
}
