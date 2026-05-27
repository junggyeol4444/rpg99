package kr.reborn.stat.serendipity;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.stat.RebornStat;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** config의 fortunes: 섹션을 읽어 세계별 기연 목록으로 보관. */
public final class FortuneRegistry {

    private final RebornStat plugin;
    private final Map<String, Fortune> byId = new LinkedHashMap<>();
    private final Map<WorldKey, List<Fortune>> byWorld = new EnumMap<>(WorldKey.class);
    private final List<Fortune> global = new ArrayList<>();

    public FortuneRegistry(RebornStat plugin) { this.plugin = plugin; }

    public void load() {
        byId.clear();
        byWorld.clear();
        global.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("fortunes");
        if (root == null) { plugin.getLogger().info("기연 0종 (fortunes 섹션 없음)"); return; }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            WorldKey w = null;
            String ws = s.getString("world", "");
            if (!ws.isEmpty()) {
                try { w = WorldKey.valueOf(ws.toUpperCase()); } catch (IllegalArgumentException ignored) {}
            }
            Fortune.Trigger trig;
            try { trig = Fortune.Trigger.valueOf(s.getString("trigger", "EXPLORE").toUpperCase()); }
            catch (IllegalArgumentException e) { trig = Fortune.Trigger.EXPLORE; }
            Map<StatType, Double> rewards = new EnumMap<>(StatType.class);
            ConfigurationSection rs = s.getConfigurationSection("stats");
            if (rs != null) {
                for (String k : rs.getKeys(false)) {
                    try { rewards.put(StatType.valueOf(k.toUpperCase()), rs.getDouble(k)); }
                    catch (IllegalArgumentException ignored) {}
                }
            }
            Fortune f = new Fortune(id, s.getString("name", id), w, trig,
                    s.getString("param", ""), s.getDouble("chance", 0.01),
                    s.getBoolean("broadcast", false), s.getString("skill", ""),
                    s.getString("message", ""), rewards);
            byId.put(id, f);
            if (w == null) global.add(f);
            else byWorld.computeIfAbsent(w, k -> new ArrayList<>()).add(f);
        }
        plugin.getLogger().info("기연 " + byId.size() + "종 로드");
    }

    public Fortune get(String id) { return byId.get(id); }
    public Collection<Fortune> all() { return byId.values(); }

    /** 해당 세계에서 발동 가능한 기연 (세계 한정 + 전 세계 공통). */
    public List<Fortune> forWorld(WorldKey w) {
        List<Fortune> out = new ArrayList<>(global);
        out.addAll(byWorld.getOrDefault(w, List.of()));
        return out;
    }
}
