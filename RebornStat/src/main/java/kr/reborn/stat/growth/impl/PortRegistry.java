package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 해양 7대 항구 점령 엔진 (CityRegistry의 해양판).
 *
 * 점령 메커니즘은 사이버시티와 동일:
 *   영향력 ≥ 1000 & 현 통치자와 다른 제국 → 정복, 점령 직후 50% 감쇠.
 */
public final class PortRegistry {

    private static final String NS = "RebornStat.ocean.port";
    private static final int TAKEOVER_THRESHOLD = 1000;
    private static final int MAX_INFLUENCE = 5000;

    private final Map<String, OceanPort> ports = new LinkedHashMap<>();

    public PortRegistry() {
        for (String id : OceanPort.PORTS) ports.put(id, new OceanPort(id));
        load();
        loadBoundsFromConfig();
    }

    /**
     * config.yml의 port-bounds 섹션에서 각 항구 좌표 로드.
     *   port-bounds:
     *     ATLANTIS_HARBOR: { world: ocean, x: 100, z: 100, radius: 100 }
     *     ...
     */
    private void loadBoundsFromConfig() {
        try {
            var plugin = (org.bukkit.plugin.java.JavaPlugin)
                    org.bukkit.Bukkit.getPluginManager().getPlugin("RebornStat");
            if (plugin == null) return;
            var sec = plugin.getConfig().getConfigurationSection("port-bounds");
            if (sec == null) return;
            for (String key : sec.getKeys(false)) {
                OceanPort port = ports.get(key);
                if (port == null) continue;
                var ds = sec.getConfigurationSection(key);
                if (ds == null) continue;
                port.world = ds.getString("world");
                port.x = ds.getDouble("x");
                port.z = ds.getDouble("z");
                port.radius = ds.getDouble("radius", 80);
            }
        } catch (Throwable ignored) {}
    }

    public String detectAt(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String wn = loc.getWorld().getName();
        double px = loc.getX(), pz = loc.getZ();
        for (OceanPort p : ports.values()) {
            if (p.contains(wn, px, pz)) return p.id;
        }
        return null;
    }

    private void load() {
        try {
            var all = RebornCore.get().kv().loadAll(NS, null);
            for (var e : all.entrySet()) {
                int dot = e.getKey().indexOf('.');
                if (dot < 0) continue;
                String pId = e.getKey().substring(0, dot);
                String field = e.getKey().substring(dot + 1);
                OceanPort p = ports.get(pId);
                if (p == null) continue;
                switch (field) {
                    case "ruler" -> p.currentRuler = e.getValue().isEmpty() ? null : e.getValue();
                    case "since" -> {
                        try { p.ruledSince = Long.parseLong(e.getValue()); }
                        catch (Throwable ignored) {}
                    }
                    default -> {
                        if (field.startsWith("inf.")) {
                            String empireId = field.substring(4);
                            try { p.influence.put(empireId, Integer.parseInt(e.getValue())); }
                            catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public OceanPort get(String id) { return ports.get(id); }
    public Collection<OceanPort> all() { return ports.values(); }

    public int influenceOf(String portId, String empireId) {
        OceanPort p = ports.get(portId);
        return p == null ? 0 : p.influence.getOrDefault(empireId, 0);
    }

    public String rulerOf(String portId) {
        OceanPort p = ports.get(portId);
        return p == null ? null : p.currentRuler;
    }

    public void addInfluence(String portId, String empireId, int delta) {
        OceanPort p = ports.get(portId);
        if (p == null) return;
        synchronized (p) {
            int cur = p.influence.getOrDefault(empireId, 0);
            int next = Math.max(0, Math.min(MAX_INFLUENCE, cur + delta));
            p.influence.put(empireId, next);
            RebornCore.get().kv().putInt(NS, null, portId + ".inf." + empireId, next);
            checkTakeover(p);
        }
    }

    /** caller가 synchronized(p) 안에서 호출한다고 가정. */
    private void checkTakeover(OceanPort p) {
        if (p.influence.isEmpty()) return;
        String topEmpire = null;
        int topInf = 0;
        for (var e : p.influence.entrySet()) {
            if (e.getValue() > topInf) { topInf = e.getValue(); topEmpire = e.getKey(); }
        }
        if (topEmpire == null || topInf < TAKEOVER_THRESHOLD) return;
        if (topEmpire.equals(p.currentRuler)) return;
        String prev = p.currentRuler;
        p.currentRuler = topEmpire;
        p.ruledSince = System.currentTimeMillis();
        RebornCore.get().kv().put(NS, null, p.id + ".ruler", topEmpire);
        RebornCore.get().kv().putLong(NS, null, p.id + ".since", p.ruledSince);
        java.util.Map<String, Integer> halved = new java.util.LinkedHashMap<>();
        for (var e : p.influence.entrySet()) {
            int half = e.getValue() / 2;
            halved.put(e.getKey(), half);
            RebornCore.get().kv().putInt(NS, null, p.id + ".inf." + e.getKey(), half);
        }
        p.influence.clear();
        p.influence.putAll(halved);
        Bukkit.broadcastMessage("§3§l[" + p.id + " 점령] §f" + topEmpire
                + (prev == null ? " §7가 무인 항구를 차지했다."
                                : " §7가 §6" + prev + " §7로부터 항구를 빼앗았다."));
    }
}
