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
        File f = new File(plugin.getDataFolder(), "npcs.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String id : y.getKeys(false)) {
            String name = y.getString(id + ".name", id);
            WorldKey world = WorldKey.valueOf(y.getString(id + ".world", "FANTASY"));
            String worldName = y.getString(id + ".bukkit-world", "world");
            World bw = Bukkit.getWorld(worldName);
            if (bw == null) continue;
            Location loc = new Location(bw,
                    y.getDouble(id + ".x"), y.getDouble(id + ".y"), y.getDouble(id + ".z"));
            RebornNpc n = spawn(id, name, world, loc,
                    y.getString(id + ".faction", ""), y.getString(id + ".job", "VILLAGER"));
            n.hermit = y.getBoolean(id + ".hermit", false);
            for (String key : y.getConfigurationSection(id + ".stats") == null ?
                    java.util.Collections.<String>emptySet()
                    : y.getConfigurationSection(id + ".stats").getKeys(false)) {
                n.stats.put(key, y.getDouble(id + ".stats." + key));
            }
        }
    }

    public void saveAll() {
        File f = new File(plugin.getDataFolder(), "npcs.yml");
        plugin.getDataFolder().mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        for (RebornNpc n : byId.values()) {
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
