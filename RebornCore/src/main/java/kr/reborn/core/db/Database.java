package kr.reborn.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kr.reborn.core.RebornCore;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private final RebornCore plugin;
    private HikariDataSource dataSource;

    public Database(RebornCore plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("database");
        HikariConfig cfg = new HikariConfig();
        String type = s.getString("type", "mysql");
        if ("mysql".equalsIgnoreCase(type)) {
            cfg.setJdbcUrl("jdbc:mysql://" + s.getString("host") + ":" + s.getInt("port") + "/"
                    + s.getString("database") + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            cfg.setUsername(s.getString("user"));
            cfg.setPassword(s.getString("password"));
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            cfg.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/reborn.db");
            cfg.setDriverClassName("org.sqlite.JDBC");
        }
        cfg.setMaximumPoolSize(s.getInt("pool-size", 10));
        cfg.setPoolName("RebornHikari");
        this.dataSource = new HikariDataSource(cfg);
    }

    public Connection get() throws SQLException {
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null) dataSource.close();
    }

    public void bootstrapTables() throws SQLException {
        try (Connection c = get(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_player (" +
                    "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(32), world_key VARCHAR(48), " +
                    "tier VARCHAR(48), title_id VARCHAR(48), clan_id VARCHAR(48), " +
                    "lineage VARCHAR(48), dragon_age INT DEFAULT 0, deaths INT DEFAULT 0, " +
                    "reincarnations INT DEFAULT 0, gym_used TINYINT(1) DEFAULT 0, " +
                    "child_start TINYINT(1) DEFAULT 0, first_join BIGINT DEFAULT 0, " +
                    "last_join BIGINT DEFAULT 0, playtime BIGINT DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_stats (" +
                    "uuid VARCHAR(36), stat VARCHAR(32), value DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY(uuid, stat))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_visited_worlds (" +
                    "uuid VARCHAR(36), world_key VARCHAR(48), first_visit BIGINT, " +
                    "PRIMARY KEY(uuid, world_key))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_status (" +
                    "uuid VARCHAR(36), id VARCHAR(64), type VARCHAR(16), " +
                    "remaining BIGINT, stacks INT DEFAULT 1, " +
                    "PRIMARY KEY(uuid, id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_skills (" +
                    "uuid VARCHAR(36), skill_id VARCHAR(64), proficiency INT DEFAULT 0, " +
                    "slot INT DEFAULT -1, PRIMARY KEY(uuid, skill_id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_titles (" +
                    "uuid VARCHAR(36), title_id VARCHAR(64), is_main TINYINT(1) DEFAULT 0, " +
                    "obtained BIGINT, PRIMARY KEY(uuid, title_id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_quests (" +
                    "uuid VARCHAR(36), quest_id VARCHAR(64), state VARCHAR(16), " +
                    "progress TEXT, started BIGINT, " +
                    "PRIMARY KEY(uuid, quest_id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reborn_money (" +
                    "uuid VARCHAR(36), currency VARCHAR(32), balance DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY(uuid, currency))");
        }
    }
}
