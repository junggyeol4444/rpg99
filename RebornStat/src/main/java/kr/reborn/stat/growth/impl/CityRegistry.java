package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사이버시티 7대 구역 점령 엔진.
 *
 * 점령 메커니즘:
 *   1. 플레이어가 활동 중인 구역에서 후원 코프 영향력 누적 (gainCorpFavor → addInfluence).
 *   2. 영향력 ≥ 1000 & 현 점유자와 다른 코프 → 점령 전환.
 *   3. 점령 직후 모든 코프 영향력 50% 감쇠 (방어전 쿨다운 효과).
 *   4. 영향력 상한 5000 — 무한 누적 방지.
 *
 * 점령 효과 (CyberpunkGrowth에서 활용):
 *   - 소유 코프의 동맹(tier≥4) 플레이어가 해당 구역에서 활동 시 보너스 +10%.
 *   - 점령 사건은 전 서버 방송.
 */
public final class CityRegistry {

    private static final String NS = "RebornStat.cyberpunk.city";
    private static final int TAKEOVER_THRESHOLD = 1000;
    private static final int MAX_INFLUENCE = 5000;

    private final Map<String, CyberCity> districts = new LinkedHashMap<>();

    public CityRegistry() {
        for (String id : CyberCity.DISTRICTS) districts.put(id, new CyberCity(id));
        load();
        loadBoundsFromConfig();
    }

    /**
     * config.yml의 district-bounds 섹션에서 각 구역 좌표 로드.
     * 형식:
     *   district-bounds:
     *     NIGHT_MARKET: { world: cyberpunk, x: 100, z: 100, radius: 80 }
     *     ...
     * 설정 없으면 hasBounds=false → 수동 /district enter만 작동.
     */
    private void loadBoundsFromConfig() {
        try {
            var plugin = (org.bukkit.plugin.java.JavaPlugin)
                    org.bukkit.Bukkit.getPluginManager().getPlugin("RebornStat");
            if (plugin == null) return;
            var sec = plugin.getConfig().getConfigurationSection("district-bounds");
            if (sec == null) return;
            for (String key : sec.getKeys(false)) {
                CyberCity c = districts.get(key);
                if (c == null) continue;
                var ds = sec.getConfigurationSection(key);
                if (ds == null) continue;
                c.world = ds.getString("world");
                c.x = ds.getDouble("x");
                c.z = ds.getDouble("z");
                c.radius = ds.getDouble("radius", 50);
            }
        } catch (Throwable ignored) {}
    }

    /** 주어진 위치에 해당하는 구역 id 반환. null이면 어떤 구역에도 안 속함. */
    public String detectAt(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String wn = loc.getWorld().getName();
        double px = loc.getX(), pz = loc.getZ();
        for (CyberCity c : districts.values()) {
            if (c.contains(wn, px, pz)) return c.id;
        }
        return null;
    }

    private void load() {
        try {
            var all = RebornCore.get().kv().loadAll(NS, null);
            for (var e : all.entrySet()) {
                int dot = e.getKey().indexOf('.');
                if (dot < 0) continue;
                String dId = e.getKey().substring(0, dot);
                String field = e.getKey().substring(dot + 1);
                CyberCity c = districts.get(dId);
                if (c == null) continue;
                switch (field) {
                    case "owner" -> c.currentOwner = e.getValue().isEmpty() ? null : e.getValue();
                    case "since" -> {
                        try { c.ownedSince = Long.parseLong(e.getValue()); }
                        catch (Throwable ignored) {}
                    }
                    default -> {
                        if (field.startsWith("inf.")) {
                            String corpId = field.substring(4);
                            try { c.influence.put(corpId, Integer.parseInt(e.getValue())); }
                            catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public CyberCity get(String id) { return districts.get(id); }
    public Collection<CyberCity> all() { return districts.values(); }

    public int influenceOf(String districtId, String corpId) {
        CyberCity c = districts.get(districtId);
        return c == null ? 0 : c.influence.getOrDefault(corpId, 0);
    }

    public String ownerOf(String districtId) {
        CyberCity c = districts.get(districtId);
        return c == null ? null : c.currentOwner;
    }

    public void addInfluence(String districtId, String corpId, int delta) {
        CyberCity c = districts.get(districtId);
        if (c == null) return;
        int cur = c.influence.getOrDefault(corpId, 0);
        int next = Math.max(0, Math.min(MAX_INFLUENCE, cur + delta));
        c.influence.put(corpId, next);
        RebornCore.get().kv().putInt(NS, null, districtId + ".inf." + corpId, next);
        checkTakeover(c);
    }

    private void checkTakeover(CyberCity c) {
        if (c.influence.isEmpty()) return;
        String topCorp = null;
        int topInf = 0;
        for (var e : c.influence.entrySet()) {
            if (e.getValue() > topInf) { topInf = e.getValue(); topCorp = e.getKey(); }
        }
        if (topCorp == null || topInf < TAKEOVER_THRESHOLD) return;
        if (topCorp.equals(c.currentOwner)) return;
        String prev = c.currentOwner;
        c.currentOwner = topCorp;
        c.ownedSince = System.currentTimeMillis();
        RebornCore.get().kv().put(NS, null, c.id + ".owner", topCorp);
        RebornCore.get().kv().putLong(NS, null, c.id + ".since", c.ownedSince);
        // 점령 직후 50% 감쇠 — 후속 방어전 쿨다운
        for (var e : c.influence.entrySet()) {
            int half = e.getValue() / 2;
            c.influence.put(e.getKey(), half);
            RebornCore.get().kv().putInt(NS, null, c.id + ".inf." + e.getKey(), half);
        }
        Bukkit.broadcastMessage("§b§l[" + c.id + " 점령] §f" + topCorp
                + (prev == null ? " §7가 무주공산을 차지했다."
                                : " §7가 §6" + prev + " §7로부터 구역을 빼앗았다."));
    }
}
