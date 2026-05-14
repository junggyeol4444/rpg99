package kr.reborn.worldai.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RebornDisasterStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public final WorldKey world;
    public final String disasterType;
    public final int durationSeconds;

    public RebornDisasterStartEvent(WorldKey w, String t, int dur) {
        this.world = w; this.disasterType = t; this.durationSeconds = dur;
    }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
