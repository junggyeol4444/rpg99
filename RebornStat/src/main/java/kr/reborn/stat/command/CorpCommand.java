package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.CyberpunkGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /corp — 7대 메가코프 평판 조회·임무 보상.
 *
 * 사용:
 *   /corp                  — 7대 코프 평판 표시
 *   /corp join <CORP>      — 코프 시민 신청 (평판 +25, 라이벌 -12)
 *   /corp mission <type>   — admin: 임무 완료 적용 (minor/major/legend/betray)
 */
public final class CorpCommand implements CommandExecutor {
    private final RebornStat plugin;
    public CorpCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (data == null) return true;
        if (data.worldKey() != WorldKey.CYBERPUNK) {
            Msg.error(p, "메가코프 시스템은 사이버펑크 거주자만.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.CYBERPUNK);
        if (!(strategy instanceof CyberpunkGrowth corp)) {
            Msg.error(p, "사이버펑크 성장 strategy 없음.");
            return true;
        }
        if (a.length == 0) {
            Msg.send(p, "&6=== 7대 메가코프 평판 ===");
            // 코프별 점령 구역 수 집계
            java.util.Map<String, Integer> ownedCount = new java.util.HashMap<>();
            for (var d : corp.cities().all()) {
                if (d.currentOwner != null) {
                    ownedCount.merge(d.currentOwner, 1, Integer::sum);
                }
            }
            for (var e : corp.allCorpReputations(p.getUniqueId()).entrySet()) {
                int tier = corp.corpTier(e.getValue());
                String[] labels = {"&4적", "&c적대", "&7냉랭", "&f중립", "&a우호", "&b동맹"};
                String label = labels[Math.max(0, Math.min(labels.length - 1, tier))];
                int owned = ownedCount.getOrDefault(e.getKey(), 0);
                String districtLabel = owned > 0 ? " §6[구역 " + owned + "/7]" : "";
                p.sendMessage("§b• §f" + e.getKey() + " §7: §f" + e.getValue()
                        + " §8[" + label + "&8]" + districtLabel);
            }
            Msg.send(p, "&7/corp join <CORP> | /corp mission <type> | /district");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "gui" -> openGui(p, corp);
            case "join" -> {
                if (a.length < 2) { Msg.warn(p, "/corp join <CORP>"); return true; }
                String cid = a[1].toUpperCase();
                if (!corp.isCorp(cid)) { Msg.error(p, "유효한 CORP 아님. " + java.util.Arrays.toString(CyberpunkGrowth.CORPS)); return true; }
                corp.gainCorpFavor(p, cid, 25);
                Msg.send(p, "&a" + cid + " 시민 신청 — 평판 +25, 라이벌 -12");
            }
            case "mission" -> {
                if (!p.hasPermission("rebornstat.admin") && !p.isOp()) {
                    Msg.error(p, "관리 권한 필요.");
                    return true;
                }
                if (a.length < 3) { Msg.warn(p, "/corp mission <CORP> <minor|major|legend|betray>"); return true; }
                corp.onCorpMission(p, a[1].toUpperCase(), a[2].toLowerCase());
            }
            default -> Msg.warn(p, "/corp | /corp gui | /corp join | /corp mission");
        }
        return true;
    }

    /**
     * 7대 메가코프 평판 GUI — 각 코프를 아이템으로, 평판 tier 색상 + 점령 구역 수 lore.
     * 클릭 시 해당 코프 가입 (/corp join).
     */
    private void openGui(Player p, CyberpunkGrowth corp) {
        var b = plugin.gui().builder("&b7대 메가코프", 3);
        java.util.Map<String, Integer> ownedCount = new java.util.HashMap<>();
        for (var d : corp.cities().all()) {
            if (d.currentOwner != null) ownedCount.merge(d.currentOwner, 1, Integer::sum);
        }
        String patron = corp.patronCorp(p.getUniqueId());
        String[] labels = {"&4적", "&c적대", "&7냉랭", "&f중립", "&a우호", "&b동맹"};
        org.bukkit.Material[] mats = {
                org.bukkit.Material.IRON_BLOCK, org.bukkit.Material.REDSTONE_BLOCK,
                org.bukkit.Material.LAPIS_BLOCK, org.bukkit.Material.EMERALD_BLOCK,
                org.bukkit.Material.DIAMOND_BLOCK, org.bukkit.Material.GOLD_BLOCK,
                org.bukkit.Material.NETHERITE_BLOCK
        };
        int slot = 0;
        for (String cid : CyberpunkGrowth.CORPS) {
            final String corpId = cid;
            int rep = corp.corpReputation(p.getUniqueId(), cid);
            int tier = corp.corpTier(rep);
            String label = labels[Math.max(0, Math.min(labels.length - 1, tier))];
            int owned = ownedCount.getOrDefault(cid, 0);
            boolean isPatron = cid.equals(patron);
            var item = kr.reborn.core.util.Items.of(
                    mats[Math.min(slot, mats.length - 1)],
                    (isPatron ? "&e★ " : "&b") + cid,
                    "&7평판: &f" + rep + " &8[" + label + "&8]",
                    "&7점령 구역: &6" + owned + "/7",
                    isPatron ? "&e현재 후원 코프" : "",
                    "",
                    "&a클릭 — 시민 신청 (+25)");
            b.set(slot, item, e -> {
                p.closeInventory();
                corp.gainCorpFavor(p, corpId, 25);
                Msg.send(p, "&a" + corpId + " 시민 신청 — 평판 +25, 라이벌 -12");
            });
            slot++;
        }
        b.open(p);
    }
}
