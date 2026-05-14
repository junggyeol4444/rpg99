package kr.reborn.spawn.gym;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.spawn.RebornSpawn;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GymTracker implements Listener {

    private final RebornSpawn plugin;
    private final ConcurrentHashMap<UUID, Integer> points = new ConcurrentHashMap<>();

    public GymTracker(RebornSpawn p) { this.plugin = p; }

    private boolean inGym(org.bukkit.entity.Player p) {
        var c = plugin.getConfig().getConfigurationSection("lobby.gym-area");
        if (c == null) return false;
        var loc = p.getLocation();
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(plugin.getConfig().getString("lobby.world", "lobby"))) return false;
        return loc.getX() >= c.getDouble("min.x") && loc.getX() <= c.getDouble("max.x")
            && loc.getY() >= c.getDouble("min.y") && loc.getY() <= c.getDouble("max.y")
            && loc.getZ() >= c.getDouble("min.z") && loc.getZ() <= c.getDouble("max.z");
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent e) {
        if (!inGym(e.getPlayer()) || !e.isSprinting()) return;
        addPoint(e.getPlayer().getUniqueId(), 1);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!inGym(e.getPlayer()) || !e.isSneaking()) return;
        addPoint(e.getPlayer().getUniqueId(), 1);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getY() < e.getTo().getY() && inGym(e.getPlayer())) {
            // 점프 추정
            addPoint(e.getPlayer().getUniqueId(), 1);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        points.remove(e.getEntity().getUniqueId());
    }

    private void addPoint(UUID id, int n) {
        int next = points.merge(id, n, Integer::sum);
        if (next >= plugin.getConfig().getInt("lobby.gym-points-required", 100)) {
            applyGym(id);
            points.remove(id);
        }
    }

    private void applyGym(UUID id) {
        PlayerData d = RebornCore.get().api().getPlayerData(id);
        if (d.gymUsed()) return;
        var c = plugin.getConfig().getConfigurationSection("initial-stats.gym-bonus");
        int min = c.getInt("min", 1), max = c.getInt("max", 15);
        for (StatType t : StatType.COMMON_8) {
            d.addStat(t, Rand.range(min, max));
        }
        d.gymUsed(true);
        var p = Bukkit.getPlayer(id);
        if (p != null) Msg.send(p, "&6신들의 헬스장 — 모든 공통 스탯 강화!");
    }

    /** /gym 으로 즉시 정산 */
    public void manualFinalize(UUID id) { applyGym(id); }
}
