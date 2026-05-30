package kr.reborn.core;

import kr.reborn.core.api.RebornAPI;
import kr.reborn.core.command.CoreCommand;
import kr.reborn.core.data.DataManager;
import kr.reborn.core.data.PlayerDataListener;
import kr.reborn.core.db.Database;
import kr.reborn.core.scheduler.RebornScheduler;
import kr.reborn.core.tier.TierManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornCore extends JavaPlugin {

    private static RebornCore instance;

    private Database database;
    private DataManager dataManager;
    private TierManager tierManager;
    private RebornScheduler scheduler;
    private RebornAPI api;

    public static RebornCore get() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.scheduler = RebornScheduler.detect(this);
        this.database = new Database(this);
        try {
            database.connect();
            database.bootstrapTables();
        } catch (Exception e) {
            getLogger().severe("DB 연결 실패: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.dataManager = new DataManager(this, database);
        this.tierManager = new TierManager(this);
        this.api = new RebornAPI(this);

        getServer().getPluginManager().registerEvents(new PlayerDataListener(dataManager), this);

        getCommand("reborncore").setExecutor(new CoreCommand(this));
        if (getCommand("dashboard") != null) {
            getCommand("dashboard").setExecutor(new kr.reborn.core.command.DashboardCommand(this));
        }

        long interval = getConfig().getLong("auto-save-interval", 300L) * 20L;
        scheduler.runTimerAsync(() -> dataManager.flushAll(), interval, interval);

        getLogger().info("RebornCore 활성화 완료 (Folia=" + scheduler.isFolia() + ")");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.flushAll();
        if (database != null) database.shutdown();
    }

    public Database database() { return database; }
    public DataManager dataManager() { return dataManager; }
    public TierManager tierManager() { return tierManager; }
    public RebornScheduler scheduler() { return scheduler; }
    public RebornAPI api() { return api; }
}
