package kr.reborn.craft.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornProficiencyUpEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String profession, tierName;
    private final int totalExp;
    public RebornProficiencyUpEvent(Player p, String prof, String tier, int exp) {
        super(p); this.profession = prof; this.tierName = tier; this.totalExp = exp;
    }
    public String profession() { return profession; }
    public String tierName() { return tierName; }
    public int totalExp() { return totalExp; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
