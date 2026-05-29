package kr.reborn.curse;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Gui;
import kr.reborn.curse.berserk.BerserkEngine;
import kr.reborn.curse.command.BuffCommand;
import kr.reborn.curse.command.CurseCommand;
import kr.reborn.curse.cure.CureEngine;
import kr.reborn.curse.listener.EffectStatusListener;
import kr.reborn.curse.manager.EffectRegistry;
import kr.reborn.curse.manager.PlayerEffectManager;
import kr.reborn.curse.special.SpecialEffectEngine;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornCurse extends JavaPlugin {

    private static RebornCurse instance;

    private EffectRegistry registry;
    private PlayerEffectManager effects;
    private SpecialEffectEngine special;
    private BerserkEngine berserk;
    private CureEngine cure;
    private Gui gui;

    public static RebornCurse get() { return instance; }

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
        this.registry = new EffectRegistry(this);
        this.special = new SpecialEffectEngine(this);
        this.berserk = new BerserkEngine(this);
        this.cure = new CureEngine(this);
        this.effects = new PlayerEffectManager(this);

        getCommand("buff").setExecutor(new BuffCommand(this));
        getCommand("curse").setExecutor(new CurseCommand(this));

        getServer().getPluginManager().registerEvents(new EffectStatusListener(this), this);

        // 1초마다 모든 활성 효과 tick
        RebornCore.get().scheduler().runTimer(() -> effects.tickAll(), 20L, 20L);

        getLogger().info("RebornCurse 활성화: 축복 " + registry.blessings().size()
                + " · 저주 " + registry.curses().size()
                + " — Berserk + Cure + Special 엔진 가동");
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.shutdown();
    }

    public EffectRegistry registry() { return registry; }
    public PlayerEffectManager effects() { return effects; }
    public SpecialEffectEngine special() { return special; }
    public BerserkEngine berserk() { return berserk; }
    public CureEngine cure() { return cure; }
    public Gui gui() { return gui; }
}
