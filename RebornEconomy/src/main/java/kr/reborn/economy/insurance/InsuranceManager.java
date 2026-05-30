package kr.reborn.economy.insurance;

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
 * 보험 매니저.
 *
 * 플레이어가 일정 금액 보험료를 납부하면 사망 시 보상.
 * - 월 단위 보험료 (게임 30일 = 30분)
 * - 보장: 사망 시 보장 금액 자동 지급
 * - 등급별 차등: BASIC (월 100g, 보장 1000g), PREMIUM (1000g, 보장 20000g), VIP (10000g, 보장 500000g)
 * - 미납 시 자동 해지
 */
public final class InsuranceManager {

    public enum Grade {
        BASIC(100, 1000),
        PREMIUM(1000, 20000),
        VIP(10000, 500000);
        public final long monthlyPremium;
        public final long coverage;
        Grade(long pre, long cov) { this.monthlyPremium = pre; this.coverage = cov; }
    }

    public static final class Policy {
        public final UUID owner;
        public Grade grade;
        public long subscribedAt;
        public long lastPaidAt;
        public int monthsPaid;
        public int payouts;

        public Policy(UUID owner, Grade grade) {
            this.owner = owner;
            this.grade = grade;
            this.subscribedAt = System.currentTimeMillis();
            this.lastPaidAt = subscribedAt;
        }

        /** 활성 = 1달 이내 납부 (30분 간격). */
        public boolean active() {
            return System.currentTimeMillis() - lastPaidAt < 30 * 60_000L;
        }
    }

    private final RebornEconomy plugin;
    private final Map<UUID, Policy> policies = new ConcurrentHashMap<>();

    public InsuranceManager(RebornEconomy plugin) {
        this.plugin = plugin;
        // 매 30분 = 1달, 보험료 자동 징수
        RebornCore.get().scheduler().runTimerAsync(this::collectPremiums, 36_000L, 36_000L);
    }

    public boolean subscribe(Player p, Grade grade) {
        if (policies.containsKey(p.getUniqueId())) {
            Msg.error(p, "이미 보험 가입 — 먼저 해지하세요.");
            return false;
        }
        // 첫달 보험료 즉시 납부
        if (!plugin.currencies().withdraw(p.getUniqueId(), "GOLD_COIN", grade.monthlyPremium)) {
            Msg.error(p, "보험료 부족 (" + grade.monthlyPremium + " GOLD)");
            return false;
        }
        Policy pol = new Policy(p.getUniqueId(), grade);
        pol.monthsPaid = 1;
        policies.put(p.getUniqueId(), pol);
        Msg.send(p, "&a보험 가입: " + grade.name() + " §7월 " + grade.monthlyPremium
                + " GOLD, 보장 " + grade.coverage + " GOLD");
        return true;
    }

    public boolean cancel(Player p) {
        Policy pol = policies.remove(p.getUniqueId());
        if (pol == null) { Msg.warn(p, "가입 안 됨."); return false; }
        Msg.send(p, "&7보험 해지: 누적 납부 " + pol.monthsPaid + "개월.");
        return true;
    }

    public Policy of(UUID p) { return policies.get(p); }

    /** 매 30분 호출 — 모든 활성 보험의 보험료 자동 인출. */
    public void collectPremiums() {
        long now = System.currentTimeMillis();
        for (var entry : policies.entrySet()) {
            Policy pol = entry.getValue();
            if (now - pol.lastPaidAt < 30 * 60_000L) continue;
            // 자동 인출 시도
            boolean ok = plugin.currencies().withdraw(pol.owner, "GOLD_COIN", pol.grade.monthlyPremium);
            Player p = Bukkit.getPlayer(pol.owner);
            if (!ok) {
                // 자동 해지
                policies.remove(entry.getKey());
                if (p != null) Msg.warn(p, "&c보험 자동 해지 — 보험료 부족 (" + pol.grade.monthlyPremium + "g)");
                continue;
            }
            pol.lastPaidAt = now;
            pol.monthsPaid++;
            if (p != null) Msg.send(p, "&7보험료 자동 납부: " + pol.grade.monthlyPremium + "g");
        }
    }

    /** 사망 시 자동 지급 (DeathListener에서 호출). */
    public boolean payoutOnDeath(Player p) {
        Policy pol = policies.get(p.getUniqueId());
        if (pol == null) return false;
        if (!pol.active()) {
            Msg.warn(p, "보험 미납 — 보장 미적용.");
            return false;
        }
        pol.payouts++;
        plugin.currencies().deposit(p.getUniqueId(), "GOLD_COIN", pol.grade.coverage);
        Msg.send(p, "&6&l[보험 지급] §f사망 보장 §6" + pol.grade.coverage + " GOLD §7지급.");
        return true;
    }

    public Map<UUID, Policy> all() { return policies; }
}
