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
            java.util.Map<String, Integer> ruledCount = new java.util.HashMap<>();
            for (var port : empire.ports().all()) {
                if (port.currentRuler != null) {
                    ruledCount.merge(port.currentRuler, 1, Integer::sum);
                }
            }
            for (var e : empire.allEmpireReputations(p.getUniqueId()).entrySet()) {
                int tier = empire.empireTier(e.getValue());
                String[] labels = {"&4적", "&c적대", "&7냉랭", "&f중립", "&a동맹", "&b시민"};
                String label = labels[Math.max(0, Math.min(labels.length - 1, tier))];
                int ruled = ruledCount.getOrDefault(e.getKey(), 0);
                String portLabel = ruled > 0 ? " §6[항구 " + ruled + "/7]" : "";
                p.sendMessage("§3• §f" + e.getKey() + " §7: §f" + e.getValue()
                        + " §8[" + label + "&8]" + portLabel);
            }
            Msg.send(p, "&7/empire join <EMPIRE> | /empire mission <e> <type> | /port");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "gui" -> openGui(p, empire);
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
            default -> Msg.warn(p, "/empire | /empire gui | /empire join | /empire mission");
        }
        return true;
    }

    /**
     * 7대 해양 제국 평판 GUI — 각 제국을 테마 아이템으로, 평판 tier 색상 + 점령 항구 수 lore.
     * 후원 제국은 ★, 클릭 시 해당 제국 가입 (/empire join).
     */
    private void openGui(Player p, OceanGrowth empire) {
        var b = plugin.gui().builder("&37대 해양 제국", 3);
        java.util.Map<String, Integer> ruledCount = new java.util.HashMap<>();
        for (var port : empire.ports().all()) {
            if (port.currentRuler != null) ruledCount.merge(port.currentRuler, 1, Integer::sum);
        }
        String patron = empire.patronEmpire(p.getUniqueId());
        String[] labels = {"&4적", "&c적대", "&7냉랭", "&f중립", "&a동맹", "&b시민"};
        // 제국별 테마 아이템 — 아쿠아리온=해군(트라이던트), 코럴=무역(에메랄드), 크라켄=신정(프리즈마린),
        // 펄=인어(해양심장), 자유해=해적(블랙썰), 폭풍=군사(번개기), 망자=언데드(해골).
        org.bukkit.Material[] mats = {
                org.bukkit.Material.TRIDENT, org.bukkit.Material.EMERALD_BLOCK,
                org.bukkit.Material.PRISMARINE_BRICKS, org.bukkit.Material.HEART_OF_THE_SEA,
                org.bukkit.Material.BLACK_BANNER, org.bukkit.Material.LIGHTNING_ROD,
                org.bukkit.Material.SKELETON_SKULL
        };
        int slot = 0;
        for (String eid : OceanGrowth.EMPIRES) {
            final String empireId = eid;
            int rep = empire.empireReputation(p.getUniqueId(), eid);
            int tier = empire.empireTier(rep);
            String label = labels[Math.max(0, Math.min(labels.length - 1, tier))];
            int ruled = ruledCount.getOrDefault(eid, 0);
            boolean isPatron = eid.equals(patron);
            var item = kr.reborn.core.util.Items.of(
                    mats[Math.min(slot, mats.length - 1)],
                    (isPatron ? "&e★ " : "&3") + eid,
                    "&7평판: &f" + rep + " &8[" + label + "&8]",
                    "&7점령 항구: &6" + ruled + "/7",
                    isPatron ? "&e현재 후원 제국" : "",
                    "",
                    "&a클릭 — 시민 신청 (+25)");
            b.set(slot, item, e -> {
                p.closeInventory();
                empire.gainEmpireFavor(p, empireId, 25);
                Msg.send(p, "&a" + empireId + " 시민 신청 — 평판 +25, 라이벌 -12");
            });
            slot++;
        }
        b.open(p);
    }
}
