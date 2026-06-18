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
    private kr.reborn.mob.ai.MobController controller;
    private kr.reborn.mob.dungeon.DungeonManager dungeons;

    public static RebornMob get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new MobRegistry(this);
        this.bosses = new BossManager(this);
        this.controller = new kr.reborn.mob.ai.MobController(this);
        registry.load();
        this.dungeons = new kr.reborn.mob.dungeon.DungeonManager(this);

        getCommand("rmob").setExecutor(new MobCommand(this));
        if (getCommand("dungeon") != null) {
            getCommand("dungeon").setExecutor(
                    new kr.reborn.mob.command.DungeonCommand(this));
        }

        long tick = getConfig().getLong("spawn-tick-interval", 100L);
        SpawnTicker ticker = new SpawnTicker(this);
        RebornCore.get().scheduler().runTimer(ticker::run, tick, tick);
        long aiTick = getConfig().getLong("ai-tick-interval", 20L);
        RebornCore.get().scheduler().runTimer(controller::tick, aiTick, aiTick);
        getServer().getPluginManager().registerEvents(new kr.reborn.mob.listener.MobListener(this), this);

        getLogger().info("RebornMob 활성화");
    }

    public MobRegistry registry() { return registry; }
    public BossManager bosses() { return bosses; }
    public kr.reborn.mob.ai.MobController controller() { return controller; }
    public kr.reborn.mob.dungeon.DungeonManager dungeons() { return dungeons; }
}
