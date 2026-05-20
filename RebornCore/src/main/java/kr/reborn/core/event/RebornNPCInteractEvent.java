package kr.reborn.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornNPCInteractEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String npcId;

    public RebornNPCInteractEvent(Player who, String npcId) {
        super(who);
        this.npcId = npcId;
    }
    public String npcId() { return npcId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
