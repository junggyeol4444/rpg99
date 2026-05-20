package kr.reborn.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornSkillLearnEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String skillId;

    public RebornSkillLearnEvent(Player who, String skillId) {
        super(who);
        this.skillId = skillId;
    }
    public String skillId() { return skillId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
