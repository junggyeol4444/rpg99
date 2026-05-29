package kr.reborn.hiddenclass;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Gui;
import kr.reborn.hiddenclass.ability.AbilityEngine;
import kr.reborn.hiddenclass.command.HiddenClassCommand;
import kr.reborn.hiddenclass.listener.AbilityListener;
import kr.reborn.hiddenclass.listener.ConditionListener;
import kr.reborn.hiddenclass.manager.ConditionEngine;
import kr.reborn.hiddenclass.manager.HiddenClassRegistry;
import kr.reborn.hiddenclass.manager.PlayerProgress;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornHiddenClass extends JavaPlugin {

    private static RebornHiddenClass instance;

    private HiddenClassRegistry registry;
    private ConditionEngine engine;
    private PlayerProgress progress;
    private AbilityEngine abilities;
    private Gui gui;

    public static RebornHiddenClass get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (RebornCore.get() == null) {
            getLogger().severe("RebornCore가 비활성. 비활성화.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gui = new Gui(this);
        this.registry = new HiddenClassRegistry(this);
        this.progress = new PlayerProgress(this);
        this.engine = new ConditionEngine(this);
        this.abilities = new AbilityEngine(this);

        getCommand("hiddenclass").setExecutor(new HiddenClassCommand(this));
        getServer().getPluginManager().registerEvents(new ConditionListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);

        getLogger().info("RebornHiddenClass 활성화: 클래스 " + registry.all().size()
                + "종, 능력 " + kr.reborn.hiddenclass.ability.HiddenAbility.values().length + "종");
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.shutdown();
    }

    public HiddenClassRegistry registry() { return registry; }
    public ConditionEngine engine() { return engine; }
    public PlayerProgress progress() { return progress; }
    public AbilityEngine abilities() { return abilities; }
    public Gui gui() { return gui; }
}
