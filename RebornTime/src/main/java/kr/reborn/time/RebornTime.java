package kr.reborn.time;

import kr.reborn.core.RebornCore;
import kr.reborn.time.chamber.TimeChamber;
import kr.reborn.time.command.ChamberCommand;
import kr.reborn.time.command.TimeCommand;
import kr.reborn.time.command.WorldTravelCommand;
import kr.reborn.time.sync.RealtimeSync;
import kr.reborn.time.travel.WorldTravelManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornTime extends JavaPlugin {

    private static RebornTime instance;
    private RealtimeSync sync;
    private WorldTravelManager travel;
    private TimeChamber chamber;

    public static RebornTime get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.sync = new RealtimeSync(this);
        this.travel = new WorldTravelManager(this);
        this.chamber = new TimeChamber(this);

        getCommand("worldtravel").setExecutor(new WorldTravelCommand(this));
        getCommand("time").setExecutor(new TimeCommand(this));
        getCommand("chamber").setExecutor(new ChamberCommand(this));

        long interval = getConfig().getLong("sync-interval-seconds", 60) * 20L;
        RebornCore.get().scheduler().runTimer(sync::syncAll, interval, interval);

        getLogger().info("RebornTime 활성화");
    }

    public RealtimeSync sync() { return sync; }
    public WorldTravelManager travel() { return travel; }
    public TimeChamber chamber() { return chamber; }
}
