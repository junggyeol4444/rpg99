package kr.reborn.tutorial.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public final class RebornTutorialStageChangeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int stage;
    public RebornTutorialStageChangeEvent(Player p, int stage) { super(p); this.stage = stage; }
    public int stage() { return stage; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
