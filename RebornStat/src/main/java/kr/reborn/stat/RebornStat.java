package kr.reborn.stat;

import kr.reborn.core.RebornCore;
import kr.reborn.stat.command.AnswerCommand;
import kr.reborn.stat.command.MeditateCommand;
import kr.reborn.stat.command.StatsCommand;
import kr.reborn.stat.command.TierUpCommand;
import kr.reborn.stat.growth.GrowthRegistry;
import kr.reborn.stat.minigame.MinigameManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornStat extends JavaPlugin {

    private static RebornStat instance;
    private GrowthRegistry growth;
    private MinigameManager minigames;
    private kr.reborn.stat.serendipity.FortuneRegistry fortunes;
    private kr.reborn.stat.serendipity.FortuneManager fortuneManager;

    public static RebornStat get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.growth = new GrowthRegistry(this);
        this.minigames = new MinigameManager(this);
        this.fortunes = new kr.reborn.stat.serendipity.FortuneRegistry(this);
        this.fortunes.load();
        this.fortuneManager = new kr.reborn.stat.serendipity.FortuneManager(this, fortunes);

        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("tierup").setExecutor(new TierUpCommand(this));
        getCommand("meditate").setExecutor(new MeditateCommand(this));
        getCommand("answer").setExecutor(new AnswerCommand(this));
        getCommand("fortune").setExecutor(new kr.reborn.stat.serendipity.FortuneCommand(this));

        getServer().getPluginManager().registerEvents(growth, this);
        getServer().getPluginManager().registerEvents(
                new kr.reborn.stat.minigame.InputListener(this), this);
        getServer().getPluginManager().registerEvents(fortuneManager, this);

        getLogger().info("RebornStat 활성화 (Core 연결: " + (RebornCore.get() != null) + ")");
    }

    public GrowthRegistry growth() { return growth; }
    public MinigameManager minigames() { return minigames; }
    public kr.reborn.stat.serendipity.FortuneRegistry fortunes() { return fortunes; }
    public kr.reborn.stat.serendipity.FortuneManager fortuneManager() { return fortuneManager; }
}
