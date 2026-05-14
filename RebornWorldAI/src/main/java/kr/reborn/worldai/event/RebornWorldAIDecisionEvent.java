package kr.reborn.worldai.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RebornWorldAIDecisionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public final WorldKey world;
    public final String decisionKey;
    public final String label;

    public RebornWorldAIDecisionEvent(WorldKey w, String key, String label) {
        this.world = w; this.decisionKey = key; this.label = label;
    }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
