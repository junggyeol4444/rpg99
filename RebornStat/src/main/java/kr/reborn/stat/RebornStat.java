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
        if (getCommand("transform") != null) {
            getCommand("transform").setExecutor(new kr.reborn.stat.command.TransformCommand(this));
        }
        if (getCommand("moonritual") != null) {
            getCommand("moonritual").setExecutor(new kr.reborn.stat.command.MoonRitualCommand(this));
        }
        if (getCommand("pray") != null) {
            getCommand("pray").setExecutor(new kr.reborn.stat.command.PrayCommand(this));
        }
        if (getCommand("petition") != null) {
            getCommand("petition").setExecutor(new kr.reborn.stat.command.PetitionCommand(this));
        }
        if (getCommand("stabilize") != null) {
            getCommand("stabilize").setExecutor(new kr.reborn.stat.command.StabilizeCommand(this));
        }
        if (getCommand("arraymeditate") != null) {
            getCommand("arraymeditate").setExecutor(new kr.reborn.stat.command.ArrayMeditateCommand(this));
        }
        if (getCommand("element") != null) {
            getCommand("element").setExecutor(new kr.reborn.stat.command.ElementCommand(this));
        }
        if (getCommand("corp") != null) {
            getCommand("corp").setExecutor(new kr.reborn.stat.command.CorpCommand(this));
        }
        if (getCommand("empire") != null) {
            getCommand("empire").setExecutor(new kr.reborn.stat.command.EmpireCommand(this));
        }

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
