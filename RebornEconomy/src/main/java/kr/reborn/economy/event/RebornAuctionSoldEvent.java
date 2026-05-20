package kr.reborn.economy.event;

import kr.reborn.economy.data.AuctionListing;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornAuctionSoldEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionListing listing;
    private final long price;

    public RebornAuctionSoldEvent(Player buyer, AuctionListing listing, long price) {
        super(buyer);
        this.listing = listing; this.price = price;
    }

    public AuctionListing listing() { return listing; }
    public long price() { return price; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
