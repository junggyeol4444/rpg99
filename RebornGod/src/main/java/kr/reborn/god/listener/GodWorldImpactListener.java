package kr.reborn.god.listener;

import kr.reborn.core.event.RebornNpcWorldImpactEvent;
import kr.reborn.god.RebornGod;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * NPC가 종교를 창시하면(RebornNpcWorldImpactEvent.RELIGION_FOUNDED) 신계에 교단을 실제 등록.
 * "NPC가 자율적으로 종교를 창시한다"(기획서 1장)를 God 도메인까지 연결.
 */
public final class GodWorldImpactListener implements Listener {

    private final RebornGod plugin;

    public GodWorldImpactListener(RebornGod plugin) { this.plugin = plugin; }

    @EventHandler
    public void onImpact(RebornNpcWorldImpactEvent e) {
        if (e.kind != RebornNpcWorldImpactEvent.Kind.RELIGION_FOUNDED) return;
        String id = "npc_religion_" + e.npcId;
        var r = plugin.religions().registerNpc(id, e.payload, e.npcId);
        if (r != null) {
            Bukkit.broadcastMessage("§5§l[교단 등록] §f" + e.payload + " 교단이 신계에 등록되었다.");
        }
    }
}
