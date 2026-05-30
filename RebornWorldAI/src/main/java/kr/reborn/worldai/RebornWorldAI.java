package kr.reborn.worldai;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.ai.WorldAI;
import kr.reborn.worldai.command.WorldAICommand;
import kr.reborn.worldai.comm.AIComm;
import kr.reborn.worldai.disaster.DisasterEngine;
import kr.reborn.worldai.faction.FactionDynamics;
import kr.reborn.worldai.history.EpochManager;
import kr.reborn.worldai.history.WorldHistory;
import kr.reborn.worldai.listener.DisasterWeatherListener;
import kr.reborn.worldai.market.MarketSimulator;
import kr.reborn.worldai.migration.MigrationEngine;
import kr.reborn.worldai.sim.NpcSimulator;
import kr.reborn.worldai.weather.WeatherEngine;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

public final class RebornWorldAI extends JavaPlugin {

    private static RebornWorldAI instance;
    private final Map<WorldKey, WorldAI> ais = new EnumMap<>(WorldKey.class);
    private AIComm comm;
    private NpcSimulator simulator;
    private FactionDynamics factions;
    private MarketSimulator market;
    private MigrationEngine migration;
    private WorldHistory history;
    private EpochManager epoch;
    private DisasterEngine disasters;
    private WeatherEngine weather;

    public static RebornWorldAI get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.comm = new AIComm(this);
        this.history = new WorldHistory(this);
        this.epoch = new EpochManager(this);
        this.factions = new FactionDynamics(this);
        this.market = new MarketSimulator(this);
        this.migration = new MigrationEngine(this);
        this.disasters = new DisasterEngine(this);
        this.weather = new WeatherEngine(this);
        this.simulator = new NpcSimulator(this);

        getServer().getPluginManager().registerEvents(new DisasterWeatherListener(this), this);

        var sec = getConfig().getConfigurationSection("worlds");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                if (!sec.getBoolean(key)) continue;
                try {
                    WorldKey wk = WorldKey.valueOf(key);
                    ais.put(wk, new WorldAI(this, wk));
                } catch (Exception ignored) {}
            }
        }

        getCommand("worldai").setExecutor(new WorldAICommand(this));
        if (getCommand("world") != null) {
            getCommand("world").setExecutor(
                    new kr.reborn.worldai.command.WorldOverviewCommand(this));
        }

        long tick = getConfig().getLong("analysis-tick-interval", 6000L);
        RebornCore.get().scheduler().runTimerAsync(this::tickAll, tick, tick);

        // 이주는 별도 간격 (config: migration-tick-interval, 기본 = AI tick * 2)
        long migTick = getConfig().getLong("migration-tick-interval", tick * 2);
        RebornCore.get().scheduler().runTimerAsync(this::tickMigration, migTick, migTick);

        getLogger().info("RebornWorldAI 활성화 — " + ais.size() + " 세계 AI, "
                + factions.all().values().stream().mapToInt(java.util.Map::size).sum()
                + " 세력 추적, 시장·이주·재해·날씨 엔진 가동");
    }

    private void tickAll() {
        for (WorldAI ai : ais.values()) {
            try { ai.cycle(); }
            catch (Exception e) { getLogger().warning("[" + ai.world() + "] 사이클 오류: " + e.getMessage()); }
        }
    }

    private void tickMigration() {
        try { migration.cycle(); }
        catch (Throwable t) { getLogger().warning("이주 사이클 오류: " + t.getMessage()); }
    }

    public WorldAI of(WorldKey w) { return ais.get(w); }
    public java.util.Collection<WorldAI> all() { return ais.values(); }
    public AIComm comm() { return comm; }
    public NpcSimulator simulator() { return simulator; }
    public FactionDynamics factions() { return factions; }
    public MarketSimulator market() { return market; }
    public MigrationEngine migration() { return migration; }
    public WorldHistory history() { return history; }
    public EpochManager epoch() { return epoch; }
    public DisasterEngine disasters() { return disasters; }
    public WeatherEngine weather() { return weather; }
}
