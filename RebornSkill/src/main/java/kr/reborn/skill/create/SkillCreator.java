package kr.reborn.skill.create;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornSkillLearnEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.def.SkillDef;
import kr.reborn.skill.effect.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 스킬 창조 시스템 (기획서 6장).
 *
 * 플레이어가 같은 행동(patternKey)을 임계치 이상 반복하면 AI가 새 스킬을 "창조"한다.
 * 창조된 스킬은 영구 저장되고, 이후 같은 행동을 임계치까지 반복하는 다른 플레이어에게도
 * 동일하게 지급된다(공유). 서버 재시작 후에도 유지(created-skills.yml).
 */
public final class SkillCreator {

    private static final int THRESHOLD = 100;

    private final RebornSkill plugin;
    /** 플레이어별 행동 패턴 누적 (휘발성). */
    private final Map<UUID, Map<String, Integer>> patternCount = new HashMap<>();
    /** 이미 창조된 패턴 → 스킬 id (영구·공유). */
    private final Map<String, String> createdByPattern = new HashMap<>();

    public SkillCreator(RebornSkill p) { this.plugin = p; }

    private File file() { return new File(plugin.getDataFolder(), "created-skills.yml"); }

    /** 서버 시작 시 — 이전에 창조된 스킬들을 레지스트리에 복원. */
    public void load() {
        createdByPattern.clear();
        File f = file();
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String pattern : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(pattern);
            if (s == null) continue;
            SkillDef def = fromSection(s);
            plugin.registry().register(def);
            createdByPattern.put(pattern, def.id);
        }
        plugin.getLogger().info("창조된 스킬 " + createdByPattern.size() + "종 복원");
    }

    /** 행동 1건 기록. 이미 창조됐으면 공유 지급, 아니면 누적 후 임계치에서 창조. */
    public void log(Player p, String patternKey) {
        String existing = createdByPattern.get(patternKey);
        if (existing != null) {
            grant(p, existing);  // 공유 — 같은 행동을 한 다른 플레이어도 습득
            return;
        }
        var m = patternCount.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>());
        int n = m.merge(patternKey, 1, Integer::sum);
        if (n >= THRESHOLD) {
            m.remove(patternKey);
            createSkill(p, patternKey);
        }
    }

    private void createSkill(Player p, String patternKey) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        WorldKey world = d != null ? d.worldKey() : WorldKey.LOBBY;
        String id = "created_" + patternKey.toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (plugin.registry().exists(id)) { grant(p, id); return; }

        String[] flavor = flavor(patternKey);          // {name, element, type}
        SkillType type = SkillType.valueOf(flavor[2]);
        StatType mainStat = worldMainStat(world);
        double mult = patternKey.startsWith("stealth") ? 4.0 : 2.5;
        String damage = mainStat.name().toLowerCase() + " * " + mult;

        SkillDef def = new SkillDef(id, flavor[0], world, "CREATED",
                StatType.MANA, 0, 3, 0, damage, flavor[1], "CREATE",
                type, type == SkillType.AOE ? 4 : 0, 0,
                type == SkillType.PROJECTILE ? 1.6 : 0, 0, null);
        plugin.registry().register(def);
        createdByPattern.put(patternKey, id);
        persist(patternKey, def);

        grant(p, id);
        Bukkit.broadcastMessage("§5§l[스킬 창조] §f" + p.getName()
                + "이(가) 반복된 수련 끝에 §d" + stripColor(flavor[0]) + "§f을(를) 깨우쳤다!");
    }

    private void grant(Player p, String skillId) {
        if (plugin.store().has(p.getUniqueId(), skillId)) return;
        plugin.store().learn(p.getUniqueId(), skillId);
        Msg.send(p, "&d새 스킬 습득: &f" + skillId);
        Bukkit.getPluginManager().callEvent(new RebornSkillLearnEvent(p, skillId));
    }

    /** patternKey → {표시명, 속성, SkillType}. 특정 패턴 먼저 매칭. */
    private String[] flavor(String key) {
        // 특수 패턴 (먼저)
        if (key.equals("stealth_kill")) return new String[]{"&8그림자 일격", "DARK", "MELEE"};
        if (key.equals("survive_brink")) return new String[]{"&c사선의 반격", "PHYSICAL", "MELEE"};
        // 채굴 패턴
        if (key.equals("mine_pickaxe")) return new String[]{"&7광부의 격타", "PHYSICAL", "MELEE"};
        if (key.equals("mine_axe_mine")) return new String[]{"&6벌목 강타", "PHYSICAL", "MELEE"};
        if (key.equals("mine_shovel")) return new String[]{"&e토류 격", "PHYSICAL", "MELEE"};
        // 제작 패턴
        if (key.equals("craft_weapon")) return new String[]{"&f명검의 일격", "PHYSICAL", "MELEE"};
        if (key.equals("craft_armor")) return new String[]{"&b철벽 자세", "PHYSICAL", "BUFF"};
        if (key.equals("craft_potion")) return new String[]{"&d정수 폭발", "ARCANE", "PROJECTILE"};
        // 무기 사냥 패턴
        if (key.equals("weapon_bow")) return new String[]{"&a연사 궁술", "PHYSICAL", "PROJECTILE"};
        if (key.equals("weapon_axe")) return new String[]{"&c파쇄격", "PHYSICAL", "MELEE"};
        if (key.equals("weapon_trident")) return new String[]{"&b관통 일섬", "PHYSICAL", "MELEE"};
        if (key.equals("weapon_fist")) return new String[]{"&e무형권", "PHYSICAL", "MELEE"};
        return new String[]{"&f자생 검기", "PHYSICAL", "MELEE"};
    }

    private StatType worldMainStat(WorldKey w) {
        switch (w) {
            case FANTASY: return StatType.AURA;
            case DEMON: return StatType.DEMON_KI;
            case HEAVEN: return StatType.HEAVEN_KI;
            case SPIRIT: return StatType.SPIRIT_POWER;
            case MARTIAL: return StatType.INNER_KI;
            case IMMORTAL: return StatType.IMMORTAL_KI;
            case YOKAI: return StatType.YOKAI_KI;
            case DRAGON: return StatType.DRAGON_POWER;
            case OCEAN: return StatType.OCEAN_POWER;
            case MAGITECH: return StatType.MAGITECH_ENERGY;
            case CYBERPUNK: return StatType.CYBER_ADAPTATION;
            default: return StatType.STRENGTH;
        }
    }

    private void persist(String pattern, SkillDef def) {
        File f = file();
        YamlConfiguration y = f.exists() ? YamlConfiguration.loadConfiguration(f) : new YamlConfiguration();
        String b = pattern + ".";
        y.set(b + "id", def.id);
        y.set(b + "name", def.name);
        y.set(b + "world", def.world.name());
        y.set(b + "element", def.element);
        y.set(b + "type", def.type.name());
        y.set(b + "damage", def.damageFormula);
        y.set(b + "radius", def.radius);
        y.set(b + "projectile-speed", def.projectileSpeed);
        try { plugin.getDataFolder().mkdirs(); y.save(f); } catch (Exception ignored) {}
    }

    private SkillDef fromSection(ConfigurationSection s) {
        WorldKey w;
        try { w = WorldKey.valueOf(s.getString("world", "LOBBY")); } catch (Exception e) { w = WorldKey.LOBBY; }
        SkillType type;
        try { type = SkillType.valueOf(s.getString("type", "MELEE")); } catch (Exception e) { type = SkillType.MELEE; }
        return new SkillDef(s.getString("id"), s.getString("name", "창조 스킬"), w, "CREATED",
                StatType.MANA, 0, 3, 0, s.getString("damage", "strength * 2.5"),
                s.getString("element", "PHYSICAL"), "CREATE",
                type, s.getDouble("radius", 0), 0, s.getDouble("projectile-speed", 0), 0, null);
    }

    private String stripColor(String s) { return s == null ? "" : s.replaceAll("&.|§.", ""); }
}
