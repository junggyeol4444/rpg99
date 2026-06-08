package kr.reborn.god.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DomainManager {

    private static final String NS = "RebornGod.domain";

    private final RebornGod plugin;
    /** domainWorld → 신 OR 권한 위임된 자 */
    private final Map<String, DomainRules> rules = new HashMap<>();

    public DomainManager(RebornGod p) {
        this.plugin = p;
        loadAll();
    }

    private void loadAll() {
        try {
            // NS+null+(worldName.field) 패턴: loadAll(NS, null)는 Map<key, value> 반환
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            Map<String, Map<String, String>> byWorld = new HashMap<>();
            for (var e : all.entrySet()) {
                int dot = e.getKey().indexOf('.');
                if (dot < 0) continue;
                String worldName = e.getKey().substring(0, dot);
                String field = e.getKey().substring(dot + 1);
                byWorld.computeIfAbsent(worldName, k -> new HashMap<>()).put(field, e.getValue());
            }
            for (var e : byWorld.entrySet()) {
                String worldName = e.getKey();
                var fields = e.getValue();
                try {
                    String ownerStr = fields.get("owner");
                    if (ownerStr == null) continue;
                    UUID owner = UUID.fromString(ownerStr);
                    int size = Integer.parseInt(fields.getOrDefault("size", "500"));
                    DomainRules r = new DomainRules(owner, size);
                    String grav = fields.get("gravity");
                    String dmg = fields.get("damageMult");
                    String guests = fields.get("guests");
                    if (grav != null) r.gravity = Double.parseDouble(grav);
                    if (dmg != null) r.damageMult = Double.parseDouble(dmg);
                    if (guests != null && !guests.isEmpty()) {
                        for (String g : guests.split(",")) {
                            try { r.allowedGuests.add(UUID.fromString(g)); }
                            catch (Throwable ignored) {}
                        }
                    }
                    rules.put(worldName, r);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void persist(String worldName, DomainRules r) {
        try {
            var kv = kr.reborn.core.RebornCore.get().kv();
            kv.put(NS, null, worldName + ".owner", r.ownerGod.toString());
            kv.putInt(NS, null, worldName + ".size", r.size);
            kv.putDouble(NS, null, worldName + ".gravity", r.gravity);
            kv.putDouble(NS, null, worldName + ".damageMult", r.damageMult);
            StringBuilder gb = new StringBuilder();
            for (UUID g : r.allowedGuests) {
                if (gb.length() > 0) gb.append(',');
                gb.append(g);
            }
            kv.put(NS, null, worldName + ".guests", gb.toString());
        } catch (Throwable ignored) {}
    }

    public boolean create(Player p) {
        God g = plugin.gods().of(p.getUniqueId());
        if (g == null) { Msg.error(p, "신이 아니다."); return false; }
        String name = "domain_" + p.getName().toLowerCase();
        if (Bukkit.getWorld(name) != null) {
            Msg.warn(p, "이미 신역이 있다."); return false;
        }
        // 신성 등급에 따른 크기
        var sizeMap = plugin.getConfig().getConfigurationSection("domain.size");
        int size = 500;
        if (sizeMap != null) {
            for (String tier : new String[]{"하급신", "중급신", "상급신", "주신", "개념신"}) {
                size = Math.max(size, sizeMap.getInt(tier, 500));
                if (g.tier(plugin.getConfig().getMapList("tiers")).equals(tier)) break;
            }
        }
        World w = new WorldCreator(name).createWorld();
        if (w == null) { Msg.error(p, "월드 생성 실패."); return false; }
        try { w.setGameRule(GameRule.KEEP_INVENTORY, true); } catch (Throwable ignored) {}
        try { w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); } catch (Throwable ignored) {}
        g.domainWorld = name;
        DomainRules r = new DomainRules(p.getUniqueId(), size);
        rules.put(name, r);
        persist(name, r);
        Msg.send(p, "&6신역 생성: " + name + " (크기: " + size + ")");
        return true;
    }

    public void enter(Player p) {
        God g = plugin.gods().of(p.getUniqueId());
        if (g == null || g.domainWorld.isEmpty()) { Msg.error(p, "신역 없음."); return; }
        World w = Bukkit.getWorld(g.domainWorld);
        if (w != null) p.teleport(w.getSpawnLocation());
    }

    public void invite(Player owner, Player guest) {
        God g = plugin.gods().of(owner.getUniqueId());
        if (g == null || g.domainWorld.isEmpty()) { Msg.error(owner, "신역 없음."); return; }
        DomainRules r = rules.get(g.domainWorld);
        if (r != null) {
            r.allowedGuests.add(guest.getUniqueId());
            persist(g.domainWorld, r);
        }
        Msg.send(owner, "&a" + guest.getName() + "을(를) 신역에 초대.");
        Msg.send(guest, "&6" + owner.getName() + "이(가) 신역에 초대했다.");
    }

    public void kick(Player owner, Player guest) {
        God g = plugin.gods().of(owner.getUniqueId());
        if (g == null) return;
        DomainRules r = rules.get(g.domainWorld);
        if (r != null) {
            r.allowedGuests.remove(guest.getUniqueId());
            persist(g.domainWorld, r);
        }
        if (guest.getWorld().getName().equals(g.domainWorld)) {
            // 거주 세계 (PlayerData.worldKey)로 복귀, 없으면 lobby
            World main = null;
            try {
                var d = kr.reborn.core.RebornCore.get().api().getPlayerData(guest.getUniqueId());
                if (d != null) main = Bukkit.getWorld(d.worldKey().name().toLowerCase());
            } catch (Throwable ignored) {}
            if (main == null) main = Bukkit.getWorld("lobby");
            if (main == null) main = Bukkit.getWorld("world");
            if (main != null) {
                final World target = main;
                kr.reborn.core.RebornCore.get().scheduler()
                        .runEntityTask(guest, () -> guest.teleport(target.getSpawnLocation()));
            }
        }
    }

    public void setRule(Player owner, String key, String value) {
        God g = plugin.gods().of(owner.getUniqueId());
        if (g == null) return;
        DomainRules r = rules.get(g.domainWorld);
        if (r == null) return;
        World w = Bukkit.getWorld(g.domainWorld);
        if (w == null) return;
        switch (key.toUpperCase()) {
            case "TIME":
                w.setTime(safeLong(value, 6000));
                break;
            case "WEATHER":
                w.setStorm("storm".equalsIgnoreCase(value) || "rain".equalsIgnoreCase(value));
                break;
            case "GRAVITY":
                r.gravity = safeDouble(value, 1.0);
                break;
            case "PVP":
                w.setPVP(Boolean.parseBoolean(value));
                break;
            case "DAMAGE_MULT":
                r.damageMult = safeDouble(value, 1.0);
                break;
        }
        persist(g.domainWorld, r);
        Msg.send(owner, "&6신역 규칙 설정: " + key + "=" + value);
    }

    public DomainRules rulesOf(String worldName) { return rules.get(worldName); }

    public static final class DomainRules {
        public final UUID ownerGod;
        public final int size;
        public final java.util.Set<UUID> allowedGuests = new java.util.HashSet<>();
        public double gravity = 1.0;
        public double damageMult = 1.0;
        public DomainRules(UUID owner, int size) { this.ownerGod = owner; this.size = size; }
    }

    private long safeLong(String s, long def) { try { return Long.parseLong(s); } catch (Exception e) { return def; } }
    private double safeDouble(String s, double def) { try { return Double.parseDouble(s); } catch (Exception e) { return def; } }
}
