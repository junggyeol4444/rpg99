package kr.reborn.economy.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** 우편함 1건 (낙찰 아이템 / 시스템 보상 등). */
public final class MailItem {
    public final UUID id;
    public final UUID owner;
    public final String subject;
    public final ItemStack item;
    public final String currencyId;  // null이면 화폐 첨부 없음
    public final long currencyAmount;
    public final long sentAt;

    public MailItem(UUID id, UUID owner, String subject, ItemStack item,
                    String currencyId, long currencyAmount) {
        this.id = id;
        this.owner = owner;
        this.subject = subject;
        this.item = item;
        this.currencyId = currencyId;
        this.currencyAmount = currencyAmount;
        this.sentAt = System.currentTimeMillis();
    }
}
