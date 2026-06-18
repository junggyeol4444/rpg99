package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.SpiritGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /petition <FIRE|WATER|EARTH|WIND|LIGHT|DARK> — 정령왕에 청원.
 * 호의 +5 (의식, 미니게임 없음). 매 시도 약간의 시간 소요(쿨다운 가능).
 */
public final class PetitionCommand implements CommandExecutor {
    private final RebornStat plugin;
    /** 원소별 마지막 청원 시각 — 청원 스팸 시 정령왕 호의 무한 누적 가능했음. */
    private final java.util.Map<String, Long> last = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 120_000L;  // 2분 — 원소별 독립

    public PetitionCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length < 1) {
            Msg.warn(p, "/petition <FIRE|WATER|EARTH|WIND|LIGHT|DARK>");
            return true;
        }
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return true;
        if (d.worldKey() != WorldKey.SPIRIT) {
            Msg.error(p, "청원은 정령계 거주자만 가능.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.SPIRIT);
        if (!(strategy instanceof SpiritGrowth spirit)) {
            Msg.error(p, "정령계 성장 strategy 없음.");
            return true;
        }
        SpiritGrowth.Element element;
        try { element = SpiritGrowth.Element.valueOf(a[0].toUpperCase()); }
        catch (Exception e) {
            Msg.error(p, "유효한 원소: FIRE, WATER, EARTH, WIND, LIGHT, DARK");
            return true;
        }
        String key = p.getUniqueId() + ":" + element.name();
        long now = System.currentTimeMillis();
        Long lt = last.get(key);
        if (lt != null && now - lt < COOLDOWN_MS) {
            Msg.warn(p, "&7" + element + " 청원 쿨다운 " + ((COOLDOWN_MS - (now - lt)) / 1000) + "초 남음.");
            return true;
        }
        last.put(key, now);
        spirit.petitionKing(p, element, 5.0);
        return true;
    }
}
