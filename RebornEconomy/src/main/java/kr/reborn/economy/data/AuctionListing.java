package kr.reborn.economy.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** 경매 매물 1건. */
public final class AuctionListing {
    public final UUID id;
    public final UUID seller;
    public final ItemStack item;
    public final String currency;
    public final long startPrice;
    public final long buyoutPrice;   // 0 = 즉시구매 없음
    public final long expiresAt;     // epoch ms
    public long currentBid;
    public UUID currentBidder;       // null = 입찰 없음

    public AuctionListing(UUID id, UUID seller, ItemStack item, String currency,
                          long startPrice, long buyoutPrice, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.currency = currency;
        this.startPrice = startPrice;
        this.buyoutPrice = buyoutPrice;
        this.expiresAt = expiresAt;
        this.currentBid = startPrice;
        this.currentBidder = null;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
