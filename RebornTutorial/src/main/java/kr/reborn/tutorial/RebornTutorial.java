package kr.reborn.tutorial;

import kr.reborn.core.RebornCore;
import kr.reborn.tutorial.command.TutorialCommand;
import kr.reborn.tutorial.flow.TutorialManager;
import kr.reborn.tutorial.protect.ZoneListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornTutorial extends JavaPlugin {

    private static RebornTutorial instance;
    private TutorialManager manager;
    private kr.reborn.tutorial.quest.TutorialQuestHints hints;

    public static RebornTutorial get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.manager = new TutorialManager(this);
        this.hints = new kr.reborn.tutorial.quest.TutorialQuestHints(this);

        getCommand("rtut").setExecutor(new TutorialCommand(this));
        getServer().getPluginManager().registerEvents(new ZoneListener(this), this);
        getServer().getPluginManager().registerEvents(manager, this);
        getServer().getPluginManager().registerEvents(
                new kr.reborn.tutorial.quest.HintStageListener(this), this);

        // 단계 진행 1초 단위 갱신
        RebornCore.get().scheduler().runTimer(manager::tick, 20L, 20L);

        getLogger().info("RebornTutorial 활성화 — 가이드 시스템 등록");
    }

    public TutorialManager manager() { return manager; }
    public kr.reborn.tutorial.quest.TutorialQuestHints hints() { return hints; }
}
