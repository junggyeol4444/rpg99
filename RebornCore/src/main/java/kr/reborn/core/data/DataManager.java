package kr.reborn.core.data;

import kr.reborn.core.RebornCore;
import kr.reborn.core.db.Database;
import kr.reborn.core.event.RebornStatChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class DataManager {

    private final RebornCore plugin;
    private final Database db;
    private final ConcurrentHashMap<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(RebornCore plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData getOrLoad(UUID uuid) {
        PlayerData d = cache.get(uuid);
        if (d != null) return d;
        return loadSync(uuid);
    }

    public void loadAsync(UUID uuid, java.util.function.Consumer<PlayerData> cb) {
        plugin.scheduler().runTaskAsync(() -> {
            PlayerData d = loadSync(uuid);
            cb.accept(d);
        });
    }

    public PlayerData loadSync(UUID uuid) {
        PlayerData d = new PlayerData(uuid);
        try (Connection c = db.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT name, world_key, tier, title_id, clan_id, lineage, dragon_age, " +
                            "deaths, reincarnations, gym_used, child_start, first_join, last_join, playtime " +
                            "FROM reborn_player WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        d.name(rs.getString(1));
                        try { d.worldKey(WorldKey.valueOf(rs.getString(2))); } catch (Exception ignored) {}
                        d.tier(rs.getString(3));
                        d.titleId(rs.getString(4));
                        d.clanId(rs.getString(5));
                        d.lineage(rs.getString(6));
                        d.dragonAge(rs.getInt(7));
                        d.deaths(rs.getInt(8));
                        d.reincarnations(rs.getInt(9));
                        d.gymUsed(rs.getInt(10) == 1);
                        d.childStart(rs.getInt(11) == 1);
                        d.firstJoin(rs.getLong(12));
                        d.lastJoin(rs.getLong(13));
                        d.playtime(rs.getLong(14));
                    } else {
                        d.firstJoin(System.currentTimeMillis());
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT stat, value FROM reborn_stats WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            d.setStat(StatType.valueOf(rs.getString(1)), rs.getDouble(2));
                        } catch (Exception ignored) {}
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT world_key FROM reborn_visited_worlds WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try { d.visited().add(WorldKey.valueOf(rs.getString(1))); } catch (Exception ignored) {}
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, type, remaining, stacks FROM reborn_status WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        d.status().put(rs.getString(1),
                                new PlayerData.StatusEffect(
                                        rs.getString(1), rs.getString(2),
                                        rs.getLong(3), rs.getInt(4)));
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "PlayerData 로드 실패: " + uuid, e);
        }
        d.clearDirty();
        cache.put(uuid, d);
        return d;
    }

    public void save(PlayerData d) {
        if (!d.isDirty()) return;
        try (Connection c = db.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reborn_player (uuid, name, world_key, tier, title_id, clan_id, lineage, " +
                            "dragon_age, deaths, reincarnations, gym_used, child_start, first_join, last_join, playtime) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                            "ON DUPLICATE KEY UPDATE name=VALUES(name), world_key=VALUES(world_key), " +
                            "tier=VALUES(tier), title_id=VALUES(title_id), clan_id=VALUES(clan_id), " +
                            "lineage=VALUES(lineage), dragon_age=VALUES(dragon_age), deaths=VALUES(deaths), " +
                            "reincarnations=VALUES(reincarnations), gym_used=VALUES(gym_used), " +
                            "child_start=VALUES(child_start), last_join=VALUES(last_join), playtime=VALUES(playtime)")) {
                ps.setString(1, d.uuid().toString());
                ps.setString(2, d.name());
                ps.setString(3, d.worldKey().name());
                ps.setString(4, d.tier());
                ps.setString(5, d.titleId());
                ps.setString(6, d.clanId());
                ps.setString(7, d.lineage());
                ps.setInt(8, d.dragonAge());
                ps.setInt(9, d.deaths());
                ps.setInt(10, d.reincarnations());
                ps.setInt(11, d.gymUsed() ? 1 : 0);
                ps.setInt(12, d.childStart() ? 1 : 0);
                ps.setLong(13, d.firstJoin());
                ps.setLong(14, d.lastJoin());
                ps.setLong(15, d.playtime());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reborn_stats (uuid, stat, value) VALUES (?,?,?) " +
                            "ON DUPLICATE KEY UPDATE value=VALUES(value)")) {
                for (var e : d.statsView().entrySet()) {
                    ps.setString(1, d.uuid().toString());
                    ps.setString(2, e.getKey().name());
                    ps.setDouble(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "PlayerData 저장 실패: " + d.uuid(), ex);
        }
        d.clearDirty();
    }

    public void unload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) save(d);
    }

    public void flushAll() {
        for (PlayerData d : cache.values()) save(d);
    }

    public java.util.Collection<PlayerData> all() { return cache.values(); }

    /**
     * 스탯 변경 헬퍼: 이벤트도 함께 발행한다.
     */
    public void addStat(UUID uuid, StatType t, double delta, String source) {
        PlayerData d = get(uuid);
        if (d == null) return;
        double old = d.getStat(t);
        d.addStat(t, delta);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            Bukkit.getPluginManager().callEvent(
                    new RebornStatChangeEvent(p, t, old, d.getStat(t), source));
        }
    }
}
