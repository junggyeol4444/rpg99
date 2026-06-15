package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.OceanGrowth;
import kr.reborn.stat.growth.impl.OceanPort;
import kr.reborn.stat.growth.impl.PortRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /port — 해양 7대 항구 점령전.
 *
 *   /port                  — 전 항구 통치자·내 후원 제국 영향력 표시
 *   /port enter <ID>       — 항구 정박 (활동 제국 영향력 누적 대상)
 *   /port exit             — 항구 이탈
 */
public final class PortCommand implements CommandExecutor {
    private final RebornStat plugin;
    public PortCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (data == null) return true;
        if (data.worldKey() != WorldKey.OCEAN) {
            Msg.error(p, "항구 점령 시스템은 해양 거주자만.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.OCEAN);
        if (!(strategy instanceof OceanGrowth empire)) {
            Msg.error(p, "해양 성장 strategy 없음.");
            return true;
        }
        PortRegistry portsR = empire.ports();
        if (a.length == 0) {
            Msg.send(p, "&3=== 해양 7대 항구 ===");
            String myPatron = empire.patronEmpire(p.getUniqueId());
            String active = empire.activePortOf(p.getUniqueId());
            for (var port : portsR.all()) {
                String rulerLabel = port.currentRuler == null ? "&7무인"
                        : "&b" + port.currentRuler;
                String activeLabel = port.id.equals(active) ? " §a[정박중]" : "";
                p.sendMessage("§3• §f" + port.id + " §7- 통치: "
                        + rulerLabel + activeLabel);
                if (myPatron != null) {
                    int infl = portsR.influenceOf(port.id, myPatron);
                    if (infl > 0) {
                        p.sendMessage("    §7" + myPatron + " 영향력: §f"
                                + infl + " §7/ 1000 (점령선)");
                    }
                }
            }
            Msg.send(p, "&7/port enter <ID> | /port exit");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "enter" -> {
                if (a.length < 2) { Msg.warn(p, "/port enter <ID>"); return true; }
                String id = a[1].toUpperCase();
                if (!OceanPort.isPort(id)) {
                    Msg.error(p, "유효한 항구 아님. "
                            + java.util.Arrays.toString(OceanPort.PORTS));
                    return true;
                }
                empire.setActivePort(p, id);
                Msg.send(p, "&a항구 정박: " + id
                        + " §7— 이후 제국 활동이 이 항구 영향력에 누적.");
            }
            case "exit" -> {
                empire.clearActivePort(p);
                Msg.send(p, "&7항구에서 출항.");
            }
            default -> Msg.warn(p, "/port | /port enter <ID> | /port exit");
        }
        return true;
    }
}
