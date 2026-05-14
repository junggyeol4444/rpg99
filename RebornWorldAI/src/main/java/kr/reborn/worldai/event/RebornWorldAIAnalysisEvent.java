package kr.reborn.worldai.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RebornWorldAIAnalysisEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public final WorldKey world;
    public final double inflation, tension, stability;

    public RebornWorldAIAnalysisEvent(WorldKey w, double inflation, double tension, double stability) {
        this.world = w; this.inflation = inflation;
        this.tension = tension; this.stability = stability;
    }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
