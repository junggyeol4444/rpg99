package kr.reborn.core.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornTierUpEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String previous, current;
    private final WorldKey world;

    public RebornTierUpEvent(Player who, String previous, String current, WorldKey world) {
        super(who);
        this.previous = previous; this.current = current; this.world = world;
    }
    public String previous() { return previous; }
    public String current() { return current; }
    public WorldKey world() { return world; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
