package kr.reborn.mob;

import kr.reborn.core.RebornCore;
import kr.reborn.mob.boss.BossManager;
import kr.reborn.mob.command.MobCommand;
import kr.reborn.mob.def.MobRegistry;
import kr.reborn.mob.spawn.SpawnTicker;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornMob extends JavaPlugin {

    private static RebornMob instance;
    private MobRegistry registry;
    private BossManager bosses;

    public static RebornMob get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new MobRegistry(this);
        this.bosses = new BossManager(this);
        registry.load();

        getCommand("rmob").setExecutor(new MobCommand(this));

        long tick = getConfig().getLong("spawn-tick-interval", 100L);
        SpawnTicker ticker = new SpawnTicker(this);
        RebornCore.get().scheduler().runTimer(ticker::run, tick, tick);
        getServer().getPluginManager().registerEvents(new kr.reborn.mob.listener.MobListener(this), this);

        getLogger().info("RebornMob 활성화");
    }

    public MobRegistry registry() { return registry; }
    public BossManager bosses() { return bosses; }
}
