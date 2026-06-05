package kr.reborn.economy.price;

import kr.reborn.core.data.WorldKey;
import kr.reborn.economy.RebornEconomy;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 가격 컨트롤러 — RebornWorldAI.MarketSimulator의 가격 변동을 받아 저장.
 *
 * ShopManager.buy/sell이 priceMultiplier(world, material)을 곱해 최종 가격 결정.
 * 기본가 100을 기준으로 변동 가격을 비율로 환산.
 *
 * 외부에서 reflection으로 호출:
 *   plugin.priceController().updateCategoryPrice(WorldKey, String, double)
 */
public final class PriceController {

    /** 기본 카테고리 가격 (MarketSimulator basePriceFor와 동일). */
    private static final Map<String, Double> BASE_PRICE = new HashMap<>();
    static {
        BASE_PRICE.put("FOOD", 10.0);
        BASE_PRICE.put("METAL", 100.0);
        BASE_PRICE.put("MAGIC", 500.0);
        BASE_PRICE.put("WEAPON", 300.0);
        BASE_PRICE.put("ARMOR", 250.0);
        BASE_PRICE.put("RARE", 5000.0);
        BASE_PRICE.put("INFO", 200.0);
        BASE_PRICE.put("MEDICINE", 50.0);
    }

    private static final String NS = "RebornEconomy.price";

    private final RebornEconomy plugin;
    /** world → category → multiplier (1.0 = 평시) */
    private final Map<WorldKey, Map<String, Double>> mult = new EnumMap<>(WorldKey.class);

    public PriceController(RebornEconomy plugin) {
        this.plugin = plugin;
        for (WorldKey w : WorldKey.values()) {
            mult.put(w, new HashMap<>());
        }
        // KV에서 모든 multiplier 로드 — global (owner="" 빈문자열). key = "WORLD:CATEGORY"
        try {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            for (var e : all.entrySet()) {
                String[] parts = e.getKey().split(":", 2);
                if (parts.length != 2) continue;
                try {
                    WorldKey w = WorldKey.valueOf(parts[0]);
                    mult.get(w).put(parts[1], Double.parseDouble(e.getValue()));
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    /** MarketSimulator → PriceController 직접 호출(리플렉션 타깃). */
    public void updateCategoryPrice(WorldKey world, String category, double newPrice) {
        Double base = BASE_PRICE.get(category);
        if (base == null || base <= 0) return;
        double m = newPrice / base;
        // 극단값 클램프: 0.3 ~ 3.0
        m = Math.max(0.3, Math.min(3.0, m));
        mult.computeIfAbsent(world, k -> new HashMap<>()).put(category, m);
        // 영속화 — namespace=NS, owner=null(""), key="WORLD:CATEGORY"
        kr.reborn.core.RebornCore.get().kv().putDouble(NS, null, world.name() + ":" + category, m);
    }

    /** ShopManager가 buy/sell 가격 결정 시 사용. */
    public double multiplier(WorldKey world, Material material) {
        if (world == null) return 1.0;
        String cat = CategoryMapping.of(material);
        Double m = mult.getOrDefault(world, java.util.Collections.emptyMap()).get(cat);
        return m == null ? 1.0 : m;
    }

    public double multiplier(WorldKey world, String category) {
        if (world == null) return 1.0;
        Double m = mult.getOrDefault(world, java.util.Collections.emptyMap()).get(category);
        return m == null ? 1.0 : m;
    }

    public Map<String, Double> snapshot(WorldKey world) {
        return new HashMap<>(mult.getOrDefault(world, java.util.Collections.emptyMap()));
    }

    /**
     * 전 세계·전 카테고리 시세에 factor 적용 (주간 시장 등 글로벌 이벤트용).
     * 외부 reflection 호출 진입점 — Calendar.tick 등이 사용.
     * factor=0.9면 모든 multiplier ×0.9 (10% 할인). 영속화 포함.
     */
    public void applyGlobalFactor(double factor) {
        var kv = kr.reborn.core.RebornCore.get().kv();
        for (var w : WorldKey.values()) {
            var per = mult.computeIfAbsent(w, k -> new HashMap<>());
            for (String cat : BASE_PRICE.keySet()) {
                double cur = per.getOrDefault(cat, 1.0);
                double next = Math.max(0.3, Math.min(3.0, cur * factor));
                per.put(cat, next);
                kv.putDouble(NS, null, w.name() + ":" + cat, next);
            }
        }
    }
}
