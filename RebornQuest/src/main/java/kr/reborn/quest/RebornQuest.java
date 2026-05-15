package kr.reborn.quest;

import kr.reborn.quest.command.EventChoiceCommand;
import kr.reborn.quest.command.EventStartCommand;
import kr.reborn.quest.command.QuestCommand;
import kr.reborn.quest.contrib.ContributionTracker;
import kr.reborn.quest.engine.QuestEngine;
import kr.reborn.quest.engine.QuestRegistry;
import kr.reborn.quest.event.EventTree;
import kr.reborn.quest.listener.QuestListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornQuest extends JavaPlugin {

    private static RebornQuest instance;
    private QuestRegistry registry;
    private QuestEngine engine;
    private ContributionTracker contrib;
    private EventTree events;

    public static RebornQuest get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new QuestRegistry(this);
        this.engine = new QuestEngine(this);
        this.contrib = new ContributionTracker();
        this.events = new EventTree(this);
        registry.load();

        getCommand("quest").setExecutor(new QuestCommand(this));
        getCommand("event").setExecutor(new EventStartCommand(this));
        getCommand("eventchoice").setExecutor(new EventChoiceCommand(this));
        getServer().getPluginManager().registerEvents(new QuestListener(this), this);

        getLogger().info("RebornQuest 활성화 — " + registry.all().size() + "개");
    }

    public QuestRegistry registry() { return registry; }
    public QuestEngine engine() { return engine; }
    public ContributionTracker contrib() { return contrib; }
    public EventTree events() { return events; }
}
