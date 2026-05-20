package kr.reborn.economy.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornExchangeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String from, to;
    private final long fromAmount, toAmount, fee;

    public RebornExchangeEvent(Player p, String from, String to,
                               long fromAmount, long toAmount, long fee) {
        super(p);
        this.from = from; this.to = to;
        this.fromAmount = fromAmount; this.toAmount = toAmount; this.fee = fee;
    }

    public String from() { return from; }
    public String to() { return to; }
    public long fromAmount() { return fromAmount; }
    public long toAmount() { return toAmount; }
    public long fee() { return fee; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
