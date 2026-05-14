package kr.reborn.worldai.event;

import kr.reborn.core.data.WorldKey;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RebornWeatherChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public final WorldKey world;
    public final String weather;
    public final int durationMinutes;

    public RebornWeatherChangeEvent(WorldKey w, String weather, int dur) {
        this.world = w; this.weather = weather; this.durationMinutes = dur;
    }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
