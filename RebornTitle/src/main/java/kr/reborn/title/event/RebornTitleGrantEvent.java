package kr.reborn.title.event;

import kr.reborn.title.data.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornTitleGrantEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Title title;
    public RebornTitleGrantEvent(Player p, Title t) { super(p); this.title = t; }
    public Title title() { return title; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
