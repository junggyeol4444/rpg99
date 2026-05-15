package kr.reborn.economy.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.event.RebornExchangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 환전. 자동 아님. /exchange 명령어로 수동. */
public final class ExchangeManager {

    private final RebornEconomy plugin;
    private final Map<String, Map<String, Double>> rates = new HashMap<>();
    private double feePercent;
    private double fluctuation;

    public ExchangeManager(RebornEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        rates.clear();
        feePercent = plugin.getConfig().getDouble("exchange-fee-percent", 5.0);
        fluctuation = plugin.getConfig().getDouble("exchange-fluctuation-percent", 30.0);
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("exchange-rates");
        if (sec == null) return;
        for (String from : sec.getKeys(false)) {
            ConfigurationSection inner = sec.getConfigurationSection(from);
            if (inner == null) continue;
            Map<String, Double> map = new HashMap<>();
            for (String to : inner.getKeys(false)) {
                map.put(to, inner.getDouble(to));
            }
            rates.put(from, map);
        }
    }

    /** 환전 1단위 시 받는 양. RebornWorldAI hook이 fluctuation 범위 내 조정 가능. */
    public double rate(String from, String to) {
        Map<String, Double> m = rates.get(from);
        if (m == null) return 0;
        Double r = m.get(to);
        return r == null ? 0 : r;
    }

    /**
     * @return 변경 결과 메시지를 위해 받은 양. 0이면 실패.
     */
    public long exchange(UUID player, String from, String to, long amount, boolean feeExempt) {
        double r = rate(from, to);
        if (r <= 0 || amount <= 0) return 0;
        CurrencyManager cm = plugin.currencies();
        if (!cm.has(player, from, amount)) return 0;
        long received = (long) Math.floor(amount * r);
        if (received <= 0) return 0;
        long fee = feeExempt ? 0 : (long) Math.floor(received * feePercent / 100.0);
        long net = received - fee;
        if (!cm.withdraw(player, from, amount)) return 0;
        cm.deposit(player, to, net);

        Player p = Bukkit.getPlayer(player);
        if (p != null) {
            Bukkit.getPluginManager().callEvent(new RebornExchangeEvent(p, from, to, amount, net, fee));
            Msg.send(p, "&a환전 완료: &f" + amount + " " + from + " &7→ &f" + net + " " + to
                    + " &8(수수료 " + fee + ")");
        }
        return net;
    }

    /** 차원상인 히든 클래스 보유자 = 면제. RebornHiddenClass reflection. */
    public boolean isFeeExempt(UUID player) {
        try {
            var hcPlugin = Bukkit.getPluginManager().getPlugin("RebornHiddenClass");
            if (hcPlugin == null) return false;
            Object progress = hcPlugin.getClass().getMethod("progress").invoke(hcPlugin);
            Object hasMethod = progress.getClass().getMethod("has", UUID.class, String.class)
                    .invoke(progress, player, "dimensional_merchant");
            return hasMethod instanceof Boolean && (Boolean) hasMethod;
        } catch (Throwable e) {
            return false;
        }
    }

    /** 동적 환율 변동: WorldAI hook. */
    public void adjustRate(String from, String to, double multiplier) {
        Map<String, Double> m = rates.computeIfAbsent(from, k -> new HashMap<>());
        Double base = m.get(to);
        if (base == null) return;
        double cap = base * (1 + fluctuation / 100.0);
        double floor = base * (1 - fluctuation / 100.0);
        double next = Math.max(floor, Math.min(cap, base * multiplier));
        m.put(to, next);
        if (RebornCore.get() != null) RebornCore.get().getLogger().fine("환율 조정: " + from + "→" + to + "=" + next);
    }
}
