package kr.reborn.curse.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornCurseCureEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String id;
    public RebornCurseCureEvent(Player p, String id) { super(p); this.id = id; }
    public String id() { return id; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
