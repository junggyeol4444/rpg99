package kr.reborn.economy.bank;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 은행 매니저.
 *
 * 예금/인출/대출/상환 + 일 단위 이자 정산.
 *
 * 이자율(config):
 *   deposit-rate-daily: 0.001 (0.1%)
 *   loan-rate-daily: 0.003 (0.3%)
 *   maturity-bonus-7d: 0.005 (만기 7일 보너스 +0.5%)
 *   maturity-bonus-30d: 0.025
 *   maturity-bonus-90d: 0.10
 *
 * 신용 점수: 70 시작. 대출 미상환 = -10/주. 상환 완료 = +5.
 *
 * 1분마다 일 단위 이자 계산 (실제로는 1일 게임시간 = 24분 등 설정 가능).
 */
public final class BankManager {

    private final RebornEconomy plugin;
    /** uuid → 통화ID → 계좌 */
    private final Map<UUID, Map<String, BankAccount>> accounts = new ConcurrentHashMap<>();

    public BankManager(RebornEconomy plugin) {
        this.plugin = plugin;
        // 1분마다 이자 정산
        RebornCore.get().scheduler().runTimerAsync(this::tickInterest, 1200L, 1200L);
    }

    public BankAccount open(Player p, String currency) {
        Map<String, BankAccount> map = accounts.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        BankAccount existing = map.get(currency);
        if (existing != null) return existing;
        BankAccount a = new BankAccount(p.getUniqueId(), currency);
        map.put(currency, a);
        Msg.send(p, "&a은행 계좌 개설: " + currency + " §7신용 70");
        return a;
    }

    public BankAccount get(UUID p, String currency) {
        Map<String, BankAccount> map = accounts.get(p);
        return map == null ? null : map.get(currency);
    }

    public boolean deposit(Player p, String currency, long amount) {
        if (amount <= 0) return false;
        BankAccount a = open(p, currency);
        if (!plugin.currencies().withdraw(p.getUniqueId(), currency, amount)) {
            Msg.error(p, "잔액 부족 (지갑).");
            return false;
        }
        a.deposit += amount;
        Msg.send(p, "&a예금 +" + amount + " §7(총 " + a.deposit + ")");
        return true;
    }

    public boolean withdraw(Player p, String currency, long amount) {
        BankAccount a = get(p.getUniqueId(), currency);
        if (a == null) { Msg.error(p, "계좌 없음."); return false; }
        if (a.deposit < amount) { Msg.error(p, "예금 부족."); return false; }
        if (a.maturityAt > 0 && System.currentTimeMillis() < a.maturityAt) {
            // 만기 전 인출 — 이자 패널티 5%
            long penalty = amount / 20;
            amount -= penalty;
            Msg.warn(p, "&7만기 전 인출 — 5% 패널티 -" + penalty);
        }
        a.deposit -= amount;
        plugin.currencies().deposit(p.getUniqueId(), currency, amount);
        Msg.send(p, "&a인출 " + amount + " §7(남은 " + a.deposit + ")");
        return true;
    }

    public boolean setMaturity(Player p, String currency, int days) {
        BankAccount a = get(p.getUniqueId(), currency);
        if (a == null) { Msg.error(p, "계좌 없음."); return false; }
        if (days != 7 && days != 30 && days != 90) {
            Msg.error(p, "만기는 7/30/90일만 가능.");
            return false;
        }
        a.maturityAt = System.currentTimeMillis() + days * 86_400_000L;
        Msg.send(p, "&6정기예금 설정: " + days + "일 만기 보너스");
        return true;
    }

    public boolean takeLoan(Player p, String currency, long amount) {
        BankAccount a = open(p, currency);
        long maxLoan = Math.max(1000, (a.deposit + 1000) * 3L * a.credit / 100);
        if (amount > maxLoan) {
            Msg.error(p, "대출 한도 초과 — 최대 " + maxLoan + " (신용 " + a.credit + ")");
            return false;
        }
        a.loan += amount;
        plugin.currencies().deposit(p.getUniqueId(), currency, amount);
        Msg.send(p, "&6대출 +" + amount + " §7(총 대출 " + a.loan + ")");
        return true;
    }

    public boolean repay(Player p, String currency, long amount) {
        BankAccount a = get(p.getUniqueId(), currency);
        if (a == null || a.loan == 0) { Msg.error(p, "대출 없음."); return false; }
        if (a.loan < amount) amount = a.loan;
        if (!plugin.currencies().withdraw(p.getUniqueId(), currency, amount)) {
            Msg.error(p, "잔액 부족 (지갑).");
            return false;
        }
        a.loan -= amount;
        if (a.loan == 0) {
            a.credit = Math.min(100, a.credit + 5);
            Msg.send(p, "&a대출 완납 — 신용 +5 (" + a.credit + ")");
        } else {
            Msg.send(p, "&a상환 -" + amount + " §7(남은 대출 " + a.loan + ")");
        }
        return true;
    }

    /** 이자 정산 — 매 1분 = 1일 모델. */
    public void tickInterest() {
        long now = System.currentTimeMillis();
        double depositRate = plugin.getConfig().getDouble("bank.deposit-rate-daily", 0.001);
        double loanRate = plugin.getConfig().getDouble("bank.loan-rate-daily", 0.003);
        for (Map<String, BankAccount> map : accounts.values()) {
            for (BankAccount a : map.values()) {
                if (now - a.lastInterestAt < 60_000L) continue; // 1분 = 1일
                a.lastInterestAt = now;
                // 예금 이자
                if (a.deposit > 0) {
                    double rate = depositRate;
                    if (a.maturityAt > now) {
                        long left = a.maturityAt - now;
                        if (left > 7 * 86_400_000L) rate += plugin.getConfig()
                                .getDouble("bank.maturity-bonus-30d", 0.025);
                        else rate += plugin.getConfig()
                                .getDouble("bank.maturity-bonus-7d", 0.005);
                    }
                    a.deposit += (long)(a.deposit * rate);
                }
                // 대출 이자
                if (a.loan > 0) {
                    a.loan += (long)(a.loan * loanRate);
                    // 7일 미상환마다 신용 -10
                    if (now - a.openedAt > 7 * 86_400_000L) {
                        a.credit = Math.max(0, a.credit - 1);
                        if (a.credit == 0) {
                            Player p = Bukkit.getPlayer(a.owner);
                            if (p != null) Msg.warn(p, "&c신용 0 — 새 대출·예금 거부.");
                        }
                    }
                }
            }
        }
    }

    public Map<String, BankAccount> accountsOf(UUID p) {
        return accounts.getOrDefault(p, java.util.Collections.emptyMap());
    }
}
