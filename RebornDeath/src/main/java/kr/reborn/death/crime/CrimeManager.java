package kr.reborn.death.crime;

import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrimeManager {

    private final RebornDeath plugin;
    private final ConcurrentHashMap<UUID, Double> crime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> fame = new ConcurrentHashMap<>();

    public CrimeManager(RebornDeath p) { this.plugin = p; }

    public double crime(UUID id) { return crime.getOrDefault(id, 0.0); }
    public double fame(UUID id) { return fame.getOrDefault(id, 0.0); }

    public void onPvpKill(Player killer, Player victim) {
        double inc = plugin.getConfig().getDouble("crime.pk-crime", 100);
        double dec = plugin.getConfig().getDouble("crime.pk-fame", -50);
        // 전시·광폭화 면책 적용
        double reduction = computeImmunityReduction(killer);
        inc *= (1.0 - reduction);
        dec *= (1.0 - reduction);
        crime.merge(killer.getUniqueId(), inc, Double::sum);
        fame.merge(killer.getUniqueId(), dec, Double::sum);
        if (reduction > 0) {
            Bukkit.getPlayer(killer.getUniqueId()).sendMessage(
                "§7전시/광폭 면책 — 범죄 -" + (int)(reduction * 100) + "%");
        }
        announceLevel(killer);
    }

    /** RebornClan 전시 면책 + RebornCurse 광폭화 면책 합산. */
    private double computeImmunityReduction(Player killer) {
        double reduction = 0;
        // RebornClan ClanWar 면책
        try {
            var cp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornClan");
            if (cp != null) {
                Object wars = cp.getClass().getMethod("wars").invoke(cp);
                if (wars != null) {
                    Object res = wars.getClass().getMethod("isImmuneByWar", java.util.UUID.class)
                            .invoke(wars, killer.getUniqueId());
                    if (Boolean.TRUE.equals(res)) reduction = Math.max(reduction, 1.0);
                }
            }
        } catch (Throwable ignored) {}
        // RebornCurse 광폭화 PK 감면
        try {
            var cp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object berserk = cp.getClass().getMethod("berserk").invoke(cp);
                if (berserk != null) {
                    Object res = berserk.getClass().getMethod("pkReductionFor", java.util.UUID.class)
                            .invoke(berserk, killer.getUniqueId());
                    if (res instanceof Number n) reduction = Math.max(reduction, n.doubleValue());
                }
            }
        } catch (Throwable ignored) {}
        return Math.min(1.0, reduction);
    }

    public void tickHourlyDecay() {
        double decay = plugin.getConfig().getDouble("crime.hourly-decay", 10);
        for (UUID id : crime.keySet()) {
            crime.merge(id, -decay, (a, b) -> Math.max(0, a + b));
        }
    }

    public int level(UUID id) {
        var thresholds = plugin.getConfig().getMapList("crime.thresholds");
        double c = crime(id);
        int lv = 0;
        for (Map<?, ?> t : thresholds) {
            double m = ((Number) t.get("min")).doubleValue();
            int l = ((Number) t.get("level")).intValue();
            if (c >= m) lv = l;
        }
        return lv;
    }

    public String label(UUID id) {
        var thresholds = plugin.getConfig().getMapList("crime.thresholds");
        double c = crime(id);
        String label = "일반";
        for (Map<?, ?> t : thresholds) {
            double m = ((Number) t.get("min")).doubleValue();
            String l = String.valueOf(t.get("label"));
            if (c >= m) label = l;
        }
        return label;
    }

    private void announceLevel(Player killer) {
        int lv = level(killer.getUniqueId());
        Bukkit.broadcastMessage("§c[현상수배] §f" + killer.getName() + " — " + label(killer.getUniqueId()));
    }
}
