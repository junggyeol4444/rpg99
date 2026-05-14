package kr.reborn.mob.spawn;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Items;
import kr.reborn.core.util.Rand;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.def.MobDef;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class SpawnTicker {

    private final RebornMob plugin;

    public SpawnTicker(RebornMob p) { this.plugin = p; }

    public void run() {
        int max = plugin.getConfig().getInt("max-mobs-per-chunk", 8);
        for (var w : Bukkit.getWorlds()) {
            WorldKey wk;
            try { wk = WorldKey.valueOf(w.getName().toUpperCase()); } catch (Exception e) { continue; }
            for (Chunk c : w.getLoadedChunks()) {
                int alive = countCustomMobs(c);
                if (alive >= max) continue;
                List<MobDef> candidates = new ArrayList<>();
                for (MobDef d : plugin.registry().all()) {
                    if (d.boss) continue;
                    if (d.world == wk) candidates.add(d);
                }
                if (candidates.isEmpty()) continue;
                if (!Rand.chance(0.10)) continue;
                MobDef def = candidates.get(Rand.range(0, candidates.size() - 1));
                spawn(def, c);
            }
        }
    }

    private int countCustomMobs(Chunk c) {
        int n = 0;
        for (Entity e : c.getEntities()) {
            if (e instanceof LivingEntity le && Items.tag(plugin, le.getEquipment() == null ? null : null, "rmob") != null) n++;
        }
        return n;
    }

    public LivingEntity spawn(MobDef def, Chunk chunk) {
        var w = chunk.getWorld();
        int x = (chunk.getX() << 4) + Rand.range(0, 15);
        int z = (chunk.getZ() << 4) + Rand.range(0, 15);
        int y = w.getHighestBlockYAt(x, z) + 1;
        var loc = new org.bukkit.Location(w, x + 0.5, y, z + 0.5);
        Entity ent = w.spawnEntity(loc, def.base);
        if (!(ent instanceof LivingEntity le)) { ent.remove(); return null; }
        le.setCustomName(def.name);
        le.setCustomNameVisible(true);
        var attr = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) { attr.setBaseValue(def.hp); le.setHealth(def.hp); }
        var dmg = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(def.damage);
        var spd = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(def.speed);
        le.getPersistentDataContainer().set(new NamespacedKey(plugin, "rmob"),
                PersistentDataType.STRING, def.id);
        return le;
    }
}
