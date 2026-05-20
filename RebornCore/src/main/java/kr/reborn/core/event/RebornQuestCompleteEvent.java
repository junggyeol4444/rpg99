package kr.reborn.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornQuestCompleteEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String questId;

    public RebornQuestCompleteEvent(Player who, String questId) {
        super(who);
        this.questId = questId;
    }
    public String questId() { return questId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
