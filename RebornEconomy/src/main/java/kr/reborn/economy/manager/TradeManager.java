package kr.reborn.economy.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.event.RebornTradeCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /** 양쪽 키로 동일한 세션 가리킴. Folia 멀티스레드 대비 동시성 맵. */
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

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
        // 제안한 아이템 복귀
        if (a != null && s.itemA != null) a.getInventory().addItem(s.itemA);
        if (b != null && s.itemB != null) b.getInventory().addItem(s.itemB);
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

    /** 손에 든 아이템 제안. 직전 제안이 있으면 인벤토리로 복귀. */
    public void offerItem(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null) { Msg.error(p, "진행 중인 거래가 없습니다."); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
            Msg.error(p, "손에 아이템이 없습니다."); return;
        }
        boolean isA = p.getUniqueId().equals(s.a);
        // 직전 제안 복귀
        ItemStack prev = isA ? s.itemA : s.itemB;
        if (prev != null) p.getInventory().addItem(prev);
        // 손에 든 아이템 제거 후 제안 set — 누적·복제 방지
        ItemStack offered = hand.clone();
        p.getInventory().setItemInMainHand(null);
        if (isA) s.itemA = offered; else s.itemB = offered;
        // ready 상태 리셋 (제안 변경 후 다시 확인 필요)
        s.readyA = false; s.readyB = false;
        Msg.send(p, "&a제안 갱신: &f" + offered.getType() + " ×" + offered.getAmount()
                + " &7(상대방 다시 /trade ready 필요)");
        Player other = Bukkit.getPlayer(isA ? s.b : s.a);
        if (other != null) Msg.send(other, "&7상대가 아이템을 제안했다: " + offered.getType());
    }

    /** 화폐 제안. 직전 제안이 있으면 환불. 차감은 finalize에서. */
    public void offerCurrency(Player p, String currency, long amount) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null) { Msg.error(p, "진행 중인 거래가 없습니다."); return; }
        if (amount < 0) { Msg.error(p, "금액은 0 이상"); return; }
        if (amount > 0 && !plugin.currencies().has(p.getUniqueId(), currency, amount)) {
            Msg.error(p, "잔액 부족"); return;
        }
        boolean isA = p.getUniqueId().equals(s.a);
        if (isA) { s.currencyA = amount; s.currencyAId = currency; }
        else     { s.currencyB = amount; s.currencyBId = currency; }
        s.readyA = false; s.readyB = false;
        Msg.send(p, "&a화폐 제안: &f" + amount + " " + currency
                + " &7(상대방 다시 /trade ready 필요)");
        Player other = Bukkit.getPlayer(isA ? s.b : s.a);
        if (other != null) Msg.send(other, "&7상대가 " + amount + " " + currency + " 제안.");
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
        if (a == null || b == null) {
            // 한쪽이 오프라인 — 우편함으로 제안 아이템 반환 (분실 방지)
            if (s.itemA != null && a != null) a.getInventory().addItem(s.itemA);
            else if (s.itemA != null) plugin.mailbox().enqueue(new kr.reborn.economy.data.MailItem(
                    java.util.UUID.randomUUID(), s.a, "거래 취소(상대 오프라인)", s.itemA, null, 0));
            if (s.itemB != null && b != null) b.getInventory().addItem(s.itemB);
            else if (s.itemB != null) plugin.mailbox().enqueue(new kr.reborn.economy.data.MailItem(
                    java.util.UUID.randomUUID(), s.b, "거래 취소(상대 오프라인)", s.itemB, null, 0));
            return;
        }

        // 아포칼립스 = 물물교환만
        boolean apocBarter = plugin.getConfig().getBoolean("trade.apocalypse-barter-only", true);
        WorldKey wA = RebornCore.get().api().getCurrentWorld(s.a);
        WorldKey wB = RebornCore.get().api().getCurrentWorld(s.b);
        if (apocBarter && (wA == WorldKey.APOCALYPSE || wB == WorldKey.APOCALYPSE)) {
            if (s.currencyA > 0 || s.currencyB > 0) {
                Msg.error(a, "아포칼립스에서는 물물교환만 가능합니다.");
                Msg.error(b, "아포칼립스에서는 물물교환만 가능합니다.");
                returnItems(a, b, s);
                return;
            }
        }

        // 화폐 차감 / 입금
        if (s.currencyA > 0 && !plugin.currencies().withdraw(s.a, s.currencyAId, s.currencyA)) {
            Msg.error(a, "잔액 부족.");
            returnItems(a, b, s);
            return;
        }
        if (s.currencyB > 0 && !plugin.currencies().withdraw(s.b, s.currencyBId, s.currencyB)) {
            Msg.error(b, "잔액 부족.");
            if (s.currencyA > 0) plugin.currencies().deposit(s.a, s.currencyAId, s.currencyA);
            returnItems(a, b, s);
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

    /** 거래 실패 시 제안 아이템 원주인에게 반환. */
    private void returnItems(Player a, Player b, Session s) {
        if (s.itemA != null && a != null) a.getInventory().addItem(s.itemA);
        if (s.itemB != null && b != null) b.getInventory().addItem(s.itemB);
    }

    private void cancelSession(Session s) {
        sessions.remove(s.a);
        sessions.remove(s.b);
    }
}
