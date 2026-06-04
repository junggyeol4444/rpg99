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
        // 기획서 15장: "환율은 세계 AI가 경제 상황에 따라 실시간 조정"
        // 매 5분(6000 ticks)마다 WorldAI inflation 기반 동적 환율 조정
        RebornCore.get().scheduler().runTimerAsync(this::tickDynamicRates, 6000L, 6000L);
    }

    /** WorldAI inflation 기반 환율 자동 변동. RebornWorldAI 리플렉션. */
    private void tickDynamicRates() {
        try {
            var ai = Bukkit.getPluginManager().getPlugin("RebornWorldAI");
            if (ai == null) return;
            Object all = ai.getClass().getMethod("all").invoke(ai);
            if (!(all instanceof java.util.Collection<?> col)) return;
            // 세계별 inflation 수집 (currency → inflation)
            Map<kr.reborn.core.data.WorldKey, Double> inflations = new HashMap<>();
            for (Object worldAi : col) {
                try {
                    Object world = worldAi.getClass().getMethod("world").invoke(worldAi);
                    Object state = worldAi.getClass().getMethod("state").invoke(worldAi);
                    double inflation = (double) state.getClass().getField("inflation").get(state);
                    if (world instanceof kr.reborn.core.data.WorldKey wk) {
                        inflations.put(wk, inflation);
                    }
                } catch (Throwable ignored) {}
            }
            // 환율 = baseRate × (toInflation / fromInflation) — 인플레이션 높은 통화가 약세
            for (var fromEntry : new HashMap<>(rates).entrySet()) {
                String from = fromEntry.getKey();
                var fromCur = plugin.currencies().get(from);
                if (fromCur == null) continue;
                Double fromInfl = inflations.get(fromCur.world);
                if (fromInfl == null || fromInfl <= 0) continue;
                for (var toEntry : new HashMap<>(fromEntry.getValue()).entrySet()) {
                    String to = toEntry.getKey();
                    var toCur = plugin.currencies().get(to);
                    if (toCur == null) continue;
                    Double toInfl = inflations.get(toCur.world);
                    if (toInfl == null || toInfl <= 0) continue;
                    double multiplier = fromInfl / toInfl;
                    adjustRate(from, to, multiplier);
                }
            }
        } catch (Throwable ignored) {}
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
