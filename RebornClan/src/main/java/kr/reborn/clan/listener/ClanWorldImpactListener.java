package kr.reborn.clan.listener;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.event.RebornNpcWorldImpactEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * NPC가 초월(ASCEND)하여 왕/마왕/용왕 등이 되면(RebornNpcWorldImpactEvent.ASCENDED)
 * Clan 도메인에 왕국을 실제 등록. "NPC가 자율적으로 왕국을 세운다"를 Clan까지 연결.
 */
public final class ClanWorldImpactListener implements Listener {

    private final RebornClan plugin;

    public ClanWorldImpactListener(RebornClan plugin) { this.plugin = plugin; }

    @EventHandler
    public void onImpact(RebornNpcWorldImpactEvent e) {
        if (e.kind != RebornNpcWorldImpactEvent.Kind.ASCENDED) return;
        String id = "npc_kingdom_" + e.npcId;
        String name = (e.payload == null || e.payload.isEmpty() ? "초월자" : e.payload) + "의 왕국";
        var k = plugin.kingdoms().registerNpc(id, name, e.npcId);
        if (k != null) {
            Bukkit.broadcastMessage("§6§l[왕국 등록] §f" + name + "이(가) 대륙에 들어섰다.");
        }
    }
}
