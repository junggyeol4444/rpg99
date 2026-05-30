package kr.reborn.title;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Gui;
import kr.reborn.title.command.RankingCommand;
import kr.reborn.title.command.TitleCommand;
import kr.reborn.title.listener.TitleProgressListener;
import kr.reborn.title.manager.RankingManager;
import kr.reborn.title.manager.TitleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornTitle extends JavaPlugin {

    private static RebornTitle instance;

    private TitleManager titles;
    private RankingManager rankings;
    private Gui gui;

    public static RebornTitle get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (RebornCore.get() == null) {
            getLogger().severe("RebornCore가 비활성. RebornTitle 비활성화.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gui = new Gui(this);
        this.titles = new TitleManager(this);
        this.rankings = new RankingManager(this);

        getCommand("title").setExecutor(new TitleCommand(this));
        getCommand("ranking").setExecutor(new RankingCommand(this));
        getServer().getPluginManager().registerEvents(new TitleProgressListener(this), this);
        getServer().getPluginManager().registerEvents(
                new kr.reborn.title.listener.TitleAutoGrantListener(this), this);

        long refresh = getConfig().getLong("ranking.refresh-minutes", 5L) * 60L * 20L;
        RebornCore.get().scheduler().runTimerAsync(() -> rankings.refresh(), refresh, refresh);

        getLogger().info("RebornTitle 활성화: 칭호 " + titles.all().size() + "종");
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.shutdown();
    }

    public TitleManager titles() { return titles; }
    public RankingManager rankings() { return rankings; }
    public Gui gui() { return gui; }
}
