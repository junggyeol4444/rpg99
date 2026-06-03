package kr.reborn.economy.manager;

import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.data.MailItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 우편함 — 경매 낙찰물·시스템 보상. */
public final class MailboxManager {

    private static final String NS = "RebornEconomy.mailbox";

    private final RebornEconomy plugin;
    private final Map<UUID, List<MailItem>> mailbox = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public MailboxManager(RebornEconomy plugin) {
        this.plugin = plugin;
    }

    private void ensureLoaded(UUID owner) {
        if (loaded.add(owner)) {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, owner);
            List<MailItem> list = new ArrayList<>();
            for (var e : all.entrySet()) {
                try {
                    // value: subject|currencyId|currencyAmount|sentAt|<itemBase64>
                    String[] parts = e.getValue().split("\\|", 5);
                    if (parts.length < 5) continue;
                    String subj = parts[0];
                    String currId = parts[1].isEmpty() ? null : parts[1];
                    long currAmt = Long.parseLong(parts[2]);
                    long sentAt = Long.parseLong(parts[3]);
                    org.bukkit.inventory.ItemStack item = kr.reborn.core.util.ItemSerializer
                            .fromBase64(parts[4]);
                    UUID id = UUID.fromString(e.getKey());
                    MailItem m = new MailItem(id, owner, subj, item, currId, currAmt);
                    m.sentAt = sentAt;
                    list.add(m);
                } catch (Throwable ignored) {}
            }
            if (!list.isEmpty()) mailbox.put(owner, list);
        }
    }

    private void persist(MailItem m) {
        try {
            String itemB64 = m.item == null ? "" : kr.reborn.core.util.ItemSerializer.toBase64(m.item);
            String enc = m.subject + "|"
                    + (m.currencyId == null ? "" : m.currencyId) + "|"
                    + m.currencyAmount + "|" + m.sentAt + "|"
                    + (itemB64 == null ? "" : itemB64);
            kr.reborn.core.RebornCore.get().kv().put(NS, m.owner, m.id.toString(), enc);
        } catch (Throwable ignored) {}
    }

    public void enqueue(MailItem item) {
        ensureLoaded(item.owner);
        mailbox.computeIfAbsent(item.owner, k -> new ArrayList<>()).add(item);
        persist(item);
    }

    public List<MailItem> of(UUID player) {
        ensureLoaded(player);
        return mailbox.getOrDefault(player, List.of());
    }

    public void open(Player p) {
        var b = plugin.gui().builder("&6우편함", 6);
        List<MailItem> items = mailbox.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>());
        int slot = 0;
        for (MailItem m : items) {
            if (slot >= 45) break;
            var icon = m.item != null ? m.item.clone()
                    : Items.of(Material.PAPER, "&e" + m.subject);
            final MailItem ref = m;
            b.set(slot++, icon, e -> claim(p, ref));
        }
        b.open(p);
    }

    public void claim(Player p, MailItem m) {
        ensureLoaded(p.getUniqueId());
        List<MailItem> list = mailbox.get(p.getUniqueId());
        if (list == null || !list.remove(m)) return;
        kr.reborn.core.RebornCore.get().kv().remove(NS, p.getUniqueId(), m.id.toString());
        if (m.item != null) p.getInventory().addItem(m.item);
        if (m.currencyId != null && m.currencyAmount > 0) {
            plugin.currencies().deposit(p.getUniqueId(), m.currencyId, m.currencyAmount);
        }
        Msg.send(p, "&a수령 완료: " + m.subject);
        p.closeInventory();
    }

    /** 만료된 메일 제거. */
    public void purgeExpired() {
        long expireMs = plugin.getConfig().getLong("mailbox.expire-days", 30) * 86_400_000L;
        long now = System.currentTimeMillis();
        for (List<MailItem> list : mailbox.values()) {
            Iterator<MailItem> it = list.iterator();
            while (it.hasNext()) {
                MailItem m = it.next();
                if (now - m.sentAt > expireMs) {
                    it.remove();
                    kr.reborn.core.RebornCore.get().kv().remove(NS, m.owner, m.id.toString());
                }
            }
        }
    }

    public void flush() {
        // 영속화는 enqueue/claim/purgeExpired 각 호출 시점에 발생 (즉시 저장 패턴)
    }
}
