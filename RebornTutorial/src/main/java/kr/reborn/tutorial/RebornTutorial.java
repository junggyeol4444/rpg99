package kr.reborn.tutorial;

import kr.reborn.core.RebornCore;
import kr.reborn.tutorial.command.TutorialCommand;
import kr.reborn.tutorial.flow.TutorialManager;
import kr.reborn.tutorial.protect.ZoneListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornTutorial extends JavaPlugin {

    private static RebornTutorial instance;
    private TutorialManager manager;

    public static RebornTutorial get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.manager = new TutorialManager(this);

        getCommand("rtut").setExecutor(new TutorialCommand(this));
        getServer().getPluginManager().registerEvents(new ZoneListener(this), this);
        getServer().getPluginManager().registerEvents(manager, this);

        // 단계 진행 1초 단위 갱신
        RebornCore.get().scheduler().runTimer(manager::tick, 20L, 20L);

        getLogger().info("RebornTutorial 활성화");
    }

    public TutorialManager manager() { return manager; }
}
