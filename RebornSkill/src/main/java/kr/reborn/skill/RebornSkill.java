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
    private kr.reborn.skill.combo.ComboTracker combo;
    private kr.reborn.skill.manual.ManualManager manuals;
    private kr.reborn.skill.school.SchoolManager schools;

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
        this.combo = new kr.reborn.skill.combo.ComboTracker();
        registry.load();
        this.manuals = new kr.reborn.skill.manual.ManualManager(this);
        this.schools = new kr.reborn.skill.school.SchoolManager(this);
        techniques.load();  // 초식 데이터 로드 (techniques.yml)
        creator.load();  // 이전에 창조된 스킬 복원

        getCommand("skill").setExecutor(new SkillCommand(this));
        getCommand("energy").setExecutor(new EnergyCommand(this));
        if (getCommand("manual") != null) {
            getCommand("manual").setExecutor(
                    new kr.reborn.skill.command.ManualCommand(this));
        }
        if (getCommand("school") != null) {
            getCommand("school").setExecutor(
                    new kr.reborn.skill.command.SchoolCommand(this));
        }
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

    /** HiddenClass ConditionEngine이 SKILL_LEARNED 조건 체크에 사용. */
    public boolean hasSkill(UUID id, String skillId) {
        return store.has(id, skillId);
    }

    /**
     * HiddenAbility DUAL_CAST가 호출. 보유한 magic 카테고리 스킬 중 첫 번째 시전.
     * 못 찾으면 false 반환 — 호출자가 fallback 데미지 처리.
     */
    public boolean castFirstMagic(Player p) {
        return castFirstByCategory(p, "MAGIC", "ELEMENTAL", "ARCANE", "DIVINE", "DEMONIC", "SPIRIT");
    }

    /** DUAL_CAST가 호출 — 검술/무공 첫 스킬 시전. */
    public boolean castFirstSword(Player p) {
        return castFirstByCategory(p, "MARTIAL", "SWORD");
    }

    private boolean castFirstByCategory(Player p, String... categories) {
        var owned = store.owned(p.getUniqueId());
        if (owned.isEmpty()) return false;
        for (String sid : owned) {
            var def = registry.get(sid);
            if (def == null || def.category == null) continue;
            for (String cat : categories) {
                if (cat.equalsIgnoreCase(def.category)) {
                    caster.cast(p, sid);
                    return true;
                }
            }
        }
        return false;
    }

    public SkillRegistry registry() { return registry; }
    public PlayerSkillStore store() { return store; }
    public SkillCaster caster() { return caster; }
    public EnergyManager energy() { return energy; }
    public SkillCreator creator() { return creator; }
    public kr.reborn.skill.technique.TechniqueRegistry techniques() { return techniques; }
    public kr.reborn.skill.combo.ComboTracker combo() { return combo; }
    public kr.reborn.skill.manual.ManualManager manuals() { return manuals; }
    public kr.reborn.skill.school.SchoolManager schools() { return schools; }
}
