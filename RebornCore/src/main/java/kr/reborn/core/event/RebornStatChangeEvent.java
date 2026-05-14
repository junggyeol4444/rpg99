package kr.reborn.core.event;

import kr.reborn.core.data.StatType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornStatChangeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final StatType stat;
    private final double oldValue, newValue;
    private final String source;

    public RebornStatChangeEvent(Player who, StatType stat, double oldValue, double newValue, String source) {
        super(who);
        this.stat = stat; this.oldValue = oldValue; this.newValue = newValue; this.source = source;
    }

    public StatType stat() { return stat; }
    public double oldValue() { return oldValue; }
    public double newValue() { return newValue; }
    public String source() { return source; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
