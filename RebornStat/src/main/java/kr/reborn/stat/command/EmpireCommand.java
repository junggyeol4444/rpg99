package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.OceanGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /empire — 7대 해양 제국 평판.
 *   /empire                — 전체 평판
 *   /empire join <EMPIRE>  — 시민 신청 (+25, 라이벌 -12)
 *   /empire mission <type> — admin: escort/naval/explore/pirate/betray
 */
public final class EmpireCommand implements CommandExecutor {
    private final RebornStat plugin;
    public EmpireCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (data == null) return true;
        if (data.worldKey() != WorldKey.OCEAN) {
            Msg.error(p, "해양 제국 시스템은 해양 거주자만.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.OCEAN);
        if (!(strategy instanceof OceanGrowth empire)) {
            Msg.error(p, "해양 성장 strategy 없음.");
            return true;
        }
        if (a.length == 0) {
            Msg.send(p, "&3=== 7대 해양 제국 평판 ===");
            for (var e : empire.allEmpireReputations(p.getUniqueId()).entrySet()) {
                int tier = empire.empireTier(e.getValue());
                String[] labels = {"&4적", "&c적대", "&7냉랭", "&f중립", "&a동맹", "&b시민"};
                String label = labels[Math.max(0, Math.min(labels.length - 1, tier))];
                p.sendMessage("§3• §f" + e.getKey() + " §7: §f" + e.getValue()
                        + " §8[" + label + "&8]");
            }
            Msg.send(p, "&7/empire join <EMPIRE> | /empire mission <e> <type>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "join" -> {
                if (a.length < 2) { Msg.warn(p, "/empire join <EMPIRE>"); return true; }
                String eid = a[1].toUpperCase();
                if (!empire.isEmpire(eid)) { Msg.error(p, "유효한 EMPIRE 아님. " + java.util.Arrays.toString(OceanGrowth.EMPIRES)); return true; }
                empire.gainEmpireFavor(p, eid, 25);
                Msg.send(p, "&a" + eid + " 시민 신청 — 평판 +25, 라이벌 -12");
            }
            case "mission" -> {
                if (!p.hasPermission("rebornstat.admin") && !p.isOp()) {
                    Msg.error(p, "관리 권한 필요.");
                    return true;
                }
                if (a.length < 3) { Msg.warn(p, "/empire mission <EMPIRE> <escort|naval|explore|pirate|betray>"); return true; }
                empire.onEmpireMission(p, a[1].toUpperCase(), a[2].toLowerCase());
            }
            default -> Msg.warn(p, "/empire | /empire join | /empire mission");
        }
        return true;
    }
}
