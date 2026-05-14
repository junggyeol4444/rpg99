package kr.reborn.title.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornTitleChangeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String previous, current;
    public RebornTitleChangeEvent(Player p, String prev, String cur) {
        super(p); this.previous = prev; this.current = cur;
    }
    public String previous() { return previous; }
    public String current() { return current; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
