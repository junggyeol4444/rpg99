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

    public static RebornStat get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.growth = new GrowthRegistry(this);
        this.minigames = new MinigameManager(this);

        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("tierup").setExecutor(new TierUpCommand(this));
        getCommand("meditate").setExecutor(new MeditateCommand(this));
        getCommand("answer").setExecutor(new AnswerCommand(this));

        getServer().getPluginManager().registerEvents(growth, this);

        getLogger().info("RebornStat 활성화 (Core 연결: " + (RebornCore.get() != null) + ")");
    }

    public GrowthRegistry growth() { return growth; }
    public MinigameManager minigames() { return minigames; }
}
