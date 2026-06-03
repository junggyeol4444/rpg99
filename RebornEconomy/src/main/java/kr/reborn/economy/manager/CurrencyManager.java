package kr.reborn.economy.manager;

import kr.reborn.core.data.WorldKey;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.data.Currency;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 통화 정의 + 플레이어별 잔액 관리.
 * 메모리 캐시 기반. 영구화는 RebornCore DB hook을 향후 추가.
 */
public final class CurrencyManager {

    private static final String NS = "RebornEconomy.currency";

    private final RebornEconomy plugin;
    private final Map<String, Currency> currencies = new HashMap<>();
    /** uuid -> currencyId -> amount */
    private final Map<UUID, Map<String, Long>> balances = new ConcurrentHashMap<>();
    /** 변경된 (uuid, currency) 추적 — flush 시 일괄 저장 */
    private final java.util.Set<String> dirty = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public CurrencyManager(RebornEconomy plugin) {
        this.plugin = plugin;
        load();
        // 1분마다 dirty 항목 자동 저장
        kr.reborn.core.RebornCore.get().scheduler().runTimerAsync(this::flush, 1200L, 1200L);
    }

    private void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("currencies");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(id);
            if (c == null) continue;
            String name = c.getString("name", id);
            WorldKey w;
            try { w = WorldKey.valueOf(c.getString("world", "LOBBY")); }
            catch (Exception e) { w = WorldKey.LOBBY; }
            Material m = Material.matchMaterial(c.getString("icon", "GOLD_INGOT"));
            if (m == null) m = Material.GOLD_INGOT;
            int model = c.getInt("model", 0);
            currencies.put(id, new Currency(id, name, w, m, model));
        }
    }

    public Currency get(String id) { return currencies.get(id); }

    public Collection<Currency> all() { return currencies.values(); }

    public long balance(UUID player, String currency) {
        // 캐시에 없으면 DB에서 로드
        Map<String, Long> map = balances.get(player);
        if (map == null || !map.containsKey(currency)) {
            long stored = kr.reborn.core.RebornCore.get().kv().getLong(NS, player, currency, 0);
            if (stored > 0) {
                balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                        .put(currency, stored);
                return stored;
            }
            return 0;
        }
        return map.getOrDefault(currency, 0L);
    }

    public Map<String, Long> all(UUID player) {
        return balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
    }

    public boolean has(UUID player, String currency, long amount) {
        return balance(player, currency) >= amount;
    }

    public void deposit(UUID player, String currency, long amount) {
        if (amount <= 0) return;
        // DB에서 캐시 보장 (있어도 무해)
        balance(player, currency);
        // 원자적 가산 — 동시 deposit/withdraw 안전
        balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .merge(currency, amount, Long::sum);
        dirty.add(player.toString() + "|" + currency);
    }

    /** @return true 차감 성공. 동시 호출 안전 (compute로 원자성 보장). */
    public boolean withdraw(UUID player, String currency, long amount) {
        if (amount <= 0) return true;
        balance(player, currency);  // DB 로드 보장
        Map<String, Long> map = balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        boolean[] ok = { false };
        map.compute(currency, (k, cur) -> {
            long c = cur == null ? 0 : cur;
            if (c < amount) { ok[0] = false; return c; }
            ok[0] = true;
            return c - amount;
        });
        if (ok[0]) dirty.add(player.toString() + "|" + currency);
        return ok[0];
    }

    /** 변경된 (uuid, currency) 항목만 KV에 저장. */
    public void flush() {
        if (dirty.isEmpty()) return;
        var snapshot = new java.util.HashSet<>(dirty);
        dirty.clear();
        for (String key : snapshot) {
            String[] parts = key.split("\\|", 2);
            if (parts.length != 2) continue;
            try {
                UUID uuid = UUID.fromString(parts[0]);
                long bal = balances.getOrDefault(uuid, Map.of())
                        .getOrDefault(parts[1], 0L);
                kr.reborn.core.RebornCore.get().kv().putLong(NS, uuid, parts[1], bal);
            } catch (Throwable ignored) {}
        }
    }
}
