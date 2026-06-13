package kr.reborn.clan;

import kr.reborn.clan.command.*;
import kr.reborn.clan.manager.ClanManager;
import kr.reborn.clan.manager.KingdomManager;
import kr.reborn.clan.manager.MarriageManager;
import kr.reborn.clan.manager.TerritoryManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornClan extends JavaPlugin {

    private static RebornClan instance;
    private ClanManager clans;
    private MarriageManager marriages;
    private TerritoryManager territories;
    private KingdomManager kingdoms;
    private kr.reborn.clan.power.PowerEngine powers;
    private kr.reborn.clan.war.ClanWarManager wars;
    private kr.reborn.clan.inheritance.InheritanceManager inheritance;

    public static RebornClan get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.clans = new ClanManager(this);
        this.marriages = new MarriageManager(this);
        this.territories = new TerritoryManager(this);
        this.kingdoms = new KingdomManager(this);
        this.powers = new kr.reborn.clan.power.PowerEngine(this);
        this.wars = new kr.reborn.clan.war.ClanWarManager(this);
        this.inheritance = new kr.reborn.clan.inheritance.InheritanceManager(this);

        getCommand("clan").setExecutor(new ClanCommand(this));
        getCommand("marry").setExecutor(new MarryCommand(this));
        getCommand("divorce").setExecutor(new DivorceCommand(this));
        getCommand("child").setExecutor(new ChildCommand(this));
        getCommand("territory").setExecutor(new TerritoryCommand(this));
        getCommand("kingdom").setExecutor(new KingdomCommand(this));

        getServer().getPluginManager().registerEvents(marriages, this);
        getServer().getPluginManager().registerEvents(territories, this);
        getServer().getPluginManager().registerEvents(wars, this);
        getServer().getPluginManager().registerEvents(inheritance, this);
        getServer().getPluginManager().registerEvents(
                new kr.reborn.clan.inheritance.LineageEffectListener(this), this);
        getServer().getPluginManager().registerEvents(
                new kr.reborn.clan.listener.ClanWorldImpactListener(this), this);

        // 5분마다 자동 영속화 — Calendar 분기 축제로 +1000된 treasury가
        // 크래시 시 손실되지 않도록
        kr.reborn.core.RebornCore.get().scheduler().runTimerAsync(() -> {
            if (clans != null) try { clans.saveAll(); } catch (Throwable ignored) {}
            if (territories != null) try { territories.save(); } catch (Throwable ignored) {}
            if (kingdoms != null) try { kingdoms.saveAll(); } catch (Throwable ignored) {}
        }, 6000L, 6000L);

        getLogger().info("RebornClan 활성화");
    }

    @Override
    public void onDisable() {
        if (clans != null) clans.saveAll();
        if (marriages != null) marriages.save();
        if (territories != null) territories.save();
        if (kingdoms != null) kingdoms.saveAll();
    }

    public ClanManager clans() { return clans; }
    public MarriageManager marriages() { return marriages; }
    public TerritoryManager territories() { return territories; }
    public KingdomManager kingdoms() { return kingdoms; }
    public kr.reborn.clan.power.PowerEngine powers() { return powers; }
    public kr.reborn.clan.war.ClanWarManager wars() { return wars; }
    public kr.reborn.clan.inheritance.InheritanceManager inheritance() { return inheritance; }
}
