package kr.reborn.worldai.market;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.worldai.RebornWorldAI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 시장 시뮬레이터 — 세계·아이템 카테고리별 공급·수요·가격을 추적.
 *
 * 카테고리:
 *   FOOD, METAL, MAGIC, WEAPON, ARMOR, RARE, INFO (정보)
 *
 * 매 사이클:
 *   1. 공급량 변동 — 풍년/흉년, 전쟁시 -50%
 *   2. 수요 — 인플레이션·세금 인상 시 +
 *   3. 가격 = base × (수요 / 공급) (±20% 노이즈)
 *   4. 가격 변동이 크면 RebornEconomy에 reflection으로 가격 갱신 요청
 *   5. 부족·과잉이 극단 → AI에 ECON_CRISIS 이벤트 전달
 */
public final class MarketSimulator {

    public enum Category {
        FOOD, METAL, MAGIC, WEAPON, ARMOR, RARE, INFO, MEDICINE
    }

    private final RebornWorldAI plugin;
    /** worldKey → category → marketState */
    private final Map<WorldKey, Map<Category, MarketState>> markets = new EnumMap<>(WorldKey.class);

    public MarketSimulator(RebornWorldAI plugin) {
        this.plugin = plugin;
        seed();
    }

    private void seed() {
        for (WorldKey w : WorldKey.values()) {
            Map<Category, MarketState> per = new HashMap<>();
            for (Category c : Category.values()) {
                per.put(c, new MarketState(100, 100, basePriceFor(c)));
            }
            markets.put(w, per);
        }
    }

    /** 카테고리별 기본가 (실버 단위). */
    private double basePriceFor(Category c) {
        switch (c) {
            case FOOD: return 10;
            case METAL: return 100;
            case MAGIC: return 500;
            case WEAPON: return 300;
            case ARMOR: return 250;
            case RARE: return 5000;
            case INFO: return 200;
            case MEDICINE: return 50;
        }
        return 100;
    }

    public void cycle(WorldKey world, double tension, double inflation) {
        Map<Category, MarketState> per = markets.get(world);
        if (per == null) return;

        boolean wartime = tension > 70;
        for (var e : per.entrySet()) {
            Category cat = e.getKey();
            MarketState st = e.getValue();
            // 공급 — 전쟁시 -30%, 평시 ±5%
            double supplyDelta = wartime ? -30 : Rand.rangeD(-5, 5);
            st.supply = clamp(10, 1000, st.supply + supplyDelta);
            // 수요 — 인플레이션 ÷ 50 만큼 가산 (인플레가 200이면 +4)
            double demandDelta = (inflation - 100) / 50.0 + Rand.rangeD(-3, 3);
            st.demand = clamp(10, 1000, st.demand + demandDelta);
            // 가격 갱신
            double ratio = st.demand / Math.max(1, st.supply);
            double newPrice = basePriceFor(cat) * ratio * (0.8 + Math.random() * 0.4);
            // 5% 이상 변동 시 RebornEconomy에 reflection 갱신
            double diff = Math.abs(newPrice - st.price) / Math.max(1, st.price);
            st.price = newPrice;
            if (diff > 0.05) {
                pushPriceToEconomy(world, cat, newPrice);
            }
            // 극단 부족·과잉
            if (st.supply < 30 || st.supply > 800) {
                announce(world, cat, st);
            }
        }
    }

    private void pushPriceToEconomy(WorldKey w, Category c, double newPrice) {
        try {
            Plugin ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep == null) return;
            // RebornEconomy.priceController().updateCategoryPrice(world, category, price)
            Object pc = ep.getClass().getMethod("priceController").invoke(ep);
            if (pc != null) {
                pc.getClass().getMethod("updateCategoryPrice",
                                WorldKey.class, String.class, double.class)
                        .invoke(pc, w, c.name(), newPrice);
            }
        } catch (Throwable ignored) {}
    }

    private void announce(WorldKey w, Category c, MarketState st) {
        String label = st.supply < 30 ? "§c극심한 부족" : "§e과잉 공급";
        Bukkit.broadcastMessage("§6[" + w + " 시장] " + label
                + " §7- " + categoryKo(c)
                + " §f| 가격 §6" + (int) st.price + "실버");
    }

    private String categoryKo(Category c) {
        switch (c) {
            case FOOD: return "식량";
            case METAL: return "금속";
            case MAGIC: return "마법재료";
            case WEAPON: return "무기";
            case ARMOR: return "방어구";
            case RARE: return "희귀품";
            case INFO: return "정보";
            case MEDICINE: return "약품";
        }
        return c.name();
    }

    public Map<Category, MarketState> of(WorldKey w) {
        return markets.getOrDefault(w, java.util.Collections.emptyMap());
    }

    private double clamp(double lo, double hi, double v) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static final class MarketState {
        public double supply;
        public double demand;
        public double price;

        public MarketState(double s, double d, double p) {
            this.supply = s; this.demand = d; this.price = p;
        }
    }
}
