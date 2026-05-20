package kr.reborn.economy.event;

import kr.reborn.economy.data.AuctionListing;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornAuctionCreateEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionListing listing;

    public RebornAuctionCreateEvent(Player seller, AuctionListing listing) {
        super(seller);
        this.listing = listing;
    }

    public AuctionListing listing() { return listing; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
