package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Lang;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * /serverstat — 운영자용 전체 서버 메트릭 한 화면.
 *
 * 표시:
 *   - 접속 / 최대 슬롯
 *   - JVM 메모리 (used/max MB)
 *   - TPS (Paper API)
 *   - 활성 세계 수
 *   - 각 RebornXxx 플러그인 enabled 여부 + 핵심 카운터 (가문/왕국/교단/신 등)
 *
 * /serverstat <키워드> — 플러그인 이름에 키워드 포함된 것만 필터.
 */
public final class ServerStatCommand implements CommandExecutor {

    private final RebornCore plugin;
    public ServerStatCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!s.hasPermission("reborncore.admin") && !s.isOp()) {
            Msg.t(s, "common.no-permission");
            return true;
        }
        s.sendMessage(ChatColor.translateAlternateColorCodes('&',
                Lang.t(s, "server-stat.header")));

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        Msg.t(s, "server-stat.players", online, max);

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        Msg.t(s, "server-stat.memory", usedMb, maxMb);

        try {
            double[] tps = Bukkit.getTPS();
            String t = String.format("%.1f / %.1f / %.1f", tps[0], tps[1], tps[2]);
            Msg.t(s, "server-stat.tps", t);
        } catch (Throwable ignored) {
            Msg.t(s, "server-stat.tps", "N/A");
        }

        Msg.t(s, "server-stat.plugin", "worlds", String.valueOf(Bukkit.getWorlds().size()));

        String filter = a.length > 0 ? a[0].toLowerCase() : null;
        for (var pl : Bukkit.getPluginManager().getPlugins()) {
            String name = pl.getName();
            if (!name.startsWith("Reborn")) continue;
            if (filter != null && !name.toLowerCase().contains(filter)) continue;
            if (!pl.isEnabled()) {
                Msg.t(s, "server-stat.plugin", name, "&c[OFF]");
                continue;
            }
            String detail = collectDetail(pl);
            Msg.t(s, "server-stat.plugin", name, detail);
        }
        return true;
    }

    /** 플러그인별 핵심 카운터 — 알려진 manager-accessor만 reflection 호출. */
    private String collectDetail(org.bukkit.plugin.Plugin pl) {
        String n = pl.getName();
        return switch (n) {
            case "RebornClan" -> {
                int clans = colSize(pl, "clans", "all");
                int kingdoms = colSize(pl, "kingdoms", "all");
                yield "&7가문 &f" + clans + " &7· 왕국 &f" + kingdoms;
            }
            case "RebornGod" -> {
                int religions = colSize(pl, "religions", "all");
                int npcGods = colSize(pl, "gods", "npcAll");
                int playerGods = colSize(pl, "gods", "playerAll");
                yield "&7교단 &f" + religions + " &7· NPC신 &f" + npcGods
                        + " &7· 플레이어신 &f" + playerGods;
            }
            case "RebornStat" -> {
                int cities = 7, ports = 7;
                yield "&7사이버시티 &f" + cities + " &7· 항구 &f" + ports;
            }
            default -> "&a[OK]";
        };
    }

    /** plugin.method1().method2() 결과가 Collection이면 size, 아니면 0. */
    private static int colSize(org.bukkit.plugin.Plugin pl, String managerMethod, String collectionMethod) {
        try {
            Object mgr = pl.getClass().getMethod(managerMethod).invoke(pl);
            if (mgr == null) return 0;
            Object col = mgr.getClass().getMethod(collectionMethod).invoke(mgr);
            if (col instanceof Collection<?> c) return c.size();
        } catch (Throwable ignored) {}
        return 0;
    }
}
