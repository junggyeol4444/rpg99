package kr.reborn.god;

import kr.reborn.god.command.GodCommand;
import kr.reborn.god.manager.DomainManager;
import kr.reborn.god.manager.GodManager;
import kr.reborn.god.manager.ReligionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornGod extends JavaPlugin {

    private static RebornGod instance;
    private GodManager gods;
    private DomainManager domains;
    private ReligionManager religions;

    public static RebornGod get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.gods = new GodManager(this);
        this.domains = new DomainManager(this);
        this.religions = new ReligionManager(this);
        getCommand("god").setExecutor(new GodCommand(this));
        getServer().getPluginManager().registerEvents(
                new kr.reborn.god.listener.GodWorldImpactListener(this), this);
        getLogger().info("RebornGod 활성화");
    }

    public GodManager gods() { return gods; }
    public DomainManager domains() { return domains; }
    public ReligionManager religions() { return religions; }
}
