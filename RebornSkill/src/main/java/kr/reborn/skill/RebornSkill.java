package kr.reborn.skill;

import kr.reborn.core.RebornCore;
import kr.reborn.core.event.RebornSkillLearnEvent;
import kr.reborn.skill.cast.SkillCaster;
import kr.reborn.skill.command.EnergyCommand;
import kr.reborn.skill.command.SkillCommand;
import kr.reborn.skill.create.SkillCreator;
import kr.reborn.skill.def.SkillRegistry;
import kr.reborn.skill.energy.EnergyManager;
import kr.reborn.skill.player.PlayerSkillStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class RebornSkill extends JavaPlugin {

    private static RebornSkill instance;

    private SkillRegistry registry;
    private PlayerSkillStore store;
    private SkillCaster caster;
    private EnergyManager energy;
    private SkillCreator creator;
    private kr.reborn.skill.technique.TechniqueRegistry techniques;

    public static RebornSkill get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new SkillRegistry(this);
        this.store = new PlayerSkillStore(this);
        this.techniques = new kr.reborn.skill.technique.TechniqueRegistry(this);
        this.caster = new SkillCaster(this);
        this.energy = new EnergyManager(this);
        this.creator = new SkillCreator(this);
        registry.load();
        techniques.load();  // 초식 데이터 로드 (techniques.yml)
        creator.load();  // 이전에 창조된 스킬 복원

        getCommand("skill").setExecutor(new SkillCommand(this));
        getCommand("energy").setExecutor(new EnergyCommand(this));
        getServer().getPluginManager().registerEvents(
                new kr.reborn.skill.effect.SkillProjectileListener(this), this);
        getServer().getPluginManager().registerEvents(
                new kr.reborn.skill.create.SkillCreationListener(this), this);

        long tick = getConfig().getLong("energy-recover-ticks", 20L);
        RebornCore.get().scheduler().runTimer(energy::tickAll, tick, tick);

        getLogger().info("RebornSkill 활성화 — " + registry.all().size() + "개 스킬");
    }

    /** RebornTutorial이 reflection으로 호출. */
    public void learnByApi(UUID id, String skillId) {
        var def = registry.get(skillId);
        if (def == null) return;
        store.learn(id, skillId);
        var p = Bukkit.getPlayer(id);
        if (p != null) Bukkit.getPluginManager().callEvent(new RebornSkillLearnEvent(p, skillId));
    }

    public SkillRegistry registry() { return registry; }
    public PlayerSkillStore store() { return store; }
    public SkillCaster caster() { return caster; }
    public EnergyManager energy() { return energy; }
    public SkillCreator creator() { return creator; }
    public kr.reborn.skill.technique.TechniqueRegistry techniques() { return techniques; }
}
