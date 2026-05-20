package kr.reborn.core.tier;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornTierUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public final class TierManager {

    private final RebornCore plugin;
    private final Map<String, List<Tier>> tables = new HashMap<>();

    public TierManager(RebornCore plugin) {
        this.plugin = plugin;
        loadTables();
    }

    public void loadTables() {
        tables.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("tier-tables");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            List<Tier> list = new ArrayList<>();
            List<Map<?, ?>> arr = root.getMapList(key);
            for (Map<?, ?> entry : arr) {
                String name = String.valueOf(entry.get("name"));
                double min = ((Number) entry.get("min")).doubleValue();
                double max = ((Number) entry.get("max")).doubleValue();
                int age = entry.get("age") != null ? ((Number) entry.get("age")).intValue() : 0;
                list.add(new Tier(name, min, max, age));
            }
            tables.put(key, list);
        }
    }

    public List<Tier> get(String tableId) {
        return tables.getOrDefault(tableId, Collections.emptyList());
    }

    /**
     * 현재 세계의 기본 경지 테이블 ID를 결정한다.
     * 동일 세계에서 여러 직업 분기가 있으면 setTier API로 명시 지정.
     */
    public String defaultTable(WorldKey w) {
        switch (w) {
            case FANTASY: return "FANTASY_SWORD";
            case DEMON: return "DEMON";
            case HEAVEN: return "HEAVEN";
            case SPIRIT: return "SPIRIT";
            case MARTIAL: return "MARTIAL";
            case IMMORTAL: return "IMMORTAL";
            case YOKAI: return "YOKAI";
            case EARTH: return "EARTH";
            case MAGITECH: return "MAGITECH";
            case APOCALYPSE: return "APOCALYPSE";
            case CYBERPUNK: return "CYBERPUNK";
            case DRAGON: return "DRAGON";
            case OCEAN: return "OCEAN";
            default: return null;
        }
    }

    public Tier resolveTier(double totalStats, String tableId, int age) {
        List<Tier> table = get(tableId);
        Tier found = null;
        for (Tier t : table) {
            if (totalStats >= t.min && (totalStats < t.max || t.max >= 999998)) {
                if (age >= t.requiredAge) found = t;
                else break;
            }
        }
        return found;
    }

    public Tier checkAndAdvance(Player p, PlayerData d) {
        String table = defaultTable(d.worldKey());
        if (table == null) return null;
        double total = totalStats(d);
        Tier curr = resolveTier(total, table, d.dragonAge());
        if (curr == null) return null;
        if (!curr.name.equals(d.tier())) {
            String prev = d.tier();
            d.tier(curr.name);
            Bukkit.getPluginManager().callEvent(new RebornTierUpEvent(p, prev, curr.name, d.worldKey()));
        }
        return curr;
    }

    public double totalStats(PlayerData d) {
        double sum = 0;
        for (StatType s : StatType.COMMON_8) sum += d.getStat(s);
        return sum;
    }
}
