package kr.reborn.spawn.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class RebornRouletteResultEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final WorldKey result;

    public RebornRouletteResultEvent(Player p, WorldKey result) {
        super(p);
        this.result = result;
    }
    public WorldKey result() { return result; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
