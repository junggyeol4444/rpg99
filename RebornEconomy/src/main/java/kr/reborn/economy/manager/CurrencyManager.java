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

    private final RebornEconomy plugin;
    private final Map<String, Currency> currencies = new HashMap<>();
    /** uuid -> currencyId -> amount */
    private final Map<UUID, Map<String, Long>> balances = new ConcurrentHashMap<>();

    public CurrencyManager(RebornEconomy plugin) {
        this.plugin = plugin;
        load();
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
        return balances.getOrDefault(player, Map.of()).getOrDefault(currency, 0L);
    }

    public Map<String, Long> all(UUID player) {
        return balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
    }

    public boolean has(UUID player, String currency, long amount) {
        return balance(player, currency) >= amount;
    }

    public void deposit(UUID player, String currency, long amount) {
        if (amount <= 0) return;
        balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .merge(currency, amount, Long::sum);
    }

    /** @return true 차감 성공. */
    public boolean withdraw(UUID player, String currency, long amount) {
        if (amount <= 0) return true;
        Map<String, Long> map = balances.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        Long bal = map.getOrDefault(currency, 0L);
        if (bal < amount) return false;
        map.put(currency, bal - amount);
        return true;
    }

    public void flush() {
        // TODO: persist to DB via RebornCore.database()
    }
}
