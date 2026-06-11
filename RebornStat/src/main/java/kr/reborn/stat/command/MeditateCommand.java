package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.stat.RebornStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MeditateCommand implements CommandExecutor {
    private final RebornStat plugin;
    /** 운기조식 쿨다운 — 시퀀스 완료 즉시 재시작 스팸으로 내공 무한 가능했음. */
    private final java.util.Map<java.util.UUID, Long> last = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 30_000L;

    public MeditateCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        long now = System.currentTimeMillis();
        Long lt = last.get(p.getUniqueId());
        if (lt != null && now - lt < COOLDOWN_MS) {
            kr.reborn.core.util.Msg.warn(p, "&7운기조식 쿨다운 " + ((COOLDOWN_MS - (now - lt)) / 1000) + "초 남음.");
            return true;
        }
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        int tierIdx = Math.max(0, d.tier() == null ? 0 : d.tier().length() / 2);
        last.put(p.getUniqueId(), now);
        plugin.minigames().startMeditation(p, tierIdx);
        return true;
    }
}
