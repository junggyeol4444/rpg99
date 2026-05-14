package kr.reborn.spawn;

import kr.reborn.core.RebornCore;
import kr.reborn.spawn.command.GymCommand;
import kr.reborn.spawn.command.RerollCommand;
import kr.reborn.spawn.gym.GymTracker;
import kr.reborn.spawn.join.FirstJoinListener;
import kr.reborn.spawn.roulette.Roulette;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornSpawn extends JavaPlugin {

    private static RebornSpawn instance;
    private Roulette roulette;
    private GymTracker gym;

    public static RebornSpawn get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.roulette = new Roulette(this);
        this.gym = new GymTracker(this);

        getCommand("reroll").setExecutor(new RerollCommand(this));
        getCommand("gym").setExecutor(new GymCommand(this));
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        getServer().getPluginManager().registerEvents(gym, this);

        getLogger().info("RebornSpawn 활성화");
    }

    public Roulette roulette() { return roulette; }
    public GymTracker gym() { return gym; }
}
