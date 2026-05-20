package kr.reborn.core.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

public class RebornDeathEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Location at;
    @Nullable private final Player killer;
    private final String cause;

    public RebornDeathEvent(Player who, Location at, @Nullable Player killer, String cause) {
        super(who);
        this.at = at; this.killer = killer; this.cause = cause;
    }
    public Location at() { return at; }
    @Nullable public Player killer() { return killer; }
    public String cause() { return cause; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
