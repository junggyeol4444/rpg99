package kr.reborn.title.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornFirstPlaceEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final WorldKey world;
    public RebornFirstPlaceEvent(Player p, WorldKey w) { super(p); this.world = w; }
    public WorldKey world() { return world; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
