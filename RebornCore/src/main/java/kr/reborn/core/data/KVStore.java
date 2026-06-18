package kr.reborn.core.data;

import kr.reborn.core.RebornCore;
import kr.reborn.core.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 일반 키-값 저장소 — 다른 플러그인이 자기 데이터를 영속화할 때 사용.
 *
 * 테이블 구조: reborn_kv(namespace, owner, key, value, updated_at)
 *   - namespace: 플러그인 이름 (예: "RebornEconomy.bank")
 *   - owner: UUID (플레이어 또는 가문) — global은 빈 문자열
 *   - key: namespace 안에서 유일한 키 (예: "GOLD_COIN.deposit")
 *   - value: 직렬화된 문자열
 *
 * 사용 예:
 *   plugin.kv().put("RebornEconomy.bank", playerUuid, "deposit:GOLD_COIN", "12345");
 *   String v = plugin.kv().get("RebornEconomy.bank", playerUuid, "deposit:GOLD_COIN");
 */
public final class KVStore {

    private final RebornCore plugin;
    private final Database db;

    public KVStore(RebornCore plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
        try {
            bootstrap();
        } catch (SQLException e) {
            plugin.getLogger().severe("KVStore 테이블 생성 실패: " + e.getMessage());
        }
    }

    private void bootstrap() throws SQLException {
        try (Connection c = db.get(); var st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_kv (" +
                    "namespace VARCHAR(64) NOT NULL, " +
                    "owner VARCHAR(36) NOT NULL DEFAULT '', " +
                    "k VARCHAR(128) NOT NULL, " +
                    "v TEXT, " +
                    "updated_at BIGINT NOT NULL, " +
                    "PRIMARY KEY (namespace, owner, k))");
        }
    }

    public void put(String namespace, UUID owner, String key, String value) {
        String o = owner == null ? "" : owner.toString();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "REPLACE INTO reborn_kv (namespace, owner, k, v, updated_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, namespace);
            ps.setString(2, o);
            ps.setString(3, key);
            ps.setString(4, value);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("KVStore.put 실패: " + e.getMessage());
        }
    }

    public String get(String namespace, UUID owner, String key) {
        String o = owner == null ? "" : owner.toString();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT v FROM reborn_kv WHERE namespace=? AND owner=? AND k=?")) {
            ps.setString(1, namespace);
            ps.setString(2, o);
            ps.setString(3, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("v");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("KVStore.get 실패: " + e.getMessage());
        }
        return null;
    }

    public void remove(String namespace, UUID owner, String key) {
        String o = owner == null ? "" : owner.toString();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM reborn_kv WHERE namespace=? AND owner=? AND k=?")) {
            ps.setString(1, namespace);
            ps.setString(2, o);
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("KVStore.remove 실패: " + e.getMessage());
        }
    }

    /** namespace + owner의 모든 KV 로드. */
    public Map<String, String> loadAll(String namespace, UUID owner) {
        Map<String, String> map = new HashMap<>();
        String o = owner == null ? "" : owner.toString();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT k, v FROM reborn_kv WHERE namespace=? AND owner=?")) {
            ps.setString(1, namespace);
            ps.setString(2, o);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("k"), rs.getString("v"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("KVStore.loadAll 실패: " + e.getMessage());
        }
        return map;
    }

    /** namespace 전체 (모든 owner) 로드 — global 데이터에 유용. */
    public Map<String, Map<String, String>> loadNamespace(String namespace) {
        Map<String, Map<String, String>> result = new HashMap<>();
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT owner, k, v FROM reborn_kv WHERE namespace=?")) {
            ps.setString(1, namespace);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.computeIfAbsent(rs.getString("owner"), x -> new HashMap<>())
                            .put(rs.getString("k"), rs.getString("v"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("KVStore.loadNamespace 실패: " + e.getMessage());
        }
        return result;
    }

    /* ───────── 편의 메서드 ───────── */

    public void putLong(String ns, UUID owner, String k, long v) {
        put(ns, owner, k, String.valueOf(v));
    }
    public long getLong(String ns, UUID owner, String k, long def) {
        String s = get(ns, owner, k);
        if (s == null) return def;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }
    public void putInt(String ns, UUID owner, String k, int v) {
        put(ns, owner, k, String.valueOf(v));
    }
    public int getInt(String ns, UUID owner, String k, int def) {
        String s = get(ns, owner, k);
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
    public void putDouble(String ns, UUID owner, String k, double v) {
        put(ns, owner, k, String.valueOf(v));
    }
    public double getDouble(String ns, UUID owner, String k, double def) {
        String s = get(ns, owner, k);
        if (s == null) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }
}
