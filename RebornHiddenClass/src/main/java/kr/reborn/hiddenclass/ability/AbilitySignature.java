package kr.reborn.hiddenclass.ability;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.EnumMap;
import java.util.Map;

/**
 * 히든 능력 시그니처 — 31 능력 각각 고유한 시각·청각·플레이버.
 *
 * 기존: 모두 ENDER_DRAGON_GROWL 사운드 + "&6&l[히든능력] ..." 한 줄
 * 변경: 각 능력마다 다른 입자/사운드 + 고유한 발동 메시지
 */
public final class AbilitySignature {

    public final Particle particle;
    public final Particle secondaryParticle;
    public final Sound castSound;
    public final String flavorText;
    /** broadcast 헤더 (기본 "히든능력"에서 변경) */
    public final String broadcastTag;

    public AbilitySignature(Particle p, Particle sec, Sound s, String flavor, String tag) {
        this.particle = p; this.secondaryParticle = sec;
        this.castSound = s; this.flavorText = flavor; this.broadcastTag = tag;
    }

    private static final Map<HiddenAbility, AbilitySignature> M = new EnumMap<>(HiddenAbility.class);

    static {
        // ─── 초기 (10) ───
        r(HiddenAbility.DIVINE_FAVOR, Particle.HEART, Particle.SPELL_INSTANT,
                Sound.ENTITY_PLAYER_LEVELUP, "&e&l신이 너를 굽어보신다 — 행운 +30.", "[신성]");
        r(HiddenAbility.CHAOS_BURST, Particle.EXPLOSION_HUGE, Particle.SPELL_WITCH,
                Sound.ENTITY_GENERIC_EXPLODE, "&5&l혼돈이 폭발한다 — 모든 원소가 동시에!", "[혼돈]");
        r(HiddenAbility.DEMON_KI_OVERFLOW, Particle.DRAGON_BREATH, Particle.SOUL_FIRE_FLAME,
                Sound.ENTITY_WITHER_AMBIENT, "&4&l마기 폭주 — 30초간 모든 것이 배가된다.", "[마기]");
        r(HiddenAbility.IMMORTAL_REVIVE, Particle.TOTEM, Particle.END_ROD,
                Sound.ITEM_TOTEM_USE, "&6&l불멸 — 죽음이 너를 거부한다.", "[불멸]");
        r(HiddenAbility.DRAGON_BREATH_AURA, Particle.FLAME, Particle.DRAGON_BREATH,
                Sound.ENTITY_ENDER_DRAGON_GROWL, "&c&l용의 피가 깨어난다 — 화염과 비행.", "[용혈]");
        r(HiddenAbility.NAMELESS_STEALTH, Particle.SQUID_INK, Particle.SMOKE_NORMAL,
                Sound.ENTITY_VEX_AMBIENT, "&7&l이름 없음 — 너는 그림자 속에 있다.", "[무명]");
        r(HiddenAbility.GATE_RESONANCE, Particle.PORTAL, Particle.END_ROD,
                Sound.BLOCK_END_PORTAL_FRAME_FILL, "&3&l게이트와 공명한다 — 약점이 보인다.", "[공명]");
        r(HiddenAbility.SPIRIT_SIGHT, Particle.SPELL_MOB_AMBIENT, Particle.SPELL_INSTANT,
                Sound.BLOCK_BEACON_POWER_SELECT, "&5&l천안이 열린다 — 모든 것이 투시된다.", "[천안]");
        r(HiddenAbility.TRANSFORM_ROAR, Particle.SPELL_WITCH, Particle.SOUL_FIRE_FLAME,
                Sound.ENTITY_FOX_AGGRO, "&d&l요왕의 자질 — 완전 변신과 위압.", "[변신]");
        r(HiddenAbility.CYBER_IMMUNE, Particle.ELECTRIC_SPARK, Particle.SPELL_INSTANT,
                Sound.BLOCK_PISTON_EXTEND, "&b&l기계와 하나가 된다 — 면역.", "[사이버]");

        // ─── 성장 (20+) ───
        r(HiddenAbility.DUAL_CAST, Particle.CRIT_MAGIC, Particle.SWEEP_ATTACK,
                Sound.ITEM_TRIDENT_THUNDER, "&6&l검과 마법이 동시에 — 마검사의 정수.", "[마검]");
        r(HiddenAbility.CULT_COMMAND, Particle.SPELL_MOB, Particle.SQUID_INK,
                Sound.ENTITY_WITCH_AMBIENT, "&5&l천마의 명 — 마교도가 모두 너를 따른다.", "[천마]");
        r(HiddenAbility.FORBIDDEN_POWER, Particle.SOUL_FIRE_FLAME, Particle.DRAGON_BREATH,
                Sound.ENTITY_ENDER_DRAGON_GROWL, "&5&l마선 폭주 — 선기와 마기가 동시에.", "[마선]");
        r(HiddenAbility.DEMON_LORD_AURA, Particle.SQUID_INK, Particle.SOUL_FIRE_FLAME,
                Sound.ENTITY_WITHER_AMBIENT, "&4&l마왕의 위압 — 반경 20이 약화된다.", "[마왕]");
        r(HiddenAbility.YOKAI_EMPEROR, Particle.SPELL_WITCH, Particle.PORTAL,
                Sound.ENTITY_GHAST_SCREAM, "&5&l요제의 강림 — 주변 요괴 모두 직속.", "[요제]");
        r(HiddenAbility.DRAGON_LORD_ROAR, Particle.DRAGON_BREATH, Particle.EXPLOSION_LARGE,
                Sound.ENTITY_ENDER_DRAGON_GROWL, "&6&l용왕의 포효 — 반경 30이 떨린다.", "[용왕]");
        r(HiddenAbility.APOCALYPSE_COMMAND, Particle.SMOKE_LARGE, Particle.SPELL_WITCH,
                Sound.ENTITY_WITHER_AMBIENT, "&8&l종말의 명령 — 모든 것이 슬로우+위더+포이즌.", "[종말]");
        r(HiddenAbility.ABYSS_TOUCH, Particle.SQUID_INK, Particle.SOUL,
                Sound.ENTITY_WARDEN_ANGRY, "&0&l심연이 적을 만진다 — 4초 무력화.", "[심연]");
        r(HiddenAbility.SOUL_TRAVEL, Particle.SOUL, Particle.PORTAL,
                Sound.ENTITY_VEX_AMBIENT, "&7&l영혼만 시선 끝으로 — 환혼자의 비기.", "[환혼]");
        r(HiddenAbility.TIME_REWIND, Particle.END_ROD, Particle.PORTAL,
                Sound.BLOCK_BELL_RESONATE, "&e&l시간이 거꾸로 흐른다 — 5초 전으로.", "[시간]");
        r(HiddenAbility.DREAM_INTRUSION, Particle.SPELL_WITCH, Particle.HEART,
                Sound.ENTITY_PHANTOM_AMBIENT, "&d&l꿈에 들어간다 — 적이 잠든다.", "[꿈]");
        r(HiddenAbility.LABYRINTH_TELEPORT, Particle.PORTAL, Particle.SPELL_WITCH,
                Sound.ENTITY_ENDERMAN_TELEPORT, "&5&l미궁의 임의 층 — 운명의 도박.", "[미궁]");
        r(HiddenAbility.AI_COMMAND, Particle.ELECTRIC_SPARK, Particle.SQUID_INK,
                Sound.UI_BUTTON_CLICK, "&b&l메가코프 황제 권한 — AI 모두 너에게.", "[메가코프]");
        r(HiddenAbility.DUNGEON_DECREE, Particle.PORTAL, Particle.LANDING_OBSIDIAN_TEAR,
                Sound.BLOCK_BEACON_POWER_SELECT, "&5&l던전 마스터 — 몹과 트랩이 너에게 복종.", "[던전]");
        r(HiddenAbility.HERO_AURA, Particle.HEART, Particle.END_ROD,
                Sound.BLOCK_BELL_USE, "&6&l영웅의 후광 — 신도가 모인다.", "[영웅]");
        r(HiddenAbility.SEA_KING_COMMAND, Particle.WATER_BUBBLE, Particle.NAUTILUS,
                Sound.ENTITY_CONDUIT_AMBIENT, "&3&l해왕의 후예 — 바다가 너의 명을 듣는다.", "[해왕]");
        r(HiddenAbility.GENESIS_BLESSING, Particle.TOTEM, Particle.END_ROD,
                Sound.UI_TOAST_CHALLENGE_COMPLETE, "&6&l창세신기의 축복.", "[창세]");
        r(HiddenAbility.MASTER_OF_TRADES, Particle.SPELL_INSTANT, Particle.HEART,
                Sound.BLOCK_ANVIL_USE, "&e&l만물의 장인 — 모든 제작에 통달.", "[장인]");
        r(HiddenAbility.PRIMORDIAL_CONNECT, Particle.END_ROD, Particle.SQUID_INK,
                Sound.BLOCK_BEACON_ACTIVATE, "&f&l태초의 빛과 어둠이 동시에.", "[태초]");
        r(HiddenAbility.PIRATE_KING_FLAG, Particle.NOTE, Particle.HEART,
                Sound.ENTITY_DOLPHIN_PLAY, "&6&l해적왕의 깃발 — 해적 NPC 직속.", "[해적]");
        r(HiddenAbility.DREAM_LORD_PASSIVE, Particle.SPELL_WITCH, Particle.SPELL_INSTANT,
                Sound.ENTITY_PHANTOM_AMBIENT, "&d&l꿈의 지배자 — 수면이 수련.", "[꿈]");
    }

    private static void r(HiddenAbility ab, Particle p, Particle sec, Sound s, String flavor, String tag) {
        M.put(ab, new AbilitySignature(p, sec, s, flavor, tag));
    }

    public static AbilitySignature lookup(HiddenAbility ab) {
        return M.get(ab);
    }
}
