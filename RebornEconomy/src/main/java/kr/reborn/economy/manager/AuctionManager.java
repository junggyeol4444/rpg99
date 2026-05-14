package kr.reborn.economy.manager;

import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.data.AuctionListing;
import kr.reborn.economy.data.MailItem;
import kr.reborn.economy.event.RebornAuctionCreateEvent;
import kr.reborn.economy.event.RebornAuctionSoldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** 경매장. */
public final class AuctionManager {

    private final RebornEconomy plugin;
    private final List<AuctionListing> active = new CopyOnWriteArrayList<>();
    private final java.util.Map<UUID, Integer> playerListingCount = new ConcurrentHashMap<>();

    public AuctionManager(RebornEconomy plugin) {
        this.plugin = plugin;
    }

    public boolean register(Player seller, ItemStack item, String currency,
                            long startPrice, long buyout, long durationHours) {
        int max = plugin.getConfig().getInt("auction.max-listings-per-player", 10);
        int cur = playerListingCount.getOrDefault(seller.getUniqueId(), 0);
        if (cur >= max) {
            Msg.error(seller, "등록 가능한 매물 수를 초과했습니다 (" + max + ").");
            return false;
        }
        // 등록 수수료 시작가 2%
        long fee = (long) Math.floor(startPrice
                * plugin.getConfig().getDouble("auction.registration-fee-percent", 2.0) / 100.0);
        if (!plugin.currencies().withdraw(seller.getUniqueId(), currency, fee)) {
            Msg.error(seller, "등록 수수료(" + fee + " " + currency + ")가 부족합니다.");
            return false;
        }
        long expires = System.currentTimeMillis() + durationHours * 3600_000L;
        AuctionListing listing = new AuctionListing(UUID.randomUUID(), seller.getUniqueId(),
                item.clone(), currency, startPrice, buyout, expires);
        active.add(listing);
        playerListingCount.merge(seller.getUniqueId(), 1, Integer::sum);
        Bukkit.getPluginManager().callEvent(new RebornAuctionCreateEvent(seller, listing));
        Msg.send(seller, "&a경매 등록 완료. 시작가 &f" + startPrice + " " + currency);
        return true;
    }

    public List<AuctionListing> active() { return active; }

    public void open(Player p) {
        var b = plugin.gui().builder("&6경매장", 6);
        int slot = 0;
        for (AuctionListing l : active) {
            if (slot >= 45) break;
            ItemStack display = l.item.clone();
            display = Items.tagged(plugin, display, "ah_id", l.id.toString());
            // 가격 lore 추가
            var meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(Msg.c("&7현재가: &f" + l.currentBid + " " + l.currency));
                if (l.buyoutPrice > 0) lore.add(Msg.c("&7즉구가: &f" + l.buyoutPrice + " " + l.currency));
                long secondsLeft = Math.max(0, (l.expiresAt - System.currentTimeMillis()) / 1000);
                lore.add(Msg.c("&7남은 시간: &f" + secondsLeft + "초"));
                lore.add(Msg.c("&a좌클릭: +5% 입찰  &c우클릭: 즉시구매"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            final AuctionListing item = l;
            b.set(slot++, display, e -> {
                if (e.isLeftClick()) bid(p, item, (long) Math.ceil(item.currentBid * 1.05));
                else if (e.isRightClick() && item.buyoutPrice > 0) buyout(p, item);
            });
        }
        b.set(53, Items.of(Material.BARRIER, "&c닫기"), e -> p.closeInventory());
        b.open(p);
    }

    public void bid(Player p, AuctionListing l, long amount) {
        if (l.isExpired() || !active.contains(l)) { Msg.error(p, "만료된 매물입니다."); return; }
        if (amount <= l.currentBid) { Msg.error(p, "현재가보다 높아야 합니다."); return; }
        if (!plugin.currencies().withdraw(p.getUniqueId(), l.currency, amount)) {
            Msg.error(p, "잔액 부족.");
            return;
        }
        // 이전 입찰자 환불
        if (l.currentBidder != null) {
            plugin.currencies().deposit(l.currentBidder, l.currency, l.currentBid);
        }
        l.currentBid = amount;
        l.currentBidder = p.getUniqueId();
        Msg.send(p, "&a입찰 성공: &f" + amount);
    }

    public void buyout(Player p, AuctionListing l) {
        if (l.buyoutPrice <= 0) return;
        if (!active.remove(l)) return;
        if (!plugin.currencies().withdraw(p.getUniqueId(), l.currency, l.buyoutPrice)) {
            Msg.error(p, "잔액 부족.");
            active.add(l);
            return;
        }
        if (l.currentBidder != null) plugin.currencies().deposit(l.currentBidder, l.currency, l.currentBid);
        long fee = (long) Math.floor(l.buyoutPrice
                * plugin.getConfig().getDouble("auction.sale-fee-percent", 3.0) / 100.0);
        plugin.currencies().deposit(l.seller, l.currency, l.buyoutPrice - fee);
        plugin.mailbox().enqueue(new MailItem(UUID.randomUUID(), p.getUniqueId(),
                "경매 낙찰", l.item.clone(), null, 0));
        playerListingCount.merge(l.seller, -1, Integer::sum);
        Bukkit.getPluginManager().callEvent(new RebornAuctionSoldEvent(p, l, l.buyoutPrice));
        Msg.send(p, "&a즉시구매! 우편함에서 수령하세요.");
    }

    /** 만료 매물 처리 — 1분마다 호출. */
    public void tickExpire() {
        Iterator<AuctionListing> it = active.iterator();
        while (it.hasNext()) {
            AuctionListing l = it.next();
            if (!l.isExpired()) continue;
            active.remove(l);
            if (l.currentBidder != null) {
                // 낙찰
                long fee = (long) Math.floor(l.currentBid
                        * plugin.getConfig().getDouble("auction.sale-fee-percent", 3.0) / 100.0);
                plugin.currencies().deposit(l.seller, l.currency, l.currentBid - fee);
                plugin.mailbox().enqueue(new MailItem(UUID.randomUUID(), l.currentBidder,
                        "경매 낙찰", l.item.clone(), null, 0));
            } else {
                // 유찰 — 판매자 우편함으로 반환
                plugin.mailbox().enqueue(new MailItem(UUID.randomUUID(), l.seller,
                        "경매 유찰", l.item.clone(), null, 0));
            }
            playerListingCount.merge(l.seller, -1, Integer::sum);
        }
    }

    public void flush() {
        // TODO: 경매 매물 DB 저장
    }
}
