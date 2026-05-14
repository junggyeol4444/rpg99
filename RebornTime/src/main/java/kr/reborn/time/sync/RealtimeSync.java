package kr.reborn.time.sync;

import kr.reborn.core.util.Rand;
import kr.reborn.time.RebornTime;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.time.LocalTime;
import java.time.ZoneId;

public final class RealtimeSync {

    private final RebornTime plugin;

    public RealtimeSync(RebornTime p) { this.plugin = p; }

    public void syncAll() {
        ZoneId tz = ZoneId.of(plugin.getConfig().getString("timezone", "Asia/Seoul"));
        LocalTime now = LocalTime.now(tz);
        long target = realtimeToMcTicks(now);
        var exceptions = plugin.getConfig().getConfigurationSection("exception-worlds");

        for (World w : Bukkit.getWorlds()) {
            if (exceptions != null && exceptions.contains(w.getName())) {
                applyException(w, exceptions.getConfigurationSection(w.getName()));
                continue;
            }
            try { w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); } catch (Throwable ignored) {}
            w.setTime(target);
        }
    }

    private long realtimeToMcTicks(LocalTime t) {
        // 06:00 = 0틱, 18:00 = 12000틱, 00:00 = 18000틱
        int seconds = t.toSecondOfDay();
        // 24h = 24000ticks
        long ticks = (long) ((seconds / 86400.0) * 24000.0);
        // shift by -6h so 06:00 → 0
        ticks = (ticks - 6000 + 24000) % 24000;
        return ticks;
    }

    private void applyException(World w, org.bukkit.configuration.ConfigurationSection s) {
        String mode = s.getString("mode", "FIXED");
        try { w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); } catch (Throwable ignored) {}
        if ("FIXED".equals(mode)) {
            w.setTime(s.getLong("time", 0));
        } else if ("FLUX".equals(mode)) {
            // 시간계: 5분마다 랜덤
            w.setTime((long) (Rand.rangeD(0, 24000)));
        } else if ("SCALE".equals(mode)) {
            // 시간의 방: 외부 시간 × ratio
            int ratio = s.getInt("ratio", 10);
            ZoneId tz = ZoneId.of(plugin.getConfig().getString("timezone", "Asia/Seoul"));
            int seconds = LocalTime.now(tz).toSecondOfDay();
            w.setTime((long) ((seconds * ratio / 86400.0) * 24000.0) % 24000);
        }
    }
}
