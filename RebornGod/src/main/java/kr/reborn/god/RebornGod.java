package kr.reborn.god;

import kr.reborn.core.RebornCore;
import kr.reborn.god.command.GodCommand;
import kr.reborn.god.faith.FaithEngine;
import kr.reborn.god.manager.DomainManager;
import kr.reborn.god.manager.GodManager;
import kr.reborn.god.manager.ReligionManager;
import kr.reborn.god.miracle.MiracleEngine;
import kr.reborn.god.trial.TrialManager;
import kr.reborn.god.war.DivineWarManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornGod extends JavaPlugin {

    private static RebornGod instance;
    private GodManager gods;
    private DomainManager domains;
    private ReligionManager religions;
    private FaithEngine faith;
    private MiracleEngine miracles;
    private TrialManager trials;
    private DivineWarManager wars;

    public static RebornGod get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.gods = new GodManager(this);
        this.domains = new DomainManager(this);
        // 신앙 → 신성 변환 엔진 (religion보다 먼저 — religion이 faith를 호출)
        this.faith = new FaithEngine(this);
        this.religions = new ReligionManager(this);
        this.miracles = new MiracleEngine(this);
        this.trials = new TrialManager(this);
        this.wars = new DivineWarManager(this);

        getCommand("god").setExecutor(new GodCommand(this));
        getServer().getPluginManager().registerEvents(
                new kr.reborn.god.listener.GodWorldImpactListener(this), this);
        getServer().getPluginManager().registerEvents(trials, this);
        getServer().getPluginManager().registerEvents(wars, this);

        // 신앙 → 신성 변환 1분마다
        RebornCore.get().scheduler().runTimer(faith::tick, 1200L, 1200L);

        getLogger().info("RebornGod 활성화 — NPC신 " + gods.npcAll().size()
                + " 교단 " + religions.all().size());
    }

    public GodManager gods() { return gods; }
    public DomainManager domains() { return domains; }
    public ReligionManager religions() { return religions; }
    public FaithEngine faith() { return faith; }
    public MiracleEngine miracles() { return miracles; }
    public TrialManager trials() { return trials; }
    public DivineWarManager wars() { return wars; }
}
