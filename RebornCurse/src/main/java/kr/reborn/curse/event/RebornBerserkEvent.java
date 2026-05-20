package kr.reborn.curse.event;

import kr.reborn.curse.data.EffectDef;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornBerserkEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final EffectDef cause;
    public RebornBerserkEvent(Player p, EffectDef c) { super(p); this.cause = c; }
    public EffectDef cause() { return cause; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
