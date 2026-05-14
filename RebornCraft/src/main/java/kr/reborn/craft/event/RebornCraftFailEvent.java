package kr.reborn.craft.event;

import kr.reborn.craft.data.Recipe;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornCraftFailEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Recipe recipe;
    public RebornCraftFailEvent(Player p, Recipe r) { super(p); this.recipe = r; }
    public Recipe recipe() { return recipe; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
