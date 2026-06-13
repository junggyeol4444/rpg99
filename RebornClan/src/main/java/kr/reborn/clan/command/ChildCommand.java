package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ChildCommand implements CommandExecutor {

    private static final String NS = "RebornClan.children";

    private final RebornClan plugin;
    public ChildCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length == 0) {
            Msg.send(s, "&7/child request | play | count");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "request" -> handleRequest(p);
            case "play" -> handlePlay(p);
            case "count" -> {
                int n = childCount(p);
                Msg.send(p, "&d보유 자녀 수: §f" + n);
            }
            default -> Msg.warn(p, "/child request | play | count");
        }
        return true;
    }

    private void handleRequest(Player p) {
        if (plugin.marriages().of(p.getUniqueId()) == null) {
            Msg.error(p, "결혼한 상태여야 한다.");
            return;
        }
        // 일일 1회 제한 — 무한 임신 시도 차단
        long now = System.currentTimeMillis();
        long last = RebornCore.get().kv().getLong(NS, p.getUniqueId(), "lastTry", 0);
        if (now - last < 24L * 3600_000L) {
            long h = (24L * 3600_000L - (now - last)) / 3600_000L;
            Msg.warn(p, "다음 시도까지 " + Math.max(1, h) + "시간 남음.");
            return;
        }
        RebornCore.get().kv().putLong(NS, p.getUniqueId(), "lastTry", now);
        double chance = plugin.getConfig().getDouble("child.request-success-chance", 0.30);
        if (Rand.chance(chance)) {
            int n = childCount(p) + 1;
            RebornCore.get().kv().putInt(NS, p.getUniqueId(), "count", n);
            RebornCore.get().kv().putLong(NS, p.getUniqueId(), "child" + n + ".bornAt", now);
            // 부모 스탯 스냅샷 (자녀로 전환 시 5% 보정에 사용)
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d != null) {
                double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
                RebornCore.get().kv().putDouble(NS, p.getUniqueId(),
                        "child" + n + ".parentTotal", total);
            }
            Bukkit.broadcastMessage("§d§l[출생] §f" + p.getName() + "의 자녀가 태어났다! ("
                    + n + "번째)");
            Msg.send(p, "&d자녀 태어남. /child play 로 자녀로 전환 가능.");
        } else {
            Msg.warn(p, "이번에는 임신되지 않았다.");
        }
    }

    private void handlePlay(Player p) {
        int count = childCount(p);
        if (count == 0) {
            Msg.error(p, "자녀가 없다. /child request 로 자녀를 두고 다시 시도하라.");
            return;
        }
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) { Msg.error(p, "데이터 로드 실패."); return; }
        // 현재 캐릭터 은퇴 → 과거생 기록
        try { RebornCore.get().reincarnationMemory().recordPastLife(p, "CHILD_INHERITANCE"); }
        catch (Throwable ignored) {}
        // 자녀 정보 — 가장 최근 자녀(count번째)
        double parentTotal = RebornCore.get().kv().getDouble(NS, p.getUniqueId(),
                "child" + count + ".parentTotal", 0);
        // 자녀 스탯 = 1 초기 + 부모 총합의 5% 가산 (기획서 23장)
        for (StatType t : StatType.COMMON_8) d.setStat(t, 1);
        double perStat = (parentTotal / 8.0) * 0.05;
        if (perStat > 0) {
            for (StatType t : StatType.COMMON_8) d.addStat(t, perStat);
        }
        // 자녀로 전환 흔적 보존 — clan/lineage 유지 (혈통 그대로 이어짐)
        d.reincarnations(d.reincarnations() + 1);
        d.tier("");
        d.titleId("");
        d.gymUsed(false);
        d.childStart(true);  // 자녀 시작 boon
        // 자녀 수 -1 (사용된 자녀)
        RebornCore.get().kv().putInt(NS, p.getUniqueId(), "count", count - 1);
        Bukkit.broadcastMessage("§d§l[자녀 전환] §f" + p.getName()
                + "이(가) 자녀 세대를 이어받았다!");
        Msg.send(p, "&d자녀로 전환 — 부모 총합의 5% 보정 (+"
                + String.format("%.1f", perStat) + " per 스탯).");
        // 자녀로 전환 후 환생의 월드로 이동
        var lobby = Bukkit.getWorld("lobby");
        if (lobby != null) p.teleport(lobby.getSpawnLocation());
    }

    private int childCount(Player p) {
        return RebornCore.get().kv().getInt(NS, p.getUniqueId(), "count", 0);
    }
}
