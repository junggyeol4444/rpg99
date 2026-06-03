package kr.reborn.craft.manager;

import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.event.RebornProficiencyUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 직업별 숙련도(EXP) 관리. */
public final class ProficiencyManager {

    public static final class Tier {
        public final String name;
        public final int min;
        public Tier(String name, int min) { this.name = name; this.min = min; }
    }

    private static final String NS = "RebornCraft.proficiency";

    private final RebornCraft plugin;
    private final java.util.List<Tier> tiers = new java.util.ArrayList<>();
    /** uuid → profession → exp */
    private final Map<UUID, Map<String, Integer>> data = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private final int maxJobs;

    public ProficiencyManager(RebornCraft plugin) {
        this.plugin = plugin;
        this.maxJobs = plugin.getConfig().getInt("proficiency.max-jobs", 3);
        for (Map<?, ?> raw : plugin.getConfig().getMapList("proficiency.TIERS")) {
            tiers.add(new Tier(String.valueOf(raw.get("name")),
                    raw.get("min") instanceof Number n ? n.intValue() : 0));
        }
    }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, p);
            Map<String, Integer> map = new HashMap<>();
            for (var e : all.entrySet()) {
                try { map.put(e.getKey(), Integer.parseInt(e.getValue())); } catch (Throwable ignored) {}
            }
            if (!map.isEmpty()) data.put(p, map);
        }
    }

    public int exp(UUID p, String profession) {
        ensureLoaded(p);
        return data.getOrDefault(p, Map.of()).getOrDefault(profession, 0);
    }

    public Tier tier(UUID p, String profession) {
        int x = exp(p, profession);
        Tier last = tiers.isEmpty() ? null : tiers.get(0);
        for (Tier t : tiers) {
            if (x >= t.min) last = t;
        }
        return last;
    }

    public boolean canLearn(UUID p, String profession) {
        ensureLoaded(p);
        Map<String, Integer> map = data.computeIfAbsent(p, k -> new HashMap<>());
        if (map.containsKey(profession)) return true;
        return map.size() < maxJobs;
    }

    public boolean learn(UUID p, String profession) {
        if (!canLearn(p, profession)) return false;
        data.computeIfAbsent(p, k -> new HashMap<>()).putIfAbsent(profession, 0);
        kr.reborn.core.RebornCore.get().kv().putInt(NS, p, profession, 0);
        return true;
    }

    public void grantExp(Player p, String profession, int gained) {
        ensureLoaded(p.getUniqueId());
        Map<String, Integer> map = data.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        if (!map.containsKey(profession)) return;
        Tier before = tier(p.getUniqueId(), profession);
        int next = map.merge(profession, gained, Integer::sum);
        kr.reborn.core.RebornCore.get().kv().putInt(NS, p.getUniqueId(), profession, next);
        Tier after = tier(p.getUniqueId(), profession);
        if (before != after && after != null) {
            Bukkit.getPluginManager().callEvent(new RebornProficiencyUpEvent(p, profession, after.name, next));
        }
    }
}
