package kr.reborn.economy.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.event.RebornTradeCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 플레이어 간 거래 — 단순 구현.
 * /trade <player> 시작 → 양쪽 confirm → 5초 카운트다운 → 교환.
 */
public final class TradeManager {

    private final RebornEconomy plugin;

    public static final class Session {
        public final UUID a, b;
        public ItemStack itemA, itemB;
        public long currencyA, currencyB;
        public String currencyAId, currencyBId;
        public boolean readyA, readyB;
        public long startedAt;

        public Session(UUID a, UUID b) {
            this.a = a; this.b = b;
            this.startedAt = System.currentTimeMillis();
        }
    }

    /** 양쪽 키로 동일한 세션 가리킴. */
    private final Map<UUID, Session> sessions = new HashMap<>();

    public TradeManager(RebornEconomy plugin) {
        this.plugin = plugin;
    }

    public void open(Player from, Player to) {
        if (from.getUniqueId().equals(to.getUniqueId())) {
            Msg.error(from, "자기 자신과 거래할 수 없습니다.");
            return;
        }
        if (sessions.containsKey(from.getUniqueId()) || sessions.containsKey(to.getUniqueId())) {
            Msg.error(from, "이미 거래 중입니다.");
            return;
        }
        Session s = new Session(from.getUniqueId(), to.getUniqueId());
        sessions.put(from.getUniqueId(), s);
        sessions.put(to.getUniqueId(), s);
        Msg.send(from, "&a거래 시작: &f" + to.getName() + " &7(/trade ready · /trade cancel)");
        Msg.send(to, "&a거래 시작: &f" + from.getName() + " &7(/trade ready · /trade cancel)");
    }

    public Session of(UUID id) { return sessions.get(id); }

    public void cancel(Player p) {
        Session s = sessions.remove(p.getUniqueId());
        if (s == null) return;
        sessions.remove(s.a);
        sessions.remove(s.b);
        Player a = Bukkit.getPlayer(s.a);
        Player b = Bukkit.getPlayer(s.b);
        if (a != null) Msg.warn(a, "거래 취소됨.");
        if (b != null) Msg.warn(b, "거래 취소됨.");
    }

    public void ready(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null) { Msg.error(p, "진행 중인 거래가 없습니다."); return; }
        if (p.getUniqueId().equals(s.a)) s.readyA = true;
        else s.readyB = true;
        Msg.send(p, "&7준비 완료.");
        if (s.readyA && s.readyB) startCountdown(s);
    }

    private void startCountdown(Session s) {
        Player a = Bukkit.getPlayer(s.a);
        Player b = Bukkit.getPlayer(s.b);
        if (a == null || b == null) { cancelSession(s); return; }
        int seconds = plugin.getConfig().getInt("trade.countdown-seconds", 5);
        Msg.send(a, "&e거래 확정까지 " + seconds + "초...");
        Msg.send(b, "&e거래 확정까지 " + seconds + "초...");
        RebornCore.get().scheduler().runTaskLater(() -> finalize(s), seconds * 20L);
    }

    private void finalize(Session s) {
        if (sessions.remove(s.a) == null) return;
        sessions.remove(s.b);
        Player a = Bukkit.getPlayer(s.a);
        Player b = Bukkit.getPlayer(s.b);
        if (a == null || b == null) return;

        // 아포칼립스 = 물물교환만
        boolean apocBarter = plugin.getConfig().getBoolean("trade.apocalypse-barter-only", true);
        WorldKey wA = RebornCore.get().api().getCurrentWorld(s.a);
        WorldKey wB = RebornCore.get().api().getCurrentWorld(s.b);
        if (apocBarter && (wA == WorldKey.APOCALYPSE || wB == WorldKey.APOCALYPSE)) {
            if (s.currencyA > 0 || s.currencyB > 0) {
                Msg.error(a, "아포칼립스에서는 물물교환만 가능합니다.");
                Msg.error(b, "아포칼립스에서는 물물교환만 가능합니다.");
                return;
            }
        }

        // 화폐 차감 / 입금
        if (s.currencyA > 0 && !plugin.currencies().withdraw(s.a, s.currencyAId, s.currencyA)) {
            Msg.error(a, "잔액 부족."); return;
        }
        if (s.currencyB > 0 && !plugin.currencies().withdraw(s.b, s.currencyBId, s.currencyB)) {
            Msg.error(b, "잔액 부족.");
            if (s.currencyA > 0) plugin.currencies().deposit(s.a, s.currencyAId, s.currencyA);
            return;
        }
        if (s.currencyA > 0) plugin.currencies().deposit(s.b, s.currencyAId, s.currencyA);
        if (s.currencyB > 0) plugin.currencies().deposit(s.a, s.currencyBId, s.currencyB);
        if (s.itemA != null) b.getInventory().addItem(s.itemA);
        if (s.itemB != null) a.getInventory().addItem(s.itemB);

        Bukkit.getPluginManager().callEvent(new RebornTradeCompleteEvent(a, b, s));
        Msg.send(a, "&a거래 완료.");
        Msg.send(b, "&a거래 완료.");
    }

    private void cancelSession(Session s) {
        sessions.remove(s.a);
        sessions.remove(s.b);
    }
}
