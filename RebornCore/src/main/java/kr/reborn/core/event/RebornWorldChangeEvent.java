package kr.reborn.core.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornWorldChangeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final WorldKey from, to;

    public RebornWorldChangeEvent(Player who, WorldKey from, WorldKey to) {
        super(who);
        this.from = from; this.to = to;
    }
    public WorldKey from() { return from; }
    public WorldKey to() { return to; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
