package kr.reborn.npc.interact;

import kr.reborn.core.event.RebornNPCInteractEvent;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class NpcInteractListener implements Listener {

    private final RebornNPC plugin;

    public NpcInteractListener(RebornNPC plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        var npc = plugin.registry().byEntity(e.getRightClicked().getUniqueId());
        if (npc == null) return;
        e.setCancelled(true);
        Bukkit.getPluginManager().callEvent(new RebornNPCInteractEvent(e.getPlayer(), npc.id));
        // 호감도 상승
        npc.relations.addPlayer(e.getPlayer().getUniqueId(), 0.5);
        npc.emotion.add(Emotion.Kind.CURIOSITY, 1.0);
        e.getPlayer().sendMessage("§6[" + npc.displayName + "] §f무슨 일이오?");

        // 은둔고수 정체 공개 체크
        if (npc.hermit && !npc.revealed) {
            int reveal = plugin.getConfig().getInt("hermit.reveal-favor", 80);
            if (npc.relations.player(e.getPlayer().getUniqueId()) >= reveal) {
                npc.revealed = true;
                Bukkit.broadcastMessage("§5[은둔고수] §f" + npc.displayName + "의 정체가 드러났다!");
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        var npc = plugin.registry().byEntity(e.getEntity().getUniqueId());
        if (npc == null) return;
        npc.emotion.add(Emotion.Kind.ANGER, 25);
        npc.emotion.add(Emotion.Kind.TRUST, -10);
        if (e.getDamager() instanceof org.bukkit.entity.Player p) {
            npc.relations.addPlayer(p.getUniqueId(), -5);
        }
    }
}
