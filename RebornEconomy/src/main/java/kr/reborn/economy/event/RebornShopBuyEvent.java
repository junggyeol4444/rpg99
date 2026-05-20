package kr.reborn.economy.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RebornShopBuyEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String shopId, itemId;
    private final int amount;
    private final long price;

    public RebornShopBuyEvent(Player buyer, String shopId, String itemId, int amount, long price) {
        super(buyer);
        this.shopId = shopId; this.itemId = itemId; this.amount = amount; this.price = price;
    }

    public String shopId() { return shopId; }
    public String itemId() { return itemId; }
    public int amount() { return amount; }
    public long price() { return price; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
