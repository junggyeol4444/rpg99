package kr.reborn.curse.signature;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;

/**
 * 30+ 축복/저주 시그니처. 각 effectId마다 고유한 시청각 표현.
 */
public final class BlessingSignatureRegistry {

    private static final Map<String, BlessingSignature> SIGS = new HashMap<>();

    static {
        // ─── 축복 (13) ───
        s("goddess_blessing", Particle.HEART, Particle.SPELL_INSTANT,
                Sound.ENTITY_PLAYER_LEVELUP, "&b&l여신의 손길이 너의 영혼을 어루만진다.", 5);
        s("spirit_king", Particle.SPELL_MOB_AMBIENT, Particle.NOTE,
                Sound.BLOCK_AMETHYST_BLOCK_RESONATE, "&a&l정령왕의 인정 — 모든 정령이 너를 따른다.", 8);
        s("dragon_recognition", Particle.DRAGON_BREATH, Particle.FLAME,
                Sound.ENTITY_ENDER_DRAGON_GROWL, "&c&l드래곤이 너를 인정했다 — 비늘의 단단함.", 6);
        s("martial_lord", Particle.CRIT, Particle.SWEEP_ATTACK,
                Sound.ITEM_TRIDENT_THUNDER, "&6&l무림맹주의 격 — 강호가 너의 이름을 안다.", 4);
        s("hermit_teaching", Particle.SPELL_WITCH, Particle.SPELL_INSTANT,
                Sound.BLOCK_BEACON_AMBIENT, "&7&l은둔고수의 비전이 흘러든다 — 내공이 깊어진다.", 10);
        s("unmoving_pillar", Particle.BLOCK_DUST, Particle.LANDING_OBSIDIAN_TEAR,
                Sound.BLOCK_STONE_PLACE, "&6&l부동의 기둥 — 너는 흔들리지 않는다.", 3);
        s("cheonje_grace", Particle.END_ROD, Particle.TOTEM,
                Sound.BLOCK_BELL_RESONATE, "&e&l천제의 가호 — 천기가 너에게 깃든다.", 7);
        s("full_moon_yokai", Particle.SPELL_WITCH, Particle.SOUL_FIRE_FLAME,
                Sound.ENTITY_FOX_AGGRO, "&5&l보름달의 가호 — 요기가 만월처럼 충만하다.", 9);
        s("primordial_light", Particle.END_ROD, Particle.FIREWORKS_SPARK,
                Sound.UI_TOAST_CHALLENGE_COMPLETE, "&f&l태초의 빛이 너를 비춘다 — 모든 어둠을 가른다.", 12);
        s("primordial_dark", Particle.SQUID_INK, Particle.SOUL,
                Sound.ENTITY_WARDEN_AMBIENT, "&0&l태초의 어둠이 너를 감싼다 — 빛이 닿지 않는다.", 12);
        s("sea_god_blessing", Particle.WATER_BUBBLE, Particle.BUBBLE_POP,
                Sound.BLOCK_CONDUIT_AMBIENT, "&3&l해왕의 축복 — 바다가 너의 편이다.", 6);
        s("netbreaker_renown", Particle.ELECTRIC_SPARK, Particle.REDSTONE,
                Sound.BLOCK_PISTON_EXTEND, "&d&lNetBreaker 명성 — 모든 시스템이 너에게 길을 연다.", 4);
        s("abyssal_bond", Particle.SQUID_INK, Particle.SOUL,
                Sound.ENTITY_WARDEN_HEARTBEAT, "&0&l심연의 인장 — 너는 그곳에 속한다.", 8);

        // ─── 저주 (15) ───
        s("qi_deviation", Particle.SPELL_WITCH, Particle.SQUID_INK,
                Sound.ENTITY_WITCH_DRINK, "&5&l주화입마 — 단전이 흐트러진다.", 5);
        s("demon_erosion", Particle.SQUID_INK, Particle.SOUL,
                Sound.ENTITY_WITHER_AMBIENT, "&8&l마기가 정신을 침식한다 …", 6);
        s("divine_punishment", Particle.LAVA, Particle.EXPLOSION_NORMAL,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, "&c&l천벌! 신의 노여움이 떨어진다.", 0);
        s("cyber_psychosis", Particle.ELECTRIC_SPARK, Particle.CRIT_MAGIC,
                Sound.ENTITY_VEX_HURT, "&d&lCyber Psychosis — 신경이 미친다.", 4);
        s("radiation", Particle.SPELL_WITCH, Particle.SOUL_FIRE_FLAME,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME, "&2&l방사능이 세포를 무너뜨린다.", 7);
        s("pact_break_penalty", Particle.SPELL_MOB, Particle.SQUID_INK,
                Sound.ENTITY_VEX_DEATH, "&5&l정령왕의 분노 — 계약을 파기한 대가.", 0);
        s("pirate_curse", Particle.WATER_SPLASH, Particle.SQUID_INK,
                Sound.AMBIENT_UNDERWATER_LOOP, "&3&l해적의 저주 — 바다가 너를 안고 싶어한다.", 3);
        s("poison", Particle.SPELL_WITCH, Particle.SLIME,
                Sound.ENTITY_SPIDER_HURT, "&2독이 혈관을 타고 흐른다.", 4);
        s("yokai_curse", Particle.SOUL_FIRE_FLAME, Particle.SPELL_WITCH,
                Sound.ENTITY_FOX_SCREECH, "&5&l요괴의 저주 — 낮마다 정신이 멀어진다.", 3);
        s("immortal_heavenly_punishment", Particle.LAVA, Particle.SOUL,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, "&c&l선계 천벌 — 수련의 길이 막혔다.", 5);
        s("bloodlust", Particle.REDSTONE, Particle.DRIPPING_LAVA,
                Sound.ENTITY_WITCH_DRINK, "&4&l피의 갈증 — 멈출 수 없다.", 6);
        s("shadow_taint", Particle.SQUID_INK, Particle.SMOKE_NORMAL,
                Sound.ENTITY_PHANTOM_BITE, "&8&l그림자 침식 — 어둠이 점점 깊어진다.", 4);
        s("sleep_curse", Particle.SPELL_WITCH, Particle.HEART,
                Sound.ENTITY_PHANTOM_AMBIENT, "&1&l영원한 잠 — 시전자만이 깨울 수 있다.", 0);
        s("dragon_disrespect", Particle.DRAGON_BREATH, Particle.FLAME,
                Sound.ENTITY_ENDER_DRAGON_HURT, "&c&l용의 분노 — 너를 영원히 기억할 것이다.", 5);
        s("apocalypse_starvation", Particle.SMOKE_LARGE, Particle.BLOCK_DUST,
                Sound.ENTITY_GENERIC_HURT, "&8&l폐허의 굶주림 — 살이 마른다.", 2);
    }

    private static void s(String id, Particle apply, Particle tick, Sound s, String msg, int tickCount) {
        SIGS.put(id, new BlessingSignature(id, apply, tick, s, msg, tickCount));
    }

    public static BlessingSignature lookup(String id) { return SIGS.get(id); }

    public static int size() { return SIGS.size(); }
}
