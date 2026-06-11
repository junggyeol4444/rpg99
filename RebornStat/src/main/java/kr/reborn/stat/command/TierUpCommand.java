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
    /** 천겁/깨달음 쿨다운 — 완벽 통과 시 IMMORTAL_KI +200 / INNER_KI +500을 무한 누적 가능했음. */
    private final java.util.Map<java.util.UUID, Long> lastBigEvent = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long BIG_EVENT_COOLDOWN_MS = 600_000L;  // 10분 — 천겁/깨달음은 인생 큰 사건

    public TierUpCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d.worldKey() == WorldKey.IMMORTAL) {
            if (!checkBigEventCooldown(p)) return true;
            lastBigEvent.put(p.getUniqueId(), System.currentTimeMillis());
            plugin.minigames().startTribulation(p, d.tier());
        } else if (d.worldKey() == WorldKey.MARTIAL && "생사경".equals(d.tier())) {
            if (!checkBigEventCooldown(p)) return true;
            lastBigEvent.put(p.getUniqueId(), System.currentTimeMillis());
            plugin.minigames().startEnlightenment(p);
        } else {
            // 일반 자동 승급 — 스탯 게이트가 있어 무한 호출은 의미 없음
            var t = RebornCore.get().tierManager().checkAndAdvance(p, d);
            if (t != null) Msg.send(p, "&6경지: " + t.name);
            else Msg.warn(p, "더 이상 돌파할 수 없다.");
        }
        return true;
    }

    private boolean checkBigEventCooldown(Player p) {
        long now = System.currentTimeMillis();
        Long lt = lastBigEvent.get(p.getUniqueId());
        if (lt != null && now - lt < BIG_EVENT_COOLDOWN_MS) {
            long sec = (BIG_EVENT_COOLDOWN_MS - (now - lt)) / 1000;
            Msg.warn(p, "&7천겁/깨달음 쿨다운 " + (sec / 60) + "분 " + (sec % 60) + "초 남음.");
            return false;
        }
        return true;
    }
}
