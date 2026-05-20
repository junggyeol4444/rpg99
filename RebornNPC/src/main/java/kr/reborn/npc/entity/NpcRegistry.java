package kr.reborn.npc.entity;

import kr.reborn.core.data.WorldKey;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.SimpleAI;
import kr.reborn.npc.emotion.Emotion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcRegistry {

    private final RebornNPC plugin;
    private final ConcurrentHashMap<String, RebornNpc> byId = new ConcurrentHashMap<>();
    private final EnumMap<Emotion.Kind, Double> decayRates = new EnumMap<>(Emotion.Kind.class);

    public NpcRegistry(RebornNPC plugin) {
        this.plugin = plugin;
        var s = plugin.getConfig().getConfigurationSection("emotion-decay-rate");
        for (Emotion.Kind k : Emotion.Kind.values()) {
            decayRates.put(k, s == null ? 0.5 : s.getDouble(k.name().toLowerCase(), 0.5));
        }
    }

    public RebornNpc get(String id) { return byId.get(id); }
    public java.util.Collection<RebornNpc> all() { return byId.values(); }

    public RebornNpc spawn(String id, String name, WorldKey world, Location loc, String faction, String job) {
        RebornNpc n = new RebornNpc(id, name, world, loc);
        n.faction = faction;
        n.job = job;
        byId.put(id, n);
        materialize(n);
        return n;
    }

    public void remove(String id) {
        RebornNpc n = byId.remove(id);
        if (n != null && n.bukkitEntityId != null) {
            var e = Bukkit.getEntity(n.bukkitEntityId);
            if (e != null) e.remove();
        }
    }

    private void materialize(RebornNpc n) {
        if (n.location == null) return;
        World w = n.location.getWorld();
        if (w == null) return;
        var v = w.spawn(n.location, n.defaultEntity());
        v.setCustomName(n.displayName);
        v.setCustomNameVisible(true);
        v.setAI(false);
        n.bukkitEntityId = v.getUniqueId();
    }

    public void tickAll() {
        for (RebornNpc n : byId.values()) {
            n.emotion.decay(decayRates);
            SimpleAI.step(n);
        }
    }

    public void loadAll() {
        // 0) 플러그인 jar 내장 기본 좌표 (npcs-lobby.yml) 추출
        plugin.saveResource("npcs-lobby.yml", false);

        // 1) config.yml의 npcs: 섹션을 사전 정의 NPC로 등록 (좌표는 임시, 운영 시 /rnpc spawn으로 배치)
        var npcSec = plugin.getConfig().getConfigurationSection("npcs");
        if (npcSec != null) {
            for (String id : npcSec.getKeys(false)) {
                if (byId.containsKey(id)) continue;
                var s = npcSec.getConfigurationSection(id);
                if (s == null) continue;
                String name = s.getString("name", id);
                WorldKey world;
                try { world = WorldKey.valueOf(s.getString("world", "LOBBY")); }
                catch (Exception e) { world = WorldKey.LOBBY; }
                String faction = s.getString("faction", "");
                String job = s.getString("job", "VILLAGER");
                RebornNpc n = new RebornNpc(id, name, world, null);
                n.faction = faction;
                n.job = job;
                n.hermit = s.getBoolean("hermit", false);
                var statSec = s.getConfigurationSection("stats");
                if (statSec != null) {
                    for (String k : statSec.getKeys(false)) n.stats.put(k, statSec.getDouble(k));
                }
                byId.put(id, n);
            }
            plugin.getLogger().info("config 사전 정의 NPC " + byId.size() + "개 등록");
        }

        // 2) 좌표 포함 NPC 파일들 로드 (npcs-lobby.yml, npcs.yml)
        loadCoordsFromFile(new File(plugin.getDataFolder(), "npcs-lobby.yml"));
        loadCoordsFromFile(new File(plugin.getDataFolder(), "npcs.yml"));
    }

    private void loadCoordsFromFile(File f) {
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String id : y.getKeys(false)) {
            String name = y.getString(id + ".name", id);
            WorldKey world;
            try { world = WorldKey.valueOf(y.getString(id + ".world", "FANTASY")); }
            catch (Exception e) { world = WorldKey.FANTASY; }
            String worldName = y.getString(id + ".bukkit-world", "world");
            World bw = Bukkit.getWorld(worldName);
            if (bw == null) continue;
            Location loc = new Location(bw,
                    y.getDouble(id + ".x"), y.getDouble(id + ".y"), y.getDouble(id + ".z"));
            // 사전 정의 데이터가 있으면 좌표만 갱신 후 스폰
            RebornNpc existing = byId.get(id);
            if (existing != null) {
                existing.location = loc;
                materialize(existing);
            } else {
                RebornNpc n = spawn(id, name, world, loc,
                        y.getString(id + ".faction", ""), y.getString(id + ".job", "VILLAGER"));
                n.hermit = y.getBoolean(id + ".hermit", false);
                var stSec = y.getConfigurationSection(id + ".stats");
                if (stSec != null) {
                    for (String key : stSec.getKeys(false)) n.stats.put(key, stSec.getDouble(key));
                }
            }
        }
    }

    public void saveAll() {
        File f = new File(plugin.getDataFolder(), "npcs.yml");
        plugin.getDataFolder().mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        for (RebornNpc n : byId.values()) {
            if (n.location == null) continue;  // 좌표 없는 사전 정의는 저장 안 함
            String b = n.id + ".";
            y.set(b + "name", n.displayName);
            y.set(b + "world", n.world.name());
            y.set(b + "bukkit-world", n.location.getWorld() == null ? "world" : n.location.getWorld().getName());
            y.set(b + "x", n.location.getX());
            y.set(b + "y", n.location.getY());
            y.set(b + "z", n.location.getZ());
            y.set(b + "faction", n.faction);
            y.set(b + "job", n.job);
            y.set(b + "hermit", n.hermit);
            for (var e : n.stats.entrySet()) y.set(b + "stats." + e.getKey(), e.getValue());
        }
        try { y.save(f); } catch (Exception ignored) {}
    }

    public RebornNpc nearest(Location loc, double radius) {
        RebornNpc best = null; double dist = radius * radius;
        for (RebornNpc n : byId.values()) {
            if (n.location.getWorld() != loc.getWorld()) continue;
            double d = n.location.distanceSquared(loc);
            if (d < dist) { dist = d; best = n; }
        }
        return best;
    }

    public RebornNpc byEntity(UUID entityId) {
        for (RebornNpc n : byId.values()) {
            if (entityId.equals(n.bukkitEntityId)) return n;
        }
        return null;
    }
}
