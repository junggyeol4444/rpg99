package kr.reborn.economy.listener;

import kr.reborn.core.event.RebornNpcWorldImpactEvent;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * NPC가 사업을 시작하면(RebornNpcWorldImpactEvent.SHOP_OPENED) Economy에 NPC 상점을 실제 등록.
 * "NPC가 자율적으로 상점을 차린다"를 경제 도메인까지 연결 — /shop 으로 거래 가능해진다.
 */
public final class EconomyWorldImpactListener implements Listener {

    private final RebornEconomy plugin;

    public EconomyWorldImpactListener(RebornEconomy plugin) { this.plugin = plugin; }

    @EventHandler
    public void onImpact(RebornNpcWorldImpactEvent e) {
        if (e.kind != RebornNpcWorldImpactEvent.Kind.SHOP_OPENED) return;
        String id = "npc_shop_" + e.npcId;
        String name = (e.payload == null || e.payload.isEmpty()) ? "NPC 상점" : e.payload;
        var shop = plugin.shops().registerNpc(id, name);
        if (shop != null) {
            Bukkit.broadcastMessage("§e§l[개업] §f" + name + "이(가) 문을 열었다. §7(/shop " + id + ")");
        }
    }
}
