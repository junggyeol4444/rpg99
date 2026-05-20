package kr.reborn.mob.boss;

import kr.reborn.core.RebornCore;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.def.MobDef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BossManager {

    private final RebornMob plugin;
    private final Map<UUID, BossInstance> active = new HashMap<>();

    public BossManager(RebornMob p) { this.plugin = p; }

    public LivingEntity summon(String id, Location loc) {
        MobDef def = plugin.registry().get(id);
        if (def == null || !def.boss) return null;
        var le = plugin.registry() == null ? null : null;

        var ticker = new kr.reborn.mob.spawn.SpawnTicker(plugin);
        LivingEntity e = ticker.spawn(def, loc.getChunk());
        if (e == null) return null;
        e.setCustomName("§5§l[BOSS] §f" + def.name);

        BossBar bar = Bukkit.createBossBar(def.name, BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        int radius = plugin.getConfig().getInt("boss-announce-radius", 50);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld() == loc.getWorld() && p.getLocation().distance(loc) < radius) {
                bar.addPlayer(p);
                p.sendTitle("§5보스 출현!", "§f" + def.name, 10, 60, 20);
            }
        }
        active.put(e.getUniqueId(), new BossInstance(def, e, bar));
        return e;
    }

    public void onDamage(LivingEntity entity, Player player, double amount) {
        BossInstance b = active.get(entity.getUniqueId());
        if (b == null) return;
        b.contribute(player.getUniqueId(), amount);
        b.bar.setProgress(Math.max(0, entity.getHealth() / entity.getMaxHealth()));
    }

    public void onDeath(LivingEntity entity) {
        BossInstance b = active.remove(entity.getUniqueId());
        if (b == null) return;
        b.bar.removeAll();
        Bukkit.broadcastMessage("§6[보스 처치] §f" + b.def.name);
        // 기여도 상위 N명 보상
        int top = plugin.getConfig().getInt("boss-contribution-top", 10);
        b.contributions.entrySet().stream()
                .sorted((a, c) -> Double.compare(c.getValue(), a.getValue()))
                .limit(top)
                .forEach(e -> {
                    Player p = Bukkit.getPlayer(e.getKey());
                    if (p != null) p.sendMessage("§6보스 기여 보상 지급");
                });
    }

    private static final class BossInstance {
        final MobDef def;
        final LivingEntity entity;
        final BossBar bar;
        final Map<UUID, Double> contributions = new HashMap<>();
        BossInstance(MobDef def, LivingEntity entity, BossBar bar) {
            this.def = def; this.entity = entity; this.bar = bar;
        }
        void contribute(UUID id, double v) { contributions.merge(id, v, Double::sum); }
    }
}
