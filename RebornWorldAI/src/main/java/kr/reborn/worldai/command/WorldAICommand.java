package kr.reborn.worldai.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.ai.WorldAI;
import kr.reborn.worldai.faction.FactionDynamics;
import kr.reborn.worldai.market.MarketSimulator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class WorldAICommand implements CommandExecutor {
    private final RebornWorldAI plugin;
    public WorldAICommand(RebornWorldAI p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.send(s, "&7/worldai status <WORLD>          - 핵심 지표");
            Msg.send(s, "&7/worldai factions <WORLD>        - 세력 표");
            Msg.send(s, "&7/worldai market <WORLD>          - 시장 가격표");
            Msg.send(s, "&7/worldai migration               - 이주 통계");
            Msg.send(s, "&7/worldai history <WORLD> [N]     - 역사 기록");
            Msg.send(s, "&7/worldai epoch [WORLD]           - 시대 표시");
            Msg.send(s, "&7/worldai disaster <WORLD> <type> - 강제 재해");
            Msg.send(s, "&7/worldai weather <WORLD> <name>  - 강제 날씨");
            Msg.send(s, "&7/worldai log [N]                 - AI 통신 로그");
            Msg.send(s, "&7/worldai force <WORLD> <event>   - 트리거 강제");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "status" -> {
                if (a.length < 2) return true;
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                WorldAI ai = plugin.of(w);
                if (ai == null) { Msg.error(s, "AI 미활성"); return true; }
                var st = ai.state();
                Msg.send(s, "&6[" + w + "] 인플레: " + (int) st.inflation
                        + " 거래: " + String.format("%.2f", st.tradeActivity));
                Msg.send(s, "&6긴장도: " + (int) st.tension + " 안정: " + (int) st.stability
                        + " 몬스터: " + String.format("%.2f", st.mobBalance));
                Msg.send(s, "&6시대: §e" + plugin.epoch().label(plugin.epoch().of(w)));
            }
            case "factions" -> {
                if (a.length < 2) return true;
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                Msg.send(s, "&6=== " + w + " 세력 표 ===");
                for (FactionDynamics.Faction f : plugin.factions().ofWorld(w)) {
                    s.sendMessage("§7• §f" + f.name + " §8(" + f.id + ") "
                            + "§e영향 " + (int) f.influence
                            + " §c군사 " + (int) f.military
                            + " §d야망 " + String.format("%.2f", f.ambition));
                    if (!f.allies.isEmpty()) s.sendMessage("  §a동맹: " + f.allies);
                    if (!f.enemies.isEmpty()) s.sendMessage("  §c적대: " + f.enemies);
                }
            }
            case "market" -> {
                if (a.length < 2) return true;
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                Msg.send(s, "&6=== " + w + " 시장 ===");
                for (var e : plugin.market().of(w).entrySet()) {
                    MarketSimulator.MarketState st = e.getValue();
                    s.sendMessage("§7• " + e.getKey()
                            + " §7공급: §f" + (int) st.supply
                            + " §7수요: §f" + (int) st.demand
                            + " §6가격: §f" + (int) st.price + "실버");
                }
            }
            case "migration" -> {
                Msg.send(s, "&6=== 이주 통계 ===");
                for (WorldKey w : WorldKey.values()) {
                    var inMap = plugin.migration().inflow(w);
                    var outMap = plugin.migration().outflow(w);
                    if (inMap.isEmpty() && outMap.isEmpty()) continue;
                    int in = inMap.values().stream().mapToInt(Integer::intValue).sum();
                    int out = outMap.values().stream().mapToInt(Integer::intValue).sum();
                    s.sendMessage("§7• " + w + " §a유입 §f" + in + " §c유출 §f" + out);
                }
            }
            case "history" -> {
                if (a.length < 2) return true;
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                int n = a.length >= 3 ? Integer.parseInt(a[2]) : 20;
                Msg.send(s, "&6=== " + w + " 최근 사건 (" + n + ") ===");
                for (var e : plugin.history().recent(w, n)) {
                    s.sendMessage("§7• §f[" + e.kind + "] §7" + e.text);
                }
            }
            case "epoch" -> {
                if (a.length >= 2) {
                    WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                    Msg.send(s, "&6" + w + " 시대: §e" + plugin.epoch().label(plugin.epoch().of(w)));
                } else {
                    Msg.send(s, "&6=== 세계 시대 표 ===");
                    for (WorldKey w : WorldKey.values()) {
                        var ep = plugin.epoch().of(w);
                        s.sendMessage("§7• " + w + " §e" + plugin.epoch().label(ep));
                    }
                }
            }
            case "disaster" -> {
                if (a.length < 3) { Msg.warn(s, "/worldai disaster <WORLD> <type>"); return true; }
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                plugin.disasters().start(w, a[2], 300);
                Msg.send(s, "&a재해 강제 발생: " + a[2]);
            }
            case "weather" -> {
                if (a.length < 3) { Msg.warn(s, "/worldai weather <WORLD> <name>"); return true; }
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                plugin.weather().start(w, a[2], 10);
                Msg.send(s, "&a날씨 강제 발생: " + a[2]);
            }
            case "log" -> {
                int n = a.length > 1 ? Integer.parseInt(a[1]) : 10;
                plugin.comm().recent(n).forEach(m ->
                        s.sendMessage("§7[" + m.from + "→" + m.to + "] " + m.type + ": " + m.payload));
            }
            case "force" -> {
                if (a.length < 3) return true;
                WorldKey w = parseWorld(s, a[1]); if (w == null) return true;
                WorldAI ai = plugin.of(w);
                if (ai == null) { Msg.error(s, "AI 미활성"); return true; }
                String evt = a[2].toUpperCase();
                if ("WAR".equals(evt)) ai.state().tension = 95;
                else if ("FESTIVAL".equals(evt)) { ai.state().stability = 95; ai.state().tension = 5; }
                else if ("CRISIS".equals(evt)) ai.state().inflation = 250;
                else if ("REVOLT".equals(evt)) ai.state().stability = 15;
                else if ("OVERFLOW".equals(evt)) ai.state().mobBalance = 2.5;
                Msg.send(s, "&a강제 트리거: " + evt);
            }
            default -> Msg.warn(s, "알 수 없는 하위 명령: " + a[0]);
        }
        return true;
    }

    private WorldKey parseWorld(CommandSender s, String name) {
        try { return WorldKey.valueOf(name.toUpperCase()); }
        catch (Exception e) { Msg.error(s, "잘못된 세계: " + name); return null; }
    }
}
