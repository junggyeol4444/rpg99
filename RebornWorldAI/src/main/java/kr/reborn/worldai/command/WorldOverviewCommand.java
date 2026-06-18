package kr.reborn.worldai.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.ai.WorldAI;
import kr.reborn.worldai.faction.FactionDynamics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * /world — 모든 세계 한눈에 보기 (시대·긴장·시세·세력 우위).
 */
public final class WorldOverviewCommand implements CommandExecutor {

    private final RebornWorldAI plugin;

    public WorldOverviewCommand(RebornWorldAI plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        Msg.send(s, "&6&l╔═══════════════════════════════════╗");
        Msg.send(s, "&6&l║ §f환생 13세계 — 현재 상태 (요약)&6&l ║");
        Msg.send(s, "&6&l╚═══════════════════════════════════╝");
        for (WorldKey w : WorldKey.values()) {
            WorldAI ai = plugin.of(w);
            if (ai == null) continue;
            var st = ai.state();
            var epoch = plugin.epoch().of(w);
            String tensionColor = st.tension > 80 ? "§4" : st.tension > 50 ? "§e" : "§a";
            String stabilityColor = st.stability > 70 ? "§a" : st.stability > 30 ? "§e" : "§c";
            String epochColor = switch (epoch.name()) {
                case "DARK_AGE" -> "§4§l";
                case "WAR_ERA" -> "§c";
                case "TENSION_ERA" -> "§e";
                case "PEACE_ERA" -> "§a";
                case "GOLDEN_AGE" -> "§6§l";
                default -> "§7";
            };
            s.sendMessage("§7" + w.name()
                    + " " + epochColor + plugin.epoch().label(epoch)
                    + " §7| 긴장 " + tensionColor + (int) st.tension
                    + " §7안정 " + stabilityColor + (int) st.stability
                    + " §7인플레 §6" + (int) st.inflation
                    + " §7몹 §c×" + String.format("%.1f", st.mobBalance));
            // 상위 세력 1개 (영향력 기준)
            FactionDynamics.Faction topFaction = null;
            for (FactionDynamics.Faction f : plugin.factions().ofWorld(w)) {
                if (topFaction == null || f.influence > topFaction.influence) topFaction = f;
            }
            if (topFaction != null) {
                s.sendMessage("  §7↳ 패권: §f" + topFaction.name
                        + " §7(영향 " + (int) topFaction.influence + ")");
            }
        }
        // 이주 통계 — 활발한 세계
        long totalMigration = 0;
        WorldKey topOriginating = null;
        WorldKey topReceiving = null;
        int maxOut = 0, maxIn = 0;
        for (WorldKey w : WorldKey.values()) {
            int out = plugin.migration().outflow(w).values().stream().mapToInt(Integer::intValue).sum();
            int in_ = plugin.migration().inflow(w).values().stream().mapToInt(Integer::intValue).sum();
            totalMigration += out;
            if (out > maxOut) { maxOut = out; topOriginating = w; }
            if (in_ > maxIn) { maxIn = in_; topReceiving = w; }
        }
        if (totalMigration > 0) {
            Msg.send(s, "&7──────────");
            s.sendMessage("§7총 이주 누적: §f" + totalMigration);
            if (topOriginating != null)
                s.sendMessage("§c가장 많이 떠나가는 세계: §f" + topOriginating + " §7(" + maxOut + ")");
            if (topReceiving != null)
                s.sendMessage("§a가장 많이 받아들이는 세계: §f" + topReceiving + " §7(" + maxIn + ")");
        }
        // 활성 재해
        var disasters = plugin.disasters().activeAll();
        if (!disasters.isEmpty()) {
            Msg.send(s, "&7──────────");
            s.sendMessage("§4§l재해 진행: " + disasters.size() + "건");
            for (var d : disasters.values()) {
                s.sendMessage("  §c• " + d.world + " §7- " + plugin.disasters().labelOf(d.type));
            }
        }
        return true;
    }
}
