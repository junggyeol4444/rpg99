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
        crime.merge(killer.getUniqueId(), inc, Double::sum);
        fame.merge(killer.getUniqueId(), dec, Double::sum);
        announceLevel(killer);
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
