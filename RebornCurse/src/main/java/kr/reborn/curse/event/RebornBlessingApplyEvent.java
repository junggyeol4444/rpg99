package kr.reborn.curse.event;

import kr.reborn.curse.data.EffectDef;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornBlessingApplyEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final EffectDef def;
    public RebornBlessingApplyEvent(Player p, EffectDef d) { super(p); this.def = d; }
    public EffectDef def() { return def; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
