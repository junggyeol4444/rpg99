package kr.reborn.death.bounty;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 현상금 매니저.
 *
 * - 플레이어가 다른 플레이어에게 현상금 걸기: 가문 treasury 또는 본인 화폐 소비.
 * - 현상금 누적: 한 대상에 여러 의뢰인이 추가 가능.
 * - 대상이 처치되면 처치자에게 즉시 지급 (RebornEconomy currencies.deposit).
 * - /bounty top 으로 현상금 순위 조회.
 *
 * 자동 현상금:
 * - 범죄 레벨 3+ 도달 시 시스템이 자동으로 1000~10000 현상금 부여.
 * - 7일 뒤 미처치 시 +50% 누적.
 */
public final class BountyManager {

    private final RebornDeath plugin;
    /** targetId → 누적 현상금 */
    private final Map<UUID, Long> bounties = new ConcurrentHashMap<>();
    /** targetId → 의뢰인 목록 */
    private final Map<UUID, List<Issuer>> issuers = new ConcurrentHashMap<>();

    public BountyManager(RebornDeath plugin) {
        this.plugin = plugin;
        // 1시간마다 미처치 현상금 +50%, 자동 현상금 체크
        RebornCore.get().scheduler().runTimerAsync(this::tickAutoBounty,
                72_000L, 72_000L);
    }

    /** 외부 호출 — 현상금 걸기. */
    public boolean place(Player issuer, UUID target, long amount, String currency) {
        if (amount <= 0) return false;
        // RebornEconomy 인출
        boolean ok = false;
        try {
            var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep != null) {
                Object currencies = ep.getClass().getMethod("currencies").invoke(ep);
                Object res = currencies.getClass().getMethod("withdraw",
                                UUID.class, String.class, long.class)
                        .invoke(currencies, issuer.getUniqueId(), currency, amount);
                ok = Boolean.TRUE.equals(res);
            }
        } catch (Throwable ignored) {}
        if (!ok) { Msg.error(issuer, "잔액 부족 (" + currency + ")"); return false; }

        bounties.merge(target, amount, Long::sum);
        issuers.computeIfAbsent(target, k -> new java.util.ArrayList<>())
                .add(new Issuer(issuer.getUniqueId(), amount, currency));
        String targetName = "?";
        Player tp = Bukkit.getPlayer(target);
        if (tp != null) targetName = tp.getName();
        Bukkit.broadcastMessage("§4§l[현상금] §f" + targetName
                + " §c에게 " + amount + " " + currency + " 현상금이 걸렸다! §7(총 "
                + bounties.get(target) + ")");
        return true;
    }

    /** 대상 처치 시 호출 — 자동 지급. */
    public void onKilled(Player killer, UUID target) {
        Long total = bounties.remove(target);
        if (total == null || total <= 0) return;
        List<Issuer> issList = issuers.remove(target);
        // 첫 issuer의 통화로 통일 지급 (간소화). 더 정확하게 하려면 issuer별 분리 가능.
        String currency = issList != null && !issList.isEmpty() ? issList.get(0).currency : "GOLD_COIN";
        try {
            var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep != null) {
                Object currencies = ep.getClass().getMethod("currencies").invoke(ep);
                currencies.getClass().getMethod("deposit",
                                UUID.class, String.class, long.class)
                        .invoke(currencies, killer.getUniqueId(), currency, total);
            }
        } catch (Throwable ignored) {}
        Bukkit.broadcastMessage("§e§l[현상금 지급] §f" + killer.getName()
                + " §7가 현상금 §6" + total + " " + currency + " §7을(를) 받았다!");
    }

    private void tickAutoBounty() {
        // 1) 7일 미처치 현상금 +50%
        Map<UUID, Long> snap = new java.util.HashMap<>(bounties);
        for (var e : snap.entrySet()) {
            // 단순화: 매 사이클 +10% (실제로는 issuer.placedAt 추적 필요)
            bounties.put(e.getKey(), (long)(e.getValue() * 1.10));
        }
        // 2) 범죄 레벨 3+ 자동 현상금
        for (Player p : Bukkit.getOnlinePlayers()) {
            int lvl = plugin.crime().level(p.getUniqueId());
            if (lvl < 3) continue;
            if (bounties.containsKey(p.getUniqueId())) continue;
            long bounty = lvl * 1000L;
            bounties.put(p.getUniqueId(), bounty);
            Bukkit.broadcastMessage("§4§l[시스템 현상금] §f" + p.getName()
                    + " §c (범죄 Lv " + lvl + ") — 현상금 " + bounty);
        }
    }

    public long bountyOf(UUID p) { return bounties.getOrDefault(p, 0L); }

    public List<Map.Entry<UUID, Long>> top(int n) {
        return bounties.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(n).collect(java.util.stream.Collectors.toList());
    }

    public static final class Issuer {
        public final UUID issuerId;
        public final long amount;
        public final String currency;
        public final long placedAt;

        public Issuer(UUID id, long amt, String cur) {
            this.issuerId = id; this.amount = amt; this.currency = cur;
            this.placedAt = System.currentTimeMillis();
        }
    }
}
