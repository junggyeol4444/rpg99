package kr.reborn.spawn;

import kr.reborn.core.RebornCore;
import kr.reborn.spawn.command.GymCommand;
import kr.reborn.spawn.command.RaceCommand;
import kr.reborn.spawn.command.RerollCommand;
import kr.reborn.spawn.gym.GymTracker;
import kr.reborn.spawn.join.FirstJoinListener;
import kr.reborn.spawn.race.RaceManager;
import kr.reborn.spawn.roulette.Roulette;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornSpawn extends JavaPlugin {

    private static RebornSpawn instance;
    private Roulette roulette;
    private GymTracker gym;
    private RaceManager races;

    public static RebornSpawn get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.races = new RaceManager(this);
        this.roulette = new Roulette(this);
        this.gym = new GymTracker(this);

        getCommand("reroll").setExecutor(new RerollCommand(this));
        getCommand("gym").setExecutor(new GymCommand(this));
        if (getCommand("race") != null) {
            getCommand("race").setExecutor(new RaceCommand(this));
        }
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        getServer().getPluginManager().registerEvents(gym, this);

        getLogger().info("RebornSpawn 활성화 — 36 종족 등록");
    }

    public Roulette roulette() { return roulette; }
    public GymTracker gym() { return gym; }
    public RaceManager races() { return races; }
}
