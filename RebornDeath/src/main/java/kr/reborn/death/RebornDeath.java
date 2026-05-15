package kr.reborn.death;

import kr.reborn.core.RebornCore;
import kr.reborn.death.abyss.AbyssWorld;
import kr.reborn.death.command.BountyCommand;
import kr.reborn.death.command.DeathCommand;
import kr.reborn.death.command.UnderworldCommand;
import kr.reborn.death.crime.CrimeManager;
import kr.reborn.death.listener.DeathListener;
import kr.reborn.death.underworld.UnderworldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornDeath extends JavaPlugin {

    private static RebornDeath instance;
    private UnderworldManager underworld;
    private CrimeManager crime;

    public static RebornDeath get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.underworld = new UnderworldManager(this);
        this.crime = new CrimeManager(this);

        getCommand("death").setExecutor(new DeathCommand(this));
        getCommand("underworld").setExecutor(new UnderworldCommand(this));
        getCommand("bounty").setExecutor(new BountyCommand(this));

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new AbyssWorld(this), this);

        // 범죄 시간 감쇠
        RebornCore.get().scheduler().runTimer(crime::tickHourlyDecay, 20L * 60 * 60, 20L * 60 * 60);

        getLogger().info("RebornDeath 활성화");
    }

    public UnderworldManager underworld() { return underworld; }
    public CrimeManager crime() { return crime; }
}
