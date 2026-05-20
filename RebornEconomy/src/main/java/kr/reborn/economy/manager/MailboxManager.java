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

    private final RebornEconomy plugin;
    private final Map<UUID, List<MailItem>> mailbox = new ConcurrentHashMap<>();

    public MailboxManager(RebornEconomy plugin) {
        this.plugin = plugin;
    }

    public void enqueue(MailItem item) {
        mailbox.computeIfAbsent(item.owner, k -> new ArrayList<>()).add(item);
    }

    public List<MailItem> of(UUID player) {
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
        List<MailItem> list = mailbox.get(p.getUniqueId());
        if (list == null || !list.remove(m)) return;
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
                if (now - it.next().sentAt > expireMs) it.remove();
            }
        }
    }

    public void flush() {
        // TODO: persist mailbox to DB
    }
}
