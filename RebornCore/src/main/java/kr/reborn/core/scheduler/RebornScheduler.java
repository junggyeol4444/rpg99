package kr.reborn.core.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Paper와 Folia 양쪽에서 동작하는 스케줄러 추상화.
 * Folia가 감지되면 Folia API를, 아니면 Bukkit 스케줄러를 사용한다.
 */
public final class RebornScheduler {

    private final Plugin plugin;
    private final boolean folia;

    private RebornScheduler(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    public static RebornScheduler detect(Plugin plugin) {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        return new RebornScheduler(plugin, folia);
    }

    public boolean isFolia() { return folia; }

    public void runTask(Runnable task) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runTaskAsync(Runnable task) {
        if (folia) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runTaskLater(Runnable task, long delayTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), Math.max(1L, delayTicks));
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public void runTimer(Runnable task, long delayTicks, long periodTicks) {
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(),
                    Math.max(1L, delayTicks), Math.max(1L, periodTicks));
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public void runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        if (folia) {
            long ms = Math.max(50L, periodTicks * 50L);
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(),
                    Math.max(50L, delayTicks * 50L), ms, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    public void runEntityTask(Entity entity, Runnable task) {
        if (folia) {
            entity.getScheduler().run(plugin, t -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runRegionTask(Location loc, Runnable task) {
        if (folia) {
            Bukkit.getRegionScheduler().run(plugin, loc, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
