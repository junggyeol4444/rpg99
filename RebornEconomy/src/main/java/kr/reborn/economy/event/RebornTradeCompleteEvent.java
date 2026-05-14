package kr.reborn.economy.event;

import kr.reborn.economy.manager.TradeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RebornTradeCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player a, b;
    private final TradeManager.Session session;

    public RebornTradeCompleteEvent(Player a, Player b, TradeManager.Session s) {
        this.a = a; this.b = b; this.session = s;
    }

    public Player a() { return a; }
    public Player b() { return b; }
    public TradeManager.Session session() { return session; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
