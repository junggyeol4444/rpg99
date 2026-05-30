package kr.reborn.economy.manager;

import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.economy.RebornEconomy;
import kr.reborn.economy.event.RebornShopBuyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 상점 정의 + GUI 표시 + 구매/판매 로직. */
public final class ShopManager {

    public static final class ShopItem {
        public final String id;
        public final Material material;
        public final String currency;
        public final long buy, sell;
        public int stock;       // -1 = 무한
        public final int restockMin;

        public ShopItem(String id, Material material, String currency, long buy, long sell,
                        int stock, int restockMin) {
            this.id = id; this.material = material; this.currency = currency;
            this.buy = buy; this.sell = sell; this.stock = stock; this.restockMin = restockMin;
        }
    }

    public static final class Shop {
        public final String id;
        public final String name;
        public final List<ShopItem> items = new ArrayList<>();

        public Shop(String id, String name) {
            this.id = id; this.name = name;
        }
    }

    private final RebornEconomy plugin;
    private final Map<String, Shop> shops = new HashMap<>();

    public ShopManager(RebornEconomy plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shops");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            Shop shop = new Shop(id, s.getString("name", id));
            for (Map<?, ?> raw : s.getMapList("items")) {
                Material m = Material.matchMaterial(String.valueOf(raw.getOrDefault("material", "STONE")));
                if (m == null) continue;
                shop.items.add(new ShopItem(
                        String.valueOf(raw.getOrDefault("id", "item")),
                        m,
                        String.valueOf(raw.getOrDefault("currency", "GOLD_COIN")),
                        ((Number) raw.getOrDefault("buy", 0)).longValue(),
                        ((Number) raw.getOrDefault("sell", 0)).longValue(),
                        ((Number) raw.getOrDefault("stock", -1)).intValue(),
                        ((Number) raw.getOrDefault("restock-minutes", 0)).intValue()
                ));
            }
            shops.put(id, shop);
        }
    }

    public Shop get(String id) { return shops.get(id); }
    public java.util.Collection<Shop> all() { return shops.values(); }

    /** NPC가 차린 상점 등록 (RebornNpcWorldImpactEvent 소비). 기본 잡화 매대 포함. */
    public Shop registerNpc(String id, String name) {
        Shop existing = shops.get(id);
        if (existing != null) return existing;
        Shop s = new Shop(id, name);
        s.items.add(new ShopItem("bread", org.bukkit.Material.BREAD, "GOLD_COIN", 8, 2, -1, 0));
        s.items.add(new ShopItem("torch", org.bukkit.Material.TORCH, "GOLD_COIN", 4, 1, -1, 0));
        shops.put(id, s);
        return s;
    }

    public void open(Player p, String shopId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            Msg.error(p, "상점을 찾을 수 없습니다: " + shopId);
            return;
        }
        var b = plugin.gui().builder(shop.name, 6);
        int slot = 0;
        for (ShopItem it : shop.items) {
            if (slot >= 54) break;
            String stockStr = it.stock < 0 ? "&7무한" : (it.stock <= 0 ? "&c품절" : "&f" + it.stock);
            ItemStack icon = Items.of(it.material, "&e" + it.id,
                    "&7구매가: &f" + it.buy + " " + it.currency,
                    "&7판매가: &f" + it.sell + " " + it.currency,
                    "&7재고: " + stockStr,
                    "",
                    "&a좌클릭: 구매  &c우클릭: 판매");
            final ShopItem item = it;
            b.set(slot++, icon, e -> {
                if (e.isLeftClick()) buy(p, shopId, item);
                else if (e.isRightClick()) sell(p, shopId, item);
            });
        }
        b.open(p);
    }

    private void buy(Player p, String shopId, ShopItem it) {
        if (it.stock == 0) { Msg.error(p, "품절되었습니다."); return; }
        long finalPrice = scaledBuy(p, it);
        if (!plugin.currencies().withdraw(p.getUniqueId(), it.currency, finalPrice)) {
            Msg.error(p, "화폐가 부족합니다.");
            return;
        }
        if (it.stock > 0) it.stock--;
        p.getInventory().addItem(new ItemStack(it.material, 1));
        Bukkit.getPluginManager().callEvent(new RebornShopBuyEvent(p, shopId, it.id, 1, finalPrice));
        Msg.send(p, "&a구매 완료: " + it.id + " &7(" + finalPrice + " "
                + it.currency + (finalPrice != it.buy ? " §6(시세 적용)" : "") + ")");
    }

    private void sell(Player p, String shopId, ShopItem it) {
        if (!p.getInventory().contains(it.material)) {
            Msg.error(p, "판매할 아이템이 없습니다.");
            return;
        }
        p.getInventory().removeItem(new ItemStack(it.material, 1));
        long finalSell = scaledSell(p, it);
        plugin.currencies().deposit(p.getUniqueId(), it.currency, finalSell);
        Msg.send(p, "&a판매 완료: " + it.id + " (+" + finalSell + " " + it.currency
                + (finalSell != it.sell ? " §6(시세 적용)" : "") + ")");
    }

    /** 플레이어 현재 세계의 시세를 적용한 구매가. */
    private long scaledBuy(Player p, ShopItem it) {
        try {
            var data = kr.reborn.core.RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (data == null) return it.buy;
            double m = plugin.priceController().multiplier(data.worldKey(), it.material);
            return Math.max(1, Math.round(it.buy * m));
        } catch (Throwable t) { return it.buy; }
    }

    /** 시세 적용 판매가 (판매는 보통 구매가의 25% 정도 — 시세 영향 동일). */
    private long scaledSell(Player p, ShopItem it) {
        try {
            var data = kr.reborn.core.RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (data == null) return it.sell;
            double m = plugin.priceController().multiplier(data.worldKey(), it.material);
            return Math.max(0, Math.round(it.sell * m));
        } catch (Throwable t) { return it.sell; }
    }
}
