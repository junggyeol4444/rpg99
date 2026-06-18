package kr.reborn.stat.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import kr.reborn.stat.growth.impl.CityRegistry;
import kr.reborn.stat.growth.impl.CyberCity;
import kr.reborn.stat.growth.impl.CyberpunkGrowth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /district — 사이버시티 7대 구역 점령전.
 *
 * 사용:
 *   /district               — 전 구역 소유·내 후원 코프 영향력 표시
 *   /district enter <ID>    — 구역 진입 (활동 코프 영향력 누적 대상)
 *   /district exit          — 구역 이탈
 */
public final class DistrictCommand implements CommandExecutor {
    private final RebornStat plugin;
    public DistrictCommand(RebornStat p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (data == null) return true;
        if (data.worldKey() != WorldKey.CYBERPUNK) {
            Msg.error(p, "도시 구역 시스템은 사이버펑크 거주자만.");
            return true;
        }
        GrowthStrategy strategy = plugin.growth().of(WorldKey.CYBERPUNK);
        if (!(strategy instanceof CyberpunkGrowth corp)) {
            Msg.error(p, "사이버펑크 성장 strategy 없음.");
            return true;
        }
        CityRegistry cities = corp.cities();
        if (a.length == 0) {
            Msg.send(p, "&b=== 사이버시티 7대 구역 ===");
            String myPatron = corp.patronCorp(p.getUniqueId());
            String active = corp.activeDistrictOf(p.getUniqueId());
            for (var d : cities.all()) {
                String ownerLabel = d.currentOwner == null ? "&7무주공산"
                        : "&b" + d.currentOwner;
                String activeLabel = d.id.equals(active) ? " §a[활동중]" : "";
                p.sendMessage("§3• §f" + d.id + " §7- 소유: " + ownerLabel + activeLabel);
                if (myPatron != null) {
                    int infl = cities.influenceOf(d.id, myPatron);
                    if (infl > 0) {
                        p.sendMessage("    §7" + myPatron + " 영향력: §f"
                                + infl + " §7/ 1000 (점령선)");
                    }
                }
            }
            Msg.send(p, "&7/district enter <ID> | /district exit");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "gui" -> openGui(p, corp);
            case "enter" -> {
                if (a.length < 2) { Msg.warn(p, "/district enter <ID>"); return true; }
                String id = a[1].toUpperCase();
                if (!CyberCity.isDistrict(id)) {
                    Msg.error(p, "유효한 구역 아님. "
                            + java.util.Arrays.toString(CyberCity.DISTRICTS));
                    return true;
                }
                corp.setActiveDistrict(p, id);
                Msg.send(p, "&a구역 진입: " + id
                        + " §7— 이후 코프 활동이 이 구역 영향력에 누적.");
            }
            case "exit" -> {
                corp.clearActiveDistrict(p);
                Msg.send(p, "&7구역에서 이탈.");
            }
            default -> Msg.warn(p, "/district | /district gui | /district enter <ID> | /district exit");
        }
        return true;
    }

    /**
     * 사이버시티 7대 구역 GUI — 소유 코프·후원 코프 영향력·활동 여부.
     * 클릭 → 구역 진입, Shift-클릭 → 이탈.
     */
    private void openGui(Player p, CyberpunkGrowth corp) {
        var b = plugin.gui().builder("&b사이버시티 7대 구역", 3);
        CityRegistry cities = corp.cities();
        String myPatron = corp.patronCorp(p.getUniqueId());
        String active = corp.activeDistrictOf(p.getUniqueId());
        // 구역별 테마 — 자유시장/본사단지/빈민가/데이터코어/생체공학/네온부두/스카이브릿지.
        org.bukkit.Material[] mats = {
                org.bukkit.Material.GOLD_INGOT, org.bukkit.Material.GLASS,
                org.bukkit.Material.REDSTONE_BLOCK, org.bukkit.Material.LAPIS_BLOCK,
                org.bukkit.Material.PINK_GLAZED_TERRACOTTA, org.bukkit.Material.PRISMARINE_SHARD,
                org.bukkit.Material.QUARTZ_BLOCK
        };
        int slot = 0;
        for (var d : cities.all()) {
            final String did = d.id;
            String ownerLabel = d.currentOwner == null ? "&7무주공산" : "&b" + d.currentOwner;
            int patronInfl = myPatron == null ? 0 : cities.influenceOf(d.id, myPatron);
            boolean isActive = d.id.equals(active);
            var item = kr.reborn.core.util.Items.of(
                    mats[Math.min(slot, mats.length - 1)],
                    (isActive ? "&a▶ " : "&b") + d.id,
                    "&7소유: " + ownerLabel,
                    myPatron != null ? "&7" + myPatron + " 영향력: &f" + patronInfl + " &7/ 1000" : "&7후원 코프 없음",
                    isActive ? "&a현재 활동 중" : "",
                    "",
                    isActive ? "&7Shift-클릭 — 이탈" : "&a클릭 — 진입");
            b.set(slot, item, e -> {
                p.closeInventory();
                if (e.isShiftClick() && isActive) {
                    corp.clearActiveDistrict(p);
                    Msg.send(p, "&7구역에서 이탈.");
                } else {
                    corp.setActiveDistrict(p, did);
                    Msg.send(p, "&a구역 진입: " + did);
                }
            });
            slot++;
        }
        b.open(p);
    }
}
