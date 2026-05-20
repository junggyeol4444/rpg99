package kr.reborn.craft.event;

import kr.reborn.craft.data.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornItemConsumeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CustomItem item;
    public RebornItemConsumeEvent(Player p, CustomItem ci) { super(p); this.item = ci; }
    public CustomItem item() { return item; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
