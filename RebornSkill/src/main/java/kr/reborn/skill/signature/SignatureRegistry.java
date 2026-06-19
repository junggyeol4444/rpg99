package kr.reborn.skill.signature;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * 스킬 시그니처 카탈로그.
 *
 * 80+ 유명 스킬에 고유 시각·청각·플레이버 부여.
 * 등록되지 않은 스킬은 element 기반 fallback.
 *
 * 분류:
 *   무공 (20): 독고구검·항룡18장·구양진경·태극권·천마신공·혈마공·만류귀종 등
 *   판타지 마법 (15): 메테오·블링크·텔레포트·환영·부활·헤이스트
 *   원소 마법 (12): 화염구·아쿠아샷·라이트닝·아이스랜스·바람칼 등
 *   신성 (8): 성광·심판·정화·축복·치유·부활·천계기원
 *   마기 (8): 지옥불·악마계약·영혼흡수·악몽·심마결
 *   특수 (15): 변신·소환·연쇄·DOT 등
 */
public final class SignatureRegistry {

    private static final Map<String, SkillSignature> SIGS = new HashMap<>();

    static {
        seedMartial();
        seedFantasy();
        seedElement();
        seedHoly();
        seedDemon();
        seedYokai();
        seedSpirit();
        seedDragon();
        seedOcean();
        seedCyber();
        seedSpecial();
        seedMartialExtra();
        seedFantasyExtra();
        seedCyberExtra();
        seedApocExtra();
        seedImmortalExtra();
        seedExtra();
        seedMartialFull();
        seedTaoist();
        seedHeavenFull();
        seedDemonFull();
        seedYokaiFull();
        seedDragonFull();
        seedOceanFull();
        seedEarthHunter();
        seedMagitechFull();
        seedSpiritFull();
        seedMisc();
    }

    // ──────────────────── 무공 (15) ────────────────────
    private static void seedMartial() {
        // 독고구검 — 9개의 검결, 각 일격필살
        s("dokgo_gugeom", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SWEEP_ATTACK,
                Particle.CRIT_MAGIC, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&7검의 흐름이 바람처럼 …독고검결 발현.",
                40, 2, 0);
        // 항룡십팔장 — 거대한 손바닥
        s("hangryong", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.EXPLOSION_LARGE,
                Particle.CLOUD, Sound.ENTITY_ENDER_DRAGON_HURT, Sound.ENTITY_GENERIC_EXPLODE,
                "&6용의 형상이 솟구친다 — 18장!",
                60, 3, 20);
        // 구양진경 — 양강의 정수, 빛
        s("guyang_jin", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.FLAME, Sound.BLOCK_BEACON_POWER_SELECT, Sound.BLOCK_FIRE_AMBIENT,
                "&e양강의 기가 단전을 가득 채운다.",
                100, 2, 100);
        // 태극권 — 부드러움
        s("taegeuk", SkillSignature.ParticlePattern.SPIRAL, Particle.SOUL,
                Particle.SOUL_FIRE_FLAME, Sound.BLOCK_PORTAL_AMBIENT, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&7태극의 도리, 음양조화.",
                80, 1, 60);
        // 만류귀종 — 모든 무공이 하나로
        s("manryu", SkillSignature.ParticlePattern.STAR_BURST, Particle.TOTEM,
                Particle.END_ROD, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&5만 가지 무공이 하나의 극의로 — 만류귀종!",
                100, 4, 200);
        // 혈마공 — 피의 마공, 흡혈
        s("hyeolma_gong", SkillSignature.ParticlePattern.AURA_IN, Particle.REDSTONE,
                Particle.SQUID_INK, Sound.ENTITY_WITCH_DRINK, Sound.ENTITY_WITCH_DRINK,
                "&4피의 갈증이 일어난다 …",
                100, 2, 0);
        // 수라마공 — 잔인한 마공
        s("sura_magong", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SQUID_INK,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_WITHER_HURT, Sound.ENTITY_VINDICATOR_ATTACK,
                "&4수라도의 살기가 솟구친다.",
                80, 3, 40);
        // 빙잠독경 — 만년 빙잠의 독공
        s("bingjam_dokgyeong", SkillSignature.ParticlePattern.STORM, Particle.SNOWFLAKE,
                Particle.SPELL_WITCH, Sound.BLOCK_GLASS_BREAK, Sound.ENTITY_SPIDER_DEATH,
                "&b만년빙잠의 독기 — 얼고 마비된다.",
                100, 4, 80);
        // 칠상권 — 7가지 형태
        s("chilsang_gwon", SkillSignature.ParticlePattern.RING, Particle.CRIT,
                Particle.SMOKE_LARGE, Sound.ENTITY_PLAYER_ATTACK_STRONG, Sound.ENTITY_RAVAGER_ATTACK,
                "&c철갑웅의 칠상권 — 형형색색 일곱 변화.",
                40, 2, 0);
        // 삼재검법 — 기본
        s("samje_basic_form", SkillSignature.ParticlePattern.SLASH_ARC, Particle.CRIT,
                null, Sound.ITEM_TRIDENT_RIPTIDE_1, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&7기본 검법 — 삼재의 형.",
                20, 0, 0);
        // 천마신공 — 마교 절기
        s("cheonma_singong", SkillSignature.ParticlePattern.AURA_OUT, Particle.DRAGON_BREATH,
                Particle.SQUID_INK, Sound.ENTITY_WITHER_SPAWN, Sound.ENTITY_ENDER_DRAGON_GROWL,
                "&5&l천마의 기운이 강림한다!",
                200, 4, 200);
        // 천마군림보 — 천마의 보법
        s("cheonma_step", SkillSignature.ParticlePattern.BEAM, Particle.PORTAL,
                Particle.SQUID_INK, Sound.ENTITY_ENDERMAN_TELEPORT, null,
                "&5천마군림보 — 그림자처럼.",
                40, 1, 0);
        // 일위도강 — 검 한 자루로 강을 건넌다
        s("ilwi_dogang", SkillSignature.ParticlePattern.BEAM, Particle.WATER_SPLASH,
                Particle.CRIT_MAGIC, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, null,
                "&3일위도강 — 검 한 자루로.",
                40, 0, 30);
        // 검선기 — 검에서 빛이 나옴
        s("sword_immortal_aura", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.TOTEM, Sound.BLOCK_BEACON_ACTIVATE, Sound.ITEM_TRIDENT_THUNDER,
                "&e&l검에 천기가 흐른다 — 검선의 경지.",
                200, 3, 100);
        // 오독공 — 다섯 가지 독
        s("five_poison", SkillSignature.ParticlePattern.STORM, Particle.SPELL_WITCH,
                Particle.SLIME, Sound.ENTITY_WITCH_THROW, Sound.ENTITY_SPIDER_HURT,
                "&2오독교의 다섯 독 — 백사, 황두꺼비, 청전갈, 적지네, 흑거미.",
                160, 2, 100);
    }

    // ──────────────────── 판타지 마법 (15) ────────────────────
    private static void seedFantasy() {
        // 메테오 — 하늘에서 별을 떨군다
        s("book_meteor", SkillSignature.ParticlePattern.METEOR_RAIN, Particle.LAVA,
                Particle.EXPLOSION_HUGE, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ENTITY_GENERIC_EXPLODE,
                "&c&l하늘의 별이 떨어진다 — 메테오!",
                40, 2, 0);
        // 환영 마스터의 책 — 다중 환영
        s("illusion_master", SkillSignature.ParticlePattern.AURA_OUT, Particle.PORTAL,
                Particle.SPELL_WITCH, Sound.BLOCK_PORTAL_AMBIENT, Sound.ENTITY_ENDERMAN_TELEPORT,
                "&d환영이 일어선다 — 누가 진짜인가?",
                200, 3, 100);
        // 텔레포트 — 시선 끝으로
        s("blink", SkillSignature.ParticlePattern.AURA_IN, Particle.PORTAL,
                Particle.END_ROD, Sound.ENTITY_ENDERMAN_TELEPORT, null,
                "&5블링크.",
                0, 0, 20);
        // 헤이스트
        s("haste", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                null, Sound.BLOCK_BEACON_POWER_SELECT, null,
                "&a신속의 시간.",
                400, 1, 0);
        // 부활의 서 — 신만이 알던 비기
        s("book_resurrection", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.TOTEM,
                Particle.END_ROD, Sound.ITEM_TOTEM_USE, Sound.BLOCK_BEACON_ACTIVATE,
                "&6&l죽음을 거스른다 — 부활!",
                0, 0, 100);
        // 아르테온의 검 — 전설 검술
        s("book_arteon_blade", SkillSignature.ParticlePattern.TWIN_BEAM, Particle.END_ROD,
                Particle.CRIT_MAGIC, Sound.ITEM_TRIDENT_THUNDER, Sound.ITEM_TRIDENT_RIPTIDE_3,
                "&6&l아르테온의 빛이 검에 깃든다.",
                60, 3, 40);
        // 마검사 (히든클래스) 융합 비기 — 마법 입자 + 검기 호
        s("magic_sword_fusion", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SPELL_WITCH,
                Particle.CRIT_MAGIC, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_EVOKER_CAST_SPELL,
                "&d마검 융합 — 마법과 검술이 하나로.",
                80, 3, 60);
        // 파이어볼
        s("fireball_1", SkillSignature.ParticlePattern.BEAM, Particle.FLAME,
                Particle.LAVA, Sound.ENTITY_BLAZE_SHOOT, Sound.ENTITY_GENERIC_EXPLODE,
                "&c파이어볼!",
                40, 1, 0);
        // 라이트닝 볼트
        s("lightning_bolt", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.ELECTRIC_SPARK,
                Particle.CLOUD, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
                "&e&l번개여, 떨어져라.",
                40, 2, 0);
        // 아이스 랜스
        s("ice_lance", SkillSignature.ParticlePattern.BEAM, Particle.SNOWFLAKE,
                Particle.BLOCK_CRACK, Sound.ITEM_TRIDENT_THROW, Sound.BLOCK_GLASS_BREAK,
                "&b얼음 창이 직선으로 꿰뚫는다.",
                60, 2, 0);
        // 윈드 슬래시
        s("wind_slash", SkillSignature.ParticlePattern.SLASH_ARC, Particle.CLOUD,
                Particle.SWEEP_ATTACK, Sound.ENTITY_PHANTOM_FLAP, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&7바람의 칼날.",
                30, 1, 0);
        // 어스 스파이크
        s("earth_spike", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.BLOCK_DUST,
                Particle.LANDING_OBSIDIAN_TEAR, Sound.BLOCK_STONE_BREAK, Sound.BLOCK_STONE_FALL,
                "&6대지의 가시.",
                60, 2, 0);
        // 다크니스 그립
        s("darkness_grip", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SOUL, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_WITHER_AMBIENT,
                "&0어둠이 너를 붙든다.",
                80, 3, 0);
        // 매스 디스펠 — 정화
        s("greater_sage_dispel", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.TOTEM, Sound.BLOCK_BEACON_DEACTIVATE, null,
                "&b모든 마법이 정화된다.",
                100, 0, 0);
        // 마나 폭발
        s("mana_burst", SkillSignature.ParticlePattern.EXPLOSION, Particle.EXPLOSION_HUGE,
                Particle.SPELL_WITCH, Sound.ENTITY_GENERIC_EXPLODE, Sound.ENTITY_EVOKER_CAST_SPELL,
                "&5&l마나가 폭주한다!",
                40, 3, 0);
        // 시간 정지
        s("time_stop", SkillSignature.ParticlePattern.TIME_RIPPLE, Particle.END_ROD,
                Particle.PORTAL, Sound.BLOCK_BELL_RESONATE, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&e&l시간이 멈춘다 — 너만이 움직인다.",
                100, 9, 100);
    }

    // ──────────────────── 원소 (10) ────────────────────
    private static void seedElement() {
        s("spirit_flame_lance", SkillSignature.ParticlePattern.BEAM, Particle.FLAME,
                Particle.LAVA, Sound.ENTITY_BLAZE_SHOOT, Sound.BLOCK_FIRE_AMBIENT,
                "&c정령 화염창 — 화염 정령의 분노.",
                60, 2, 0);
        s("spirit_aqua_heal", SkillSignature.ParticlePattern.AURA_OUT, Particle.WATER_SPLASH,
                Particle.HEART, Sound.BLOCK_WATER_AMBIENT, Sound.ENTITY_PLAYER_LEVELUP,
                "&3수정령의 가호 — 모두 회복.",
                60, 0, 0);
        s("spirit_quake", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.BLOCK_DUST,
                Particle.LANDING_OBSIDIAN_TEAR, Sound.BLOCK_BASALT_BREAK, Sound.BLOCK_STONE_FALL,
                "&6대지 정령의 진동.",
                40, 2, 0);
        s("spirit_wind_blade", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CLOUD,
                Particle.SWEEP_ATTACK, Sound.ITEM_ELYTRA_FLYING, Sound.ENTITY_PHANTOM_FLAP,
                "&a풍정령의 칼바람.",
                30, 1, 0);
        s("element_bullet", SkillSignature.ParticlePattern.BEAM, Particle.CRIT_MAGIC,
                Particle.SPELL_INSTANT, Sound.ENTITY_BLAZE_SHOOT, null,
                "&7원소 화살.",
                30, 1, 0);
        s("element_sense", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_MOB,
                Particle.SPELL_WITCH, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, null,
                "&5원소의 흐름이 보인다.",
                400, 0, 0);
        // 4 원소왕 소환
        s("spirit_king_summon", SkillSignature.ParticlePattern.STAR_BURST, Particle.TOTEM,
                Particle.END_ROD, Sound.BLOCK_BEACON_ACTIVATE, Sound.ENTITY_ENDER_DRAGON_GROWL,
                "&5&l정령왕이여, 강림하소서!",
                200, 4, 400);
        // 빛 폭발
        s("light_burst", SkillSignature.ParticlePattern.EXPLOSION, Particle.END_ROD,
                Particle.FIREWORKS_SPARK, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                "&e빛이 터진다.",
                40, 0, 0);
        // 어둠 흡수
        s("dark_drain", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SOUL, Sound.ENTITY_WITCH_DRINK, Sound.ENTITY_VEX_DEATH,
                "&0어둠이 너를 잡아먹는다.",
                60, 2, 0);
        // 카오스 폭발 (모든 원소)
        s("chaos_burst", SkillSignature.ParticlePattern.STAR_BURST, Particle.EXPLOSION_HUGE,
                Particle.PORTAL, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ENTITY_GENERIC_EXPLODE,
                "&5&l혼돈! 모든 원소가 한꺼번에.",
                80, 3, 0);
    }

    // ──────────────────── 신성 (10) ────────────────────
    private static void seedHoly() {
        // 성광
        s("holy_ray", SkillSignature.ParticlePattern.BEAM, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.BLOCK_BEACON_POWER_SELECT, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&e성광이 직선으로 비춘다.",
                80, 1, 0);
        // 천계 심판
        s("judgment", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.END_ROD,
                Particle.TOTEM, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.BLOCK_BELL_USE,
                "&6&l신의 심판이 떨어진다.",
                100, 4, 60);
        // 천계 기원
        s("heaven_pillar_of_fire", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.FLAME,
                Particle.END_ROD, Sound.BLOCK_FIRE_AMBIENT, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&e&l하늘의 불기둥.",
                80, 3, 80);
        // 천계 치유
        s("heaven_mass_heal", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.END_ROD, Sound.ENTITY_PLAYER_LEVELUP, Sound.BLOCK_BEACON_POWER_SELECT,
                "&b광역 치유의 빛.",
                40, 0, 0);
        // 천계 신성한 방패
        s("divine_shield", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.BLOCK_BEACON_ACTIVATE, null,
                "&e&l신의 보호 — 잠시 무적.",
                100, 5, 0);
        // 정화
        s("purify_orb", SkillSignature.ParticlePattern.SPIRAL, Particle.SPELL_INSTANT,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&f정화의 빛 — 저주가 풀린다.",
                40, 0, 0);
        // 축복
        s("bless_followers", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.TOTEM, Sound.BLOCK_BEACON_POWER_SELECT, null,
                "&6신의 축복이 모두에게.",
                100, 1, 0);
        // 신성 방패
        s("shield_of_light", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                null, Sound.BLOCK_BEACON_ACTIVATE, null,
                "&e빛의 방패 — 흡수.",
                100, 3, 0);
        // 신의 강림
        s("avatar_descent", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.TOTEM,
                Particle.END_ROD, Sound.ITEM_TOTEM_USE, Sound.BLOCK_BELL_RESONATE,
                "&6&l신성이 너의 몸에 깃든다!",
                200, 4, 200);
        // 신성한 인도
        s("guiding_light", SkillSignature.ParticlePattern.AURA_OUT, Particle.GLOW,
                Particle.SPELL_INSTANT, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&e인도의 빛 — 길이 보인다.",
                400, 0, 0);
    }

    // ──────────────────── 마계 (10) ────────────────────
    private static void seedDemon() {
        s("demon_hellfire", SkillSignature.ParticlePattern.STORM, Particle.SOUL_FIRE_FLAME,
                Particle.LAVA, Sound.BLOCK_FIRE_AMBIENT, Sound.ENTITY_BLAZE_BURN,
                "&4지옥불! 불꽃이 영혼을 태운다.",
                100, 3, 60);
        s("demon_blast", SkillSignature.ParticlePattern.EXPLOSION, Particle.SQUID_INK,
                Particle.EXPLOSION_HUGE, Sound.ENTITY_WITHER_HURT, Sound.ENTITY_GENERIC_EXPLODE,
                "&5마기 폭발!",
                40, 2, 0);
        s("hell_fire_low", SkillSignature.ParticlePattern.BEAM, Particle.SOUL_FIRE_FLAME,
                null, Sound.ENTITY_BLAZE_SHOOT, Sound.BLOCK_FIRE_AMBIENT,
                "&c하급 지옥불.",
                60, 1, 0);
        s("soul_drain", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.SOUL,
                Particle.SQUID_INK, Sound.ENTITY_VEX_DEATH, Sound.ENTITY_ENDER_EYE_DEATH,
                "&8영혼을 빨아들인다.",
                100, 3, 60);
        s("nightmare", SkillSignature.ParticlePattern.AURA_IN, Particle.SPELL_WITCH,
                Particle.SOUL, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_PHANTOM_AMBIENT,
                "&5악몽이 너를 잠재운다.",
                100, 4, 0);
        s("demon_pact", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL_FIRE_FLAME,
                Particle.TOTEM, Sound.ENTITY_WITHER_SPAWN, null,
                "&4&l악마와의 계약 — 영혼 일부를 대가로.",
                400, 3, 0);
        s("forbidden_power", SkillSignature.ParticlePattern.AURA_OUT, Particle.DRAGON_BREATH,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ITEM_TRIDENT_THUNDER,
                "&5&l선기와 마기가 동시에 폭주한다!",
                1200, 5, 1200);
        s("abyss_touch", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SOUL, Sound.ENTITY_WARDEN_AMBIENT, Sound.ENTITY_WARDEN_ANGRY,
                "&0심연이 너를 만진다.",
                80, 5, 0);
        s("demon_lord_aura", SkillSignature.ParticlePattern.AURA_OUT, Particle.SQUID_INK,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_WITHER_AMBIENT, null,
                "&4&l마왕의 위압.",
                200, 4, 200);
        s("cult_command", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_MOB,
                Particle.SQUID_INK, Sound.ENTITY_WITCH_AMBIENT, null,
                "&5마교의 명 — 모든 마교도 직속.",
                600, 1, 200);
    }

    // ──────────────────── 요계 (6) ────────────────────
    private static void seedYokai() {
        s("yokai_foxfire_volley", SkillSignature.ParticlePattern.STORM, Particle.SOUL_FIRE_FLAME,
                Particle.SPELL_WITCH, Sound.ENTITY_FOX_AGGRO, Sound.BLOCK_FIRE_AMBIENT,
                "&d구미호의 도깨비불 — 푸르게 흔들린다.",
                80, 2, 80);
        s("yokai_charm", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.SPELL_WITCH, Sound.ENTITY_CAT_PURREOW, null,
                "&d매혹의 시선 — 적이 너를 사랑하게 된다.",
                100, 3, 0);
        s("transform_human", SkillSignature.ParticlePattern.AURA_IN, Particle.PORTAL,
                Particle.SPELL_WITCH, Sound.ENTITY_FOX_AMBIENT, Sound.ENTITY_PLAYER_LEVELUP,
                "&d변신 — 인간으로.",
                600, 1, 0);
        s("fox_fire", SkillSignature.ParticlePattern.BEAM, Particle.SOUL_FIRE_FLAME,
                Particle.SPELL_WITCH, Sound.ENTITY_BLAZE_SHOOT, null,
                "&5여우 불 — 푸른 화염.",
                60, 1, 0);
        s("yokai_kishin_strike", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CRIT,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_RAVAGER_ATTACK, Sound.ENTITY_WITHER_HURT,
                "&c오니의 일격.",
                40, 3, 0);
        s("yokai_emperor_summon", SkillSignature.ParticlePattern.STAR_BURST, Particle.SPELL_WITCH,
                Particle.PORTAL, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_GHAST_SCREAM,
                "&5&l요제의 강림 — 모든 요괴가 일어선다.",
                300, 4, 300);
    }

    // ──────────────────── 정령 (이미 seedElement에 포함) — 추가는 없음 ────────────────────
    private static void seedSpirit() { /* moved to seedElement */ }

    // ──────────────────── 드래곤 (6) ────────────────────
    private static void seedDragon() {
        s("dragon_fire_breath", SkillSignature.ParticlePattern.DRAGON_BREATH, Particle.DRAGON_BREATH,
                Particle.FLAME, Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.ENTITY_BLAZE_AMBIENT,
                "&c용염 — 모든 것을 태운다.",
                100, 4, 100);
        s("dragon_acid_breath", SkillSignature.ParticlePattern.DRAGON_BREATH, Particle.SQUID_INK,
                Particle.DRIPPING_LAVA, Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.ENTITY_WITHER_HURT,
                "&8흑룡의 산성 브레스 — 갑옷을 녹인다.",
                100, 3, 80);
        s("dragon_poison_breath", SkillSignature.ParticlePattern.DRAGON_BREATH, Particle.SLIME,
                Particle.VILLAGER_HAPPY, Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.ENTITY_SPIDER_HURT,
                "&2녹룡의 독 브레스 — 숲의 분노.",
                120, 2, 60);
        s("breath_basic", SkillSignature.ParticlePattern.DRAGON_BREATH, Particle.DRAGON_BREATH,
                null, Sound.ENTITY_ENDER_DRAGON_FLAP, null,
                "&6용의 입김.",
                60, 2, 60);
        s("flight", SkillSignature.ParticlePattern.AURA_OUT, Particle.DRAGON_BREATH,
                Particle.CLOUD, Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.ITEM_ELYTRA_FLYING,
                "&c용의 날개 — 비행 가능.",
                1200, 0, 0);
        s("dragon_lord_roar", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.EXPLOSION_LARGE,
                Particle.DRAGON_BREATH, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_ENDER_DRAGON_HURT,
                "&6&l용왕의 포효 — 반경 30 적 기절!",
                200, 4, 0);
        s("transform_roar", SkillSignature.ParticlePattern.AURA_OUT, Particle.DRAGON_BREATH,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_ENDER_DRAGON_GROWL, null,
                "&d요왕 변신 — 광역 위압!",
                1200, 3, 0);
        s("dragon_breath_aura", SkillSignature.ParticlePattern.AURA_OUT, Particle.FLAME,
                Particle.DRAGON_BREATH, Sound.ENTITY_BLAZE_AMBIENT, null,
                "&c공격에 화염 부여 + 비행.",
                Integer.MAX_VALUE, 0, 0);
    }

    // ──────────────────── 해양 (5) ────────────────────
    private static void seedOcean() {
        s("ocean_water_jet", SkillSignature.ParticlePattern.BEAM, Particle.WATER_SPLASH,
                Particle.BUBBLE_POP, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, Sound.BLOCK_WATER_AMBIENT,
                "&3물의 분사 — 강력한 직선.",
                40, 1, 0);
        s("ocean_siren_song", SkillSignature.ParticlePattern.AURA_OUT, Particle.NOTE,
                Particle.HEART, Sound.ENTITY_DOLPHIN_AMBIENT, null,
                "&b사이렌의 노래 — 적이 다가온다.",
                100, 3, 0);
        s("sea_king_command", SkillSignature.ParticlePattern.AURA_OUT, Particle.WATER_BUBBLE,
                Particle.WATER_SPLASH, Sound.BLOCK_CONDUIT_AMBIENT, Sound.ENTITY_DROWNED_AMBIENT,
                "&3해왕의 명령 — 해양 생물 모두 직속.",
                6000, 0, 0);
        s("water_breathing", SkillSignature.ParticlePattern.AURA_OUT, Particle.BUBBLE_POP,
                null, Sound.BLOCK_WATER_AMBIENT, null,
                "&b수중 호흡.",
                Integer.MAX_VALUE, 0, 0);
        s("basic_navigation", SkillSignature.ParticlePattern.AURA_OUT, Particle.CLOUD,
                null, Sound.AMBIENT_UNDERWATER_LOOP, null,
                "&3기본 항해 — 방향 인지.",
                Integer.MAX_VALUE, 0, 0);
    }

    // ──────────────────── 사이버 (5) ────────────────────
    private static void seedCyber() {
        s("neural_accel", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.REDSTONE, Sound.BLOCK_BEACON_POWER_SELECT, Sound.BLOCK_PISTON_EXTEND,
                "&b뉴로부스터 — 신경 가속.",
                600, 2, 0);
        s("cyber_master_hack", SkillSignature.ParticlePattern.TIME_RIPPLE, Particle.ELECTRIC_SPARK,
                Particle.PORTAL, Sound.BLOCK_END_PORTAL_FRAME_FILL, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&b&l마스터 해킹 — 모든 시스템에 침투.",
                100, 5, 60);
        s("drone_summon", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.REDSTONE, Sound.BLOCK_PISTON_EXTEND, Sound.ITEM_TRIDENT_THROW,
                "&b드론 소환.",
                0, 0, 0);
        s("ai_command", SkillSignature.ParticlePattern.AURA_OUT, Particle.SQUID_INK,
                Particle.ELECTRIC_SPARK, Sound.UI_BUTTON_CLICK, null,
                "&b반경 50 AI/드론 직속.",
                6000, 0, 0);
    }

    // ──────────────────── 특수 (10) ────────────────────
    private static void seedSpecial() {
        s("soul_travel", SkillSignature.ParticlePattern.AURA_IN, Particle.SOUL,
                Particle.PORTAL, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_ENDERMAN_TELEPORT,
                "&7영혼이 시선 끝으로 — 환혼.",
                0, 0, 40);
        s("time_rewind", SkillSignature.ParticlePattern.TIME_RIPPLE, Particle.NAUTILUS,
                Particle.SPELL_INSTANT, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.BLOCK_BELL_USE,
                "&e&l시간이 거꾸로 흐른다.",
                0, 0, 60);
        s("time_manipulation", SkillSignature.ParticlePattern.TIME_RIPPLE, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.BLOCK_BELL_RESONATE, null,
                "&e시간을 조작한다.",
                200, 3, 100);
        s("dream_intrusion", SkillSignature.ParticlePattern.AURA_IN, Particle.SPELL_WITCH,
                Particle.HEART, Sound.ENTITY_VEX_AMBIENT, null,
                "&d꿈에 들어가 잠재운다.",
                100, 5, 0);
        s("labyrinth_teleport", SkillSignature.ParticlePattern.AURA_IN, Particle.PORTAL,
                Particle.SPELL_WITCH, Sound.ENTITY_ENDERMAN_TELEPORT, null,
                "&5미궁의 임의 층 — 무작위 이동.",
                0, 0, 0);
        s("apocalypse_command", SkillSignature.ParticlePattern.AURA_OUT, Particle.SMOKE_LARGE,
                Particle.SPELL_WITCH, Sound.ENTITY_WITHER_AMBIENT, Sound.ENTITY_RAVAGER_ROAR,
                "&8&l종말 — 반경 50 슬로우+위더+포이즌+헝거.",
                600, 3, 200);
        s("primordial_connect", SkillSignature.ParticlePattern.STAR_BURST, Particle.END_ROD,
                Particle.SQUID_INK, Sound.BLOCK_BEACON_ACTIVATE, Sound.ENTITY_ENDER_DRAGON_GROWL,
                "&f빛과 어둠이 동시에 강림한다.",
                1200, 4, 0);
        s("genesis_blessing", SkillSignature.ParticlePattern.AURA_OUT, Particle.TOTEM,
                Particle.END_ROD, Sound.UI_TOAST_CHALLENGE_COMPLETE, null,
                "&6&l창세신기의 가호 — 모든 스탯 증대.",
                Integer.MAX_VALUE, 0, 0);
        s("pirate_king_flag", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.NOTE, Sound.ENTITY_DOLPHIN_PLAY, null,
                "&6해적왕의 깃발 — 해적 NPC 직속 + 해양력 ×1.5.",
                Integer.MAX_VALUE, 0, 0);
        s("master_of_trades", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.HEART, Sound.BLOCK_ANVIL_USE, null,
                "&e만물의 장인 — 제작 30% 절감.",
                Integer.MAX_VALUE, 0, 0);
    }

    // ──────────────────── 무공 추가 (15) ────────────────────
    private static void seedMartialExtra() {
        s("byeoksa_geombeop", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SWEEP_ATTACK,
                Particle.CRIT, Sound.ITEM_TRIDENT_RIPTIDE_2, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&7&l벽사검법 — 사이한 모든 것을 베어낸다.", 0, 0, 0);
        s("cheolposam", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CRIT,
                Particle.SMOKE_LARGE, Sound.ENTITY_RAVAGER_ATTACK, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&8&l철포삼 — 한 주먹으로 산을 무너뜨린다.", 0, 0, 0);
        s("bukmyeong_singong", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL,
                Particle.SQUID_INK, Sound.ENTITY_VEX_AMBIENT, Sound.BLOCK_BEACON_AMBIENT,
                "&5&l북명신공 — 거대한 바다처럼 끝없이 흡수한다.", 200, 1, 200);
        s("cheonjam_gong", SkillSignature.ParticlePattern.SPIRAL, Particle.SPELL_WITCH,
                Particle.SOUL, Sound.BLOCK_PORTAL_AMBIENT, Sound.ENTITY_WITCH_DRINK,
                "&5&l천잠신공 — 누에가 비단을 짜듯 내공을 짠다.", 600, 1, 0);
        s("cheonin_hapil_gong", SkillSignature.ParticlePattern.TWIN_BEAM, Particle.END_ROD,
                Particle.SOUL, Sound.BLOCK_BELL_USE, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&e&l천인합일공 — 사람과 하늘이 하나가 된다.", 400, 3, 0);
        s("cheonsan_jeolmaesu", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CRIT_MAGIC,
                Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_STRONG, Sound.ENTITY_PLAYER_BIG_FALL,
                "&6&l천산절매수 — 매화 한 송이로 적을 베는 절기.", 0, 0, 0);
        s("cheonsan_yukyangjang", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.FLAME,
                Particle.LAVA, Sound.ENTITY_BLAZE_AMBIENT, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&c&l천산육양장 — 여섯 양기로 펼치는 거대 장법.", 0, 0, 0);
        s("cheonsu_gwaneumjang", SkillSignature.ParticlePattern.STAR_BURST, Particle.HEART,
                Particle.END_ROD, Sound.BLOCK_BELL_RESONATE, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&b&l천수관음장 — 천 손이 동시에 펼쳐진다.", 0, 0, 0);
        s("chilsanggwon", SkillSignature.ParticlePattern.RING, Particle.FLAME,
                Particle.CRIT_MAGIC, Sound.ENTITY_PLAYER_ATTACK_STRONG, Sound.ENTITY_BLAZE_HURT,
                "&c&l칠상권 — 일곱 가지 변화의 권법.", 0, 0, 0);
        s("amyeon_sohonjang", SkillSignature.ParticlePattern.AURA_IN, Particle.SOUL,
                Particle.SPELL_WITCH, Sound.ENTITY_VEX_DEATH, Sound.ENTITY_WITCH_DRINK,
                "&8&l암련소혼장 — 영혼을 태워 펼치는 사악한 장법.", 100, 2, 0);
        s("cheonma_bi", SkillSignature.ParticlePattern.BEAM, Particle.SQUID_INK,
                Particle.SOUL_FIRE_FLAME, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_WITHER_HURT,
                "&5&l천마비 — 천마의 비기 일격.", 0, 0, 0);
        s("cheonma_talhonsoo", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.SOUL,
                Particle.SQUID_INK, Sound.ENTITY_WITHER_AMBIENT, Sound.ENTITY_VEX_HURT,
                "&5&l천마탈혼수 — 영혼을 빨아들이는 마수.", 80, 3, 0);
        s("baekbo_singwon", SkillSignature.ParticlePattern.BEAM, Particle.CRIT_MAGIC,
                Particle.END_ROD, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&7&l백보신권 — 백 보 떨어진 적도 격살한다.", 0, 0, 0);
        s("bicheon_singong", SkillSignature.ParticlePattern.AURA_OUT, Particle.CLOUD,
                Particle.SWEEP_ATTACK, Sound.ENTITY_PHANTOM_FLAP, Sound.ITEM_ELYTRA_FLYING,
                "&b&l비천신공 — 하늘을 나는 듯한 신법.", 600, 2, 0);
        s("daena_cheonsoo", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.HEART, Sound.BLOCK_BEACON_POWER_SELECT, null,
                "&e&l대애천수 — 큰 자비의 손길.", 600, 2, 0);
    }

    // ──────────────────── 판타지 추가 (15) ────────────────────
    private static void seedFantasyExtra() {
        s("meteor_strike", SkillSignature.ParticlePattern.METEOR_RAIN, Particle.DRIPPING_LAVA,
                Particle.SMOKE_LARGE, Sound.ENTITY_GENERIC_EXPLODE, Sound.BLOCK_STONE_BREAK,
                "&c&l메테오 스트라이크 — 단발 운석.", 80, 3, 0);
        s("wish_spell", SkillSignature.ParticlePattern.STAR_BURST, Particle.TOTEM,
                Particle.END_ROD, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.BLOCK_BELL_RESONATE,
                "&6&l소원 마법 — 무엇이든 한 가지.", 0, 0, 0);
        s("soul_bind", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.SOUL,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_WITHER_AMBIENT,
                "&5&l영혼 결박 — 도망갈 수 없다.", 200, 3, 0);
        s("dragon_word", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.DRAGON_BREATH,
                Particle.FLAME, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_GHAST_SCREAM,
                "&c&l용의 언어 — 들은 자는 무릎을 꿇는다.", 100, 4, 0);
        s("dimension_gate", SkillSignature.ParticlePattern.AURA_IN, Particle.PORTAL,
                Particle.END_ROD, Sound.BLOCK_PORTAL_AMBIENT, Sound.ENTITY_ENDERMAN_TELEPORT,
                "&5&l차원의 문 — 어디로든.", 0, 0, 0);
        s("necromancy_codex", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL,
                Particle.SQUID_INK, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_SKELETON_AMBIENT,
                "&0&l네크로맨시 코덱스 — 죽은 자가 일어난다.", 200, 0, 0);
        s("elemental_overlord", SkillSignature.ParticlePattern.STAR_BURST, Particle.SPELL_MOB,
                Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&d&l원소의 대공 — 모든 원소가 너에게 복종한다.", 400, 4, 0);
        s("eternal_sleep", SkillSignature.ParticlePattern.AURA_IN, Particle.SPELL_WITCH,
                Particle.HEART, Sound.ENTITY_PHANTOM_AMBIENT, null,
                "&1&l영원한 잠 — 시전자만 깨울 수 있다.", 2400, 9, 0);
        s("gate_of_babylon", SkillSignature.ParticlePattern.STORM, Particle.END_ROD,
                Particle.CRIT_MAGIC, Sound.BLOCK_PORTAL_AMBIENT, Sound.ITEM_TRIDENT_THUNDER,
                "&6&l바빌론의 문 — 수많은 무기가 솟아오른다.", 100, 4, 0);
        s("absolute_zero", SkillSignature.ParticlePattern.AURA_OUT, Particle.SNOWFLAKE,
                Particle.CLOUD, Sound.BLOCK_GLASS_BREAK, Sound.BLOCK_PACKED_ICE_BREAK,
                "&b&l절대 영도 — 모든 것이 멈춘다.", 200, 7, 0);
        s("space_distortion", SkillSignature.ParticlePattern.TIME_RIPPLE, Particle.PORTAL,
                Particle.SPELL_WITCH, Sound.BLOCK_END_PORTAL_FRAME_FILL, Sound.ENTITY_ENDERMAN_TELEPORT,
                "&5&l공간 왜곡 — 좌표가 흐려진다.", 100, 5, 0);
        s("life_drain", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.REDSTONE,
                Particle.HEART, Sound.ENTITY_WITCH_DRINK, Sound.ENTITY_VEX_DEATH,
                "&4&l생명 흡수 — 적의 생명이 너에게로.", 100, 2, 0);
        s("summon_elemental_lord", SkillSignature.ParticlePattern.STAR_BURST, Particle.SPELL_MOB,
                Particle.END_ROD, Sound.BLOCK_BEACON_ACTIVATE, Sound.ENTITY_ENDER_DRAGON_GROWL,
                "&a&l원소 군주 소환 — 그들이 강림한다.", 0, 0, 0);
        s("create_golem", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.BLOCK_DUST,
                Particle.LANDING_OBSIDIAN_TEAR, Sound.BLOCK_STONE_PLACE, Sound.ENTITY_IRON_GOLEM_REPAIR,
                "&7&l골렘 창조 — 흙에서 일어선다.", 0, 0, 0);
        s("illusion_world", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_WITCH,
                Particle.PORTAL, Sound.BLOCK_PORTAL_AMBIENT, null,
                "&d&l환영 세계 — 진실과 환영이 뒤바뀐다.", 600, 4, 0);
        s("resurrection", SkillSignature.ParticlePattern.STAR_BURST, Particle.TOTEM,
                Particle.HEART, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_PLAYER_LEVELUP,
                "&6&l부활 — 죽음을 거스른다.", 0, 0, 0);
    }

    // ──────────────────── 사이버 추가 (8) ────────────────────
    private static void seedCyberExtra() {
        s("cyber_mantis_blade", SkillSignature.ParticlePattern.SLASH_ARC, Particle.ELECTRIC_SPARK,
                Particle.CRIT, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&b&l맨티스 블레이드 — 사이버 칼날이 솟아난다.", 0, 0, 0);
        s("cyber_optical_camo", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.SPELL_INSTANT, Sound.BLOCK_PISTON_EXTEND, null,
                "&7&l광학 위장 — 보이지 않는다.", 400, 0, 0);
        s("cyber_overload_hack", SkillSignature.ParticlePattern.TIME_RIPPLE, Particle.ELECTRIC_SPARK,
                Particle.REDSTONE, Sound.BLOCK_END_PORTAL_FRAME_FILL, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&5&l오버로드 해킹 — 시스템 강제 종료.", 100, 5, 0);
        s("cyber_sandevistan", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.SPELL_INSTANT, Sound.BLOCK_BEACON_POWER_SELECT, Sound.BLOCK_PISTON_EXTEND,
                "&b&l샌데비스탄 — 시간이 느려진다.", 80, 4, 0);
        s("cyber_smartgun", SkillSignature.ParticlePattern.STORM, Particle.ELECTRIC_SPARK,
                Particle.CRIT, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&e&l스마트건 — 추적 미사일이 적을 따라간다.", 0, 0, 0);
        s("cyber_kerenzikov", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.CLOUD, Sound.BLOCK_PISTON_EXTEND, null,
                "&b&l케렌지코프 — 슬로우 모션 회피.", 80, 1, 0);
        s("energy_shield", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.ELECTRIC_SPARK, Sound.BLOCK_BEACON_ACTIVATE, null,
                "&b&l에너지 실드.", 200, 3, 0);
        s("hack_virus", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.ELECTRIC_SPARK,
                Particle.SQUID_INK, Sound.BLOCK_END_PORTAL_FRAME_FILL, Sound.ENTITY_VEX_HURT,
                "&5&l바이러스 침투 — 적의 시스템을 오염시킨다.", 200, 2, 0);
    }

    // ──────────────────── 아포 추가 (5) ────────────────────
    private static void seedApocExtra() {
        s("apoc_adren_shot", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.HEART, Sound.ITEM_BOTTLE_FILL, null,
                "&c아드레날린 — 모든 감각이 일어선다.", 200, 2, 0);
        s("apoc_molotov", SkillSignature.ParticlePattern.EXPLOSION, Particle.FLAME,
                Particle.LAVA, Sound.ITEM_BOTTLE_FILL, Sound.ENTITY_GENERIC_EXPLODE,
                "&c&l몰로토프 칵테일 — 화염 폭발.", 100, 1, 200);
        s("apoc_mutant_smash", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.SOUL_FIRE_FLAME,
                Particle.BLOCK_DUST, Sound.ENTITY_RAVAGER_ATTACK, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&2&l변종 분쇄 — 거대한 일격.", 60, 3, 0);
        s("apoc_radiation_burst", SkillSignature.ParticlePattern.EXPLOSION, Particle.SPELL_WITCH,
                Particle.SOUL_FIRE_FLAME, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.ENTITY_GENERIC_EXPLODE,
                "&2&l방사능 폭발 — 모든 것이 오염된다.", 200, 3, 60);
        s("apoc_rifle_burst", SkillSignature.ParticlePattern.BEAM, Particle.CRIT,
                Particle.SMOKE_NORMAL, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&7라이플 연사.", 0, 0, 0);
    }

    // ──────────────────── 선계 추가 (5) ────────────────────
    private static void seedImmortalExtra() {
        s("yeongi_basic", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.BLOCK_BEACON_AMBIENT, null,
                "&b영기 운기 — 천기가 몸을 돈다.", 400, 0, 0);
        s("talisman_basic", SkillSignature.ParticlePattern.BEAM, Particle.SPELL_INSTANT,
                Particle.END_ROD, Sound.ENTITY_VILLAGER_HURT, Sound.ITEM_BOOK_PAGE_TURN,
                "&f부적 발동.", 0, 0, 0);
        s("chuhon_daebeop", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.SOUL,
                Particle.PORTAL, Sound.ENTITY_VEX_DEATH, Sound.ENTITY_ENDER_EYE_DEATH,
                "&5&l추혼대법 — 영혼을 끌어당긴다.", 100, 4, 0);
        s("cheonhwa_bongsin_gyeol", SkillSignature.ParticlePattern.STAR_BURST, Particle.END_ROD,
                Particle.TOTEM, Sound.BLOCK_BELL_RESONATE, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&e&l천화봉신결 — 천 송이 꽃이 신을 봉한다.", 200, 5, 100);
        s("cheongi_birok", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.NOTE, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&3&l천기비록 — 천기가 누설된다.", 600, 1, 0);
    }

    // ──────────────────── 잡 추가 (12) ────────────────────
    private static void seedExtra() {
        s("dark_smasher", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.SQUID_INK,
                Particle.SOUL, Sound.ENTITY_WITHER_HURT, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&0&l다크 스매셔 — 어둠으로 짓이긴다.", 0, 0, 0);
        s("storm_swordplay", SkillSignature.ParticlePattern.STORM, Particle.CLOUD,
                Particle.SWEEP_ATTACK, Sound.ITEM_TRIDENT_THUNDER, Sound.ITEM_TRIDENT_RIPTIDE_2,
                "&3&l스톰 검술 — 폭풍처럼 베어낸다.", 80, 2, 0);
        s("iron_breaker", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CRIT,
                Particle.BLOCK_DUST, Sound.BLOCK_ANVIL_LAND, Sound.ENTITY_RAVAGER_ATTACK,
                "&7&l아이언 브레이커 — 갑옷을 부순다.", 60, 3, 0);
        s("night_fang", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SQUID_INK,
                Particle.CRIT, Sound.ENTITY_VEX_HURT, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&8&l나이트 팽 — 어둠의 송곳니.", 40, 2, 0);
        s("dragonslayer_blade", SkillSignature.ParticlePattern.TWIN_BEAM, Particle.DRAGON_BREATH,
                Particle.CRIT_MAGIC, Sound.ENTITY_ENDER_DRAGON_HURT, Sound.ITEM_TRIDENT_THUNDER,
                "&6&l용살검 — 용의 비늘도 베는 검.", 60, 4, 0);
        s("silvania_green_blade", SkillSignature.ParticlePattern.SLASH_ARC, Particle.VILLAGER_HAPPY,
                Particle.SWEEP_ATTACK, Sound.BLOCK_GRASS_BREAK, Sound.ITEM_TRIDENT_THROW,
                "&a실바니아 녹색 검 — 자연의 칼.", 0, 0, 0);
        s("kaiser_imperial", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.END_ROD,
                Particle.TOTEM, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&6&l카이저 임페리얼 — 황제의 일격.", 0, 0, 0);
        s("nameless_swordplay", SkillSignature.ParticlePattern.SLASH_ARC, Particle.CRIT_MAGIC,
                null, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&8무명 검술 — 이름 없는 자의 검.", 0, 0, 0);
        s("arteon_royal_blade", SkillSignature.ParticlePattern.STAR_BURST, Particle.END_ROD,
                Particle.CRIT_MAGIC, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&6&l아르테온 로얄 블레이드 — 왕가의 검.", 60, 3, 0);
        s("heart_sword", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.CRIT, Sound.ITEM_TRIDENT_THROW, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&d&l심검 — 마음의 검.", 0, 0, 0);
        s("nightmare_grimoire", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SPELL_WITCH, Sound.ITEM_BOOK_PAGE_TURN, Sound.ENTITY_PHANTOM_AMBIENT,
                "&8&l악몽의 마도서.", 200, 4, 0);
        s("nameless_assassin", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SPELL_WITCH, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&8무명 암살 — 보이지 않는 죽음.", 100, 0, 0);
    }

    // ──────────────────── 무공 풀-시드 (45) ────────────────────
    private static void seedMartialFull() {
        s("basic_slash", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SWEEP_ATTACK,
                null, Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&7기본 베기.", 0, 0, 0);
        s("heupseong_daebeop", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.REDSTONE,
                Particle.SOUL, Sound.ENTITY_WITCH_DRINK, Sound.ENTITY_VEX_DEATH,
                "&4&l흡성대법 — 적의 내공을 강탈한다.", 100, 3, 0);
        s("hangyong_18jang", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.EXPLOSION_LARGE,
                Particle.DRAGON_BREATH, Sound.ENTITY_ENDER_DRAGON_HURT, Sound.ENTITY_GENERIC_EXPLODE,
                "&6&l항룡십팔장 — 18장의 변화.", 60, 3, 0);
        s("guyang_jingyung", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.FLAME, Sound.BLOCK_BEACON_AMBIENT, Sound.BLOCK_FIRE_AMBIENT,
                "&e&l구양진경(眞功) — 양강의 극의.", 400, 3, 200);
        s("guyang_singong", SkillSignature.ParticlePattern.AURA_OUT, Particle.FLAME,
                Particle.END_ROD, Sound.BLOCK_FIRE_AMBIENT, Sound.BLOCK_BEACON_AMBIENT,
                "&c&l구양신공 — 무궁한 양의 기운.", 200, 2, 100);
        s("manryu_gyijong", SkillSignature.ParticlePattern.STAR_BURST, Particle.FIREWORKS_SPARK,
                Particle.SWEEP_ATTACK, Sound.BLOCK_BELL_RESONATE, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&5&l만류귀종 — 만 가지가 하나로.", 100, 4, 200);
        s("mangeom_gyijong", SkillSignature.ParticlePattern.STAR_BURST, Particle.CRIT_MAGIC,
                Particle.SWEEP_ATTACK, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&6&l만검귀종 — 만 자루 검이 하나로.", 80, 4, 0);
        s("hwagong_daebeop", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.FLAME,
                Particle.SOUL_FIRE_FLAME, Sound.BLOCK_FIRE_AMBIENT, Sound.ENTITY_BLAZE_BURN,
                "&c&l화공대법 — 내공을 태운다.", 100, 2, 0);
        s("hwasan_geombeop", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SOUL_FIRE_FLAME,
                Particle.FLAME, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_BLAZE_AMBIENT,
                "&c&l화산검법 — 화염을 두른 매화.", 60, 2, 0);
        s("hyeolma_daebeop", SkillSignature.ParticlePattern.AURA_IN, Particle.REDSTONE,
                Particle.SQUID_INK, Sound.ENTITY_WITCH_DRINK, Sound.ENTITY_WITHER_HURT,
                "&4&l혈마대법 — 피의 마기가 흐른다.", 200, 3, 100);
        s("hyeoldosul", SkillSignature.ParticlePattern.STORM, Particle.REDSTONE,
                Particle.DRIPPING_LAVA, Sound.ENTITY_WITCH_DRINK, Sound.BLOCK_HONEY_BLOCK_BREAK,
                "&4&l혈도술 — 혈도를 짚어 통제한다.", 100, 4, 0);
        s("hyeonmyung_igigeom", SkillSignature.ParticlePattern.SLASH_ARC, Particle.CRIT_MAGIC,
                Particle.END_ROD, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&5&l현명이기검 — 두 자루 검이 하나처럼.", 0, 0, 0);
        s("igieogeomsul", SkillSignature.ParticlePattern.TWIN_BEAM, Particle.CRIT,
                Particle.SWEEP_ATTACK, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&7&l이기어검술 — 검이 손을 떠나 적을 벤다.", 80, 2, 60);
        s("ihwa_jeommok", SkillSignature.ParticlePattern.BEAM, Particle.CRIT,
                Particle.SOUL, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_PLAYER_HURT,
                "&5&l이화접목 — 적의 힘을 적에게 돌린다.", 40, 0, 0);
        s("ilyangji", SkillSignature.ParticlePattern.BEAM, Particle.END_ROD,
                Particle.CRIT_MAGIC, Sound.BLOCK_BELL_USE, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&e&l일양지 — 한 손가락의 양강.", 0, 0, 0);
        s("tanji_sintong", SkillSignature.ParticlePattern.BEAM, Particle.CRIT_MAGIC,
                Particle.SPELL_INSTANT, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&7&l탄지신통 — 손가락 하나로 천 리를.", 0, 0, 0);
        s("jeomhyul_su", SkillSignature.ParticlePattern.BEAM, Particle.REDSTONE,
                Particle.CRIT, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_GENERIC_HURT,
                "&c&l점혈수 — 혈을 짚어 마비시킨다.", 200, 9, 0);
        s("geumjongjo", SkillSignature.ParticlePattern.AURA_OUT, Particle.NAUTILUS,
                Particle.END_ROD, Sound.BLOCK_ANVIL_LAND, Sound.ENTITY_IRON_GOLEM_HURT,
                "&6&l금종조 — 금종으로 두른 몸.", 400, 4, 200);
        s("geumsa_simbeop", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.SPELL_WITCH, Sound.BLOCK_BEACON_AMBIENT, null,
                "&7&l금사심법 — 금사처럼 가벼운 운기.", 400, 1, 0);
        s("guhum_jingyung", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_VEX_AMBIENT, Sound.BLOCK_BONE_BLOCK_BREAK,
                "&8&l구함진경 — 구함의 깊은 음공.", 400, 3, 0);
        s("guhum_baekgoljo", SkillSignature.ParticlePattern.STORM, Particle.SOUL,
                Particle.BLOCK_DUST, Sound.ENTITY_SKELETON_AMBIENT, Sound.BLOCK_BONE_BLOCK_BREAK,
                "&8&l구함백골조 — 백골의 발톱.", 60, 3, 0);
        s("gujeon_hyeongong", SkillSignature.ParticlePattern.SPIRAL, Particle.SPELL_WITCH,
                Particle.SOUL, Sound.BLOCK_PORTAL_AMBIENT, Sound.ENTITY_VEX_AMBIENT,
                "&5&l구전현공 — 9번 변하는 깊은 공력.", 200, 2, 0);
        s("eumyang_mugeuk_gong", SkillSignature.ParticlePattern.SPIRAL, Particle.SOUL_FIRE_FLAME,
                Particle.END_ROD, Sound.BLOCK_BELL_USE, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&8&l음양무극공 — 음과 양의 극이 만난다.", 600, 4, 200);
        s("geongon_daenai", SkillSignature.ParticlePattern.STAR_BURST, Particle.PORTAL,
                Particle.END_ROD, Sound.BLOCK_PORTAL_AMBIENT, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&6&l건곤대내 — 하늘과 땅을 옮긴다.", 60, 3, 0);
        s("gyuhwa_bojeon", SkillSignature.ParticlePattern.AURA_OUT, Particle.VILLAGER_HAPPY,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&a&l규화보전 — 그러나 그 대가는…", 600, 4, 200);
        s("hondol_simbeop", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_WITCH,
                Particle.SQUID_INK, Sound.ENTITY_VEX_AMBIENT, null,
                "&5&l혼돌심법 — 정신을 어지럽게 한다.", 400, 2, 0);
        s("hwangol_taltae", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.TOTEM, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_PLAYER_LEVELUP,
                "&e&l환골탈태 — 뼈를 바꾸고 태를 빠진다.", 0, 0, 200);
        s("heukpungjang", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CLOUD,
                Particle.SQUID_INK, Sound.ENTITY_PHANTOM_FLAP, Sound.ENTITY_RAVAGER_ROAR,
                "&8&l흑풍장 — 검은 바람의 장법.", 60, 2, 0);
        s("musang_singong", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.END_ROD, Sound.BLOCK_BEACON_AMBIENT, null,
                "&5&l무상신공 — 형상 없는 신공.", 600, 3, 200);
        s("muyeong_sinbeop", SkillSignature.ParticlePattern.AURA_IN, Particle.SPELL_INSTANT,
                Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT, null,
                "&7&l무영신법 — 그림자 없는 발걸음.", 200, 3, 0);
        s("neungpa_mibo", SkillSignature.ParticlePattern.SPIRAL, Particle.WATER_SPLASH,
                Particle.CLOUD, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, null,
                "&3&l능파미보 — 물결을 밟는 가벼운 보법.", 200, 4, 0);
        s("pacheon_singong", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.END_ROD,
                Particle.EXPLOSION_LARGE, Sound.BLOCK_BELL_RESONATE, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&e&l파천신공 — 하늘을 깨는 신공.", 80, 4, 100);
        s("pokpung_geombeop", SkillSignature.ParticlePattern.STORM, Particle.ELECTRIC_SPARK,
                Particle.SWEEP_ATTACK, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&3&l폭풍검법 — 폭풍처럼 휘몰아치는 검.", 80, 2, 0);
        s("saengsaengbu", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.VILLAGER_HAPPY, Sound.ENTITY_PLAYER_LEVELUP, null,
                "&a&l생생부 — 끝없는 재생.", 600, 2, 0);
        s("sajahu", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CRIT,
                Particle.LAVA, Sound.ENTITY_RAVAGER_ROAR, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&6&l사자후 — 사자의 포효.", 60, 3, 0);
        s("seonyeo_sanhwasu", SkillSignature.ParticlePattern.STORM, Particle.VILLAGER_HAPPY,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.ENTITY_PLAYER_HURT,
                "&d&l선녀산화수 — 흩날리는 꽃잎의 손.", 60, 2, 0);
        s("sinjo_dobeop", SkillSignature.ParticlePattern.SLASH_ARC, Particle.SWEEP_ATTACK,
                Particle.CRIT, Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.ENTITY_PHANTOM_FLAP,
                "&8&l신조도법 — 신비한 새의 도법.", 0, 0, 0);
        s("sippal_bangmuyae", SkillSignature.ParticlePattern.RING, Particle.SWEEP_ATTACK,
                Particle.CRIT, Sound.ENTITY_PLAYER_ATTACK_SWEEP, null,
                "&7&l십팔반무예 — 18가지 무예를 한 번에.", 0, 0, 0);
        s("sorim_72jeolgi", SkillSignature.ParticlePattern.STAR_BURST, Particle.END_ROD,
                Particle.SWEEP_ATTACK, Sound.BLOCK_BELL_RESONATE, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&6&l소림 72절기 — 한 자락이 폭포처럼.", 100, 4, 100);
        s("soyo_yu", SkillSignature.ParticlePattern.AURA_OUT, Particle.CLOUD,
                Particle.SPELL_INSTANT, Sound.ENTITY_PHANTOM_FLAP, null,
                "&b&l소요유 — 유유자적한 신법.", 400, 1, 0);
        s("surasingong", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL_FIRE_FLAME,
                Particle.REDSTONE, Sound.ENTITY_WITHER_HURT, Sound.ENTITY_VINDICATOR_ATTACK,
                "&4&l수라신공 — 수라의 살의가 흐른다.", 200, 4, 100);
        s("swaegol_myeongjang", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.BLOCK_DUST,
                Particle.CRIT, Sound.BLOCK_ANVIL_LAND, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&7&l쇄골명장 — 뼈를 부수는 명품.", 60, 3, 0);
        s("taeguk_hwonwon", SkillSignature.ParticlePattern.SPIRAL, Particle.SOUL_FIRE_FLAME,
                Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.BLOCK_BELL_USE,
                "&7&l태극혼원 — 음양 혼원의 극.", 400, 3, 100);
        s("taehyeon_gyeong", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.SPELL_WITCH, Sound.BLOCK_BEACON_AMBIENT, null,
                "&5&l태현경 — 현묘한 경지.", 600, 3, 200);
        s("yeokeun_seisukyeong", SkillSignature.ParticlePattern.AURA_OUT, Particle.NAUTILUS,
                Particle.VILLAGER_HAPPY, Sound.ENTITY_PLAYER_LEVELUP, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&a&l역근세수경 — 근육과 정수를 바꾼다.", 400, 2, 0);
        s("yeoraesinjang", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.BLOCK_BELL_RESONATE, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&e&l여래신장 — 여래의 거대한 손바닥.", 60, 4, 100);
        s("yukmaek_singeom", SkillSignature.ParticlePattern.BEAM, Particle.SWEEP_ATTACK,
                Particle.CRIT_MAGIC, Sound.ITEM_TRIDENT_THUNDER, Sound.ITEM_TRIDENT_THROW,
                "&5&l육맥신검 — 6맥에서 뻗어나오는 검.", 0, 0, 0);
        s("mi_in_gwon", SkillSignature.ParticlePattern.RING, Particle.HEART,
                Particle.VILLAGER_HAPPY, Sound.ENTITY_CAT_PURREOW, Sound.ENTITY_PLAYER_HURT,
                "&d&l미인권 — 부드러우나 치명적.", 0, 0, 0);
        s("dumi_jiyi", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_WITCH,
                Particle.END_ROD, Sound.BLOCK_BEACON_AMBIENT, null,
                "&5&l두미지의 — 두미의 양생법.", 600, 1, 0);
        s("tagubongbeop", SkillSignature.ParticlePattern.SLASH_ARC, Particle.CRIT,
                null, Sound.BLOCK_BAMBOO_HIT, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&a&l타구봉법 — 개를 때리는 봉의 법.", 0, 0, 0);
        s("tagyeok_36ro_bongbeop", SkillSignature.ParticlePattern.STORM, Particle.CRIT,
                Particle.CLOUD, Sound.BLOCK_BAMBOO_HIT, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&7&l타격36로 봉법 — 36 변화의 봉법.", 0, 0, 0);
        s("oknyeo_simgyeong", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.SPELL_INSTANT, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&d&l옥녀심경 — 옥같이 맑은 마음의 경.", 600, 2, 100);
    }

    // ──────────────────── 도술 (12) ────────────────────
    private static void seedTaoist() {
        s("tao_barrier", SkillSignature.ParticlePattern.AURA_OUT, Particle.NAUTILUS,
                Particle.SPELL_WITCH, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.ITEM_BOOK_PAGE_TURN,
                "&b&l도사 결계 — 사악함이 들지 못한다.", 400, 3, 100);
        s("tao_cloud_step", SkillSignature.ParticlePattern.AURA_OUT, Particle.NAUTILUS,
                Particle.CLOUD, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.ENTITY_PHANTOM_FLAP,
                "&b&l운보 — 구름을 밟고 걷는다.", 200, 3, 0);
        s("tao_exorcism", SkillSignature.ParticlePattern.BEAM, Particle.END_ROD,
                Particle.SQUID_INK, Sound.BLOCK_BELL_USE, Sound.ENTITY_VEX_DEATH,
                "&f&l퇴마 — 악한 영을 쫓는다.", 80, 4, 0);
        s("tao_fire_talisman", SkillSignature.ParticlePattern.BEAM, Particle.FLAME,
                Particle.LAVA, Sound.ENTITY_BLAZE_SHOOT, Sound.ITEM_BOOK_PAGE_TURN,
                "&c&l화부 — 부적이 불꽃이 된다.", 80, 2, 60);
        s("tao_flying_sword", SkillSignature.ParticlePattern.BEAM, Particle.CRIT_MAGIC,
                Particle.SWEEP_ATTACK, Sound.ITEM_TRIDENT_THROW, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&7&l비검 — 검이 스스로 날아간다.", 0, 0, 60);
        s("tao_pill_heal", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.VILLAGER_HAPPY, Sound.ITEM_BOTTLE_FILL, Sound.ENTITY_PLAYER_LEVELUP,
                "&a&l환단 — 도가의 영약.", 0, 0, 0);
        s("tao_thunder_seal", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.ELECTRIC_SPARK,
                Particle.END_ROD, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ITEM_BOOK_PAGE_TURN,
                "&e&l뇌인 — 천둥을 가두는 부적.", 100, 4, 0);
        s("taeeul_noebeop", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.ELECTRIC_SPARK,
                Particle.END_ROD, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ITEM_TRIDENT_THUNDER,
                "&e&l태을뢰법 — 태을의 뇌격.", 80, 4, 60);
        s("taeeul_noebeop_immortal", SkillSignature.ParticlePattern.STAR_BURST, Particle.ELECTRIC_SPARK,
                Particle.END_ROD, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ITEM_TRIDENT_THUNDER,
                "&e&l태을뢰법(仙) — 선계의 뇌격.", 100, 5, 100);
        s("essence_collect", SkillSignature.ParticlePattern.AURA_IN, Particle.SPELL_MOB_AMBIENT,
                Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&b&l정수 수집 — 원소 정수를 모은다.", 0, 0, 0);
        s("dimension_travel", SkillSignature.ParticlePattern.AURA_IN, Particle.SPELL_MOB_AMBIENT,
                Particle.PORTAL, Sound.BLOCK_PORTAL_AMBIENT, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&5&l차원 여행 — 다른 세계로.", 0, 0, 60);
        s("concept_dominion", SkillSignature.ParticlePattern.STAR_BURST, Particle.END_ROD,
                Particle.SOUL_FIRE_FLAME, Sound.BLOCK_BEACON_ACTIVATE, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&5&l개념 지배 — 개념 자체를 바꾼다.", 200, 9, 200);
    }

    // ──────────────────── 천계 풀 (10) ────────────────────
    private static void seedHeavenFull() {
        s("holy_light", SkillSignature.ParticlePattern.BEAM, Particle.GLOW,
                Particle.FIREWORKS_SPARK, Sound.BLOCK_BELL_USE, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&e&l성광 — 빛의 화살.", 60, 1, 0);
        s("heaven_angel_wings", SkillSignature.ParticlePattern.AURA_OUT, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.ENTITY_PHANTOM_FLAP, Sound.BLOCK_BEACON_ACTIVATE,
                "&e&l천사의 날개 — 비행 + 빛.", 600, 0, 0);
        s("heaven_heal_light", SkillSignature.ParticlePattern.AURA_OUT, Particle.GLOW,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.ENTITY_PLAYER_LEVELUP,
                "&b&l치유의 빛.", 60, 0, 0);
        s("heaven_holy_bolt", SkillSignature.ParticlePattern.BEAM, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&e&l성스러운 화살.", 0, 0, 0);
        s("heaven_holy_chain", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.END_ROD,
                Particle.SPELL_INSTANT, Sound.BLOCK_CHAIN_BREAK, Sound.BLOCK_BELL_USE,
                "&e&l성스러운 사슬 — 사악함을 묶는다.", 200, 4, 0);
        s("heaven_judgment", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.FLAME,
                Particle.EXPLOSION_LARGE, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.BLOCK_BELL_RESONATE,
                "&6&l천계 심판 — 신의 분노가 떨어진다.", 100, 5, 80);
        s("heaven_light_shield", SkillSignature.ParticlePattern.AURA_OUT, Particle.GLOW,
                Particle.FIREWORKS_SPARK, Sound.BLOCK_BELL_USE, Sound.BLOCK_BEACON_AMBIENT,
                "&e&l빛의 방패.", 200, 3, 0);
        s("heaven_purify", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&f&l정화의 빛.", 80, 0, 0);
        s("heaven_smite", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.END_ROD,
                Particle.FLAME, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.BLOCK_FIRE_AMBIENT,
                "&e&l천벌 — 빛의 기둥.", 60, 3, 0);
        s("miracle_heal", SkillSignature.ParticlePattern.STAR_BURST, Particle.HEART,
                Particle.END_ROD, Sound.UI_TOAST_CHALLENGE_COMPLETE, Sound.ENTITY_PLAYER_LEVELUP,
                "&b&l기적의 치유.", 0, 0, 0);
        s("divine_punishment", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.LAVA,
                Particle.EXPLOSION_LARGE, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ENTITY_GENERIC_EXPLODE,
                "&c&l천벌 — 신의 분노가 떨어진다.", 100, 5, 0);
        s("healing_hands", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.SPELL_INSTANT, Sound.ENTITY_PLAYER_LEVELUP, null,
                "&a&l치유의 손길.", 0, 0, 0);
    }

    // ──────────────────── 마계 풀 (10) ────────────────────
    private static void seedDemonFull() {
        s("demon_chain_of_pain", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.SQUID_INK,
                Particle.SOUL_FIRE_FLAME, Sound.BLOCK_CHAIN_BREAK, Sound.ENTITY_WITHER_HURT,
                "&4&l고통의 사슬 — 적이 묶인다.", 200, 4, 0);
        s("demon_corruption", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SOUL, Sound.ENTITY_VEX_AMBIENT, Sound.ENTITY_WITCH_DRINK,
                "&5&l부패 — 적의 살이 썩어들어간다.", 200, 3, 0);
        s("demon_hellhound", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.SOUL_FIRE_FLAME,
                Particle.CRIT, Sound.ENTITY_WOLF_GROWL, Sound.ENTITY_HORSE_DEATH,
                "&4지옥견 소환.", 0, 0, 0);
        s("demon_lord_authority", SkillSignature.ParticlePattern.AURA_OUT, Particle.SQUID_INK,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_WITHER_AMBIENT, Sound.ENTITY_ENDER_DRAGON_GROWL,
                "&4&l마왕의 권위 — 모두 무릎을 꿇어라.", 200, 5, 100);
        s("demon_maggi_blast", SkillSignature.ParticlePattern.EXPLOSION, Particle.SQUID_INK,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_GENERIC_EXPLODE, Sound.ENTITY_WITHER_HURT,
                "&5마기 폭발.", 60, 2, 0);
        s("demon_maggi_shield", SkillSignature.ParticlePattern.AURA_OUT, Particle.SMOKE_LARGE,
                Particle.DRAGON_BREATH, Sound.BLOCK_BEACON_ACTIVATE, Sound.ENTITY_WITHER_AMBIENT,
                "&5마기 방패 — 마기로 두른다.", 200, 3, 0);
        s("demon_shadow_dash", SkillSignature.ParticlePattern.AURA_IN, Particle.SQUID_INK,
                Particle.SOUL, Sound.ENTITY_ENDERMAN_TELEPORT, null,
                "&8그림자 돌진.", 0, 0, 0);
        s("demon_soul_drain", SkillSignature.ParticlePattern.SOUL_DRAIN, Particle.SOUL_FIRE_FLAME,
                Particle.SOUL, Sound.ENTITY_WITHER_HURT, Sound.ENTITY_PHANTOM_DEATH,
                "&8&l영혼 흡수 — 적의 영혼을 빨아들인다.", 100, 4, 0);
        s("dragon_fear", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.DRAGON_BREATH,
                Particle.SQUID_INK, Sound.ENTITY_ENDER_DRAGON_GROWL, null,
                "&c&l용의 공포 — 적이 도망친다.", 200, 5, 0);
        s("curse_of_eternity", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL,
                Particle.SQUID_INK, Sound.ENTITY_VEX_AMBIENT, Sound.BLOCK_BELL_RESONATE,
                "&0&l영원의 저주 — 영영 풀리지 않는다.", Integer.MAX_VALUE, 2, 0);
        s("magic_missile_storm", SkillSignature.ParticlePattern.STORM, Particle.CRIT_MAGIC,
                Particle.SPELL_INSTANT, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, Sound.ENTITY_GENERIC_EXPLODE,
                "&5&l마법탄 폭풍.", 40, 2, 0);
    }

    // ──────────────────── 요계 풀 (7) ────────────────────
    private static void seedYokaiFull() {
        s("yokai_beast_form", SkillSignature.ParticlePattern.AURA_OUT, Particle.SOUL_FIRE_FLAME,
                Particle.SPELL_WITCH, Sound.ENTITY_FOX_AGGRO, Sound.ENTITY_WOLF_GROWL,
                "&5요수 변신 — 야성으로 돌아간다.", 1200, 3, 0);
        s("yokai_clone", SkillSignature.ParticlePattern.AURA_OUT, Particle.PORTAL,
                Particle.SOUL_FIRE_FLAME, Sound.ENTITY_ENDERMAN_TELEPORT, null,
                "&5분신 — 여러 너가 나타난다.", 400, 2, 100);
        s("yokai_illusion", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_MOB,
                Particle.HEART, Sound.ENTITY_FOX_AMBIENT, Sound.ENTITY_CAT_PURREOW,
                "&d환술 — 진실이 흐려진다.", 200, 3, 0);
        s("yokai_kangshi", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.SOUL_FIRE_FLAME,
                Particle.SOUL, Sound.ENTITY_ZOMBIE_AMBIENT, Sound.ENTITY_SKELETON_AMBIENT,
                "&8강시 소환 — 죽은 자가 일어선다.", 0, 0, 0);
        s("yokai_moonlight_blast", SkillSignature.ParticlePattern.BEAM, Particle.END_ROD,
                Particle.SPELL_WITCH, Sound.BLOCK_BELL_USE, Sound.ENTITY_PHANTOM_AMBIENT,
                "&5&l월광 폭발 — 보름달의 힘.", 80, 3, 0);
    }

    // ──────────────────── 드래곤 풀 (5) ────────────────────
    private static void seedDragonFull() {
        s("dragon_frost_breath", SkillSignature.ParticlePattern.DRAGON_BREATH, Particle.SNOWFLAKE,
                Particle.CLOUD, Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.BLOCK_GLASS_BREAK,
                "&b&l빙룡의 입김 — 모든 것이 얼어붙는다.", 100, 4, 100);
        s("dragon_lightning_breath", SkillSignature.ParticlePattern.DRAGON_BREATH, Particle.ELECTRIC_SPARK,
                Particle.END_ROD, Sound.ENTITY_ENDER_DRAGON_FLAP, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&e&l뇌룡의 입김 — 번개가 쏟아진다.", 100, 4, 100);
        s("dragon_king_roar", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.DRAGON_BREATH,
                Particle.EXPLOSION_LARGE, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_ENDER_DRAGON_HURT,
                "&6&l용왕의 포효 — 반경 30이 떨린다.", 200, 5, 0);
        s("dragon_scale_guard", SkillSignature.ParticlePattern.AURA_OUT, Particle.FLAME,
                Particle.CRIT, Sound.ITEM_TOTEM_USE, null,
                "&c&l비늘의 보호 — 용의 비늘로 두른다.", 600, 4, 0);
        s("dragon_wing_buffet", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.CLOUD,
                Particle.CRIT, Sound.ENTITY_PHANTOM_FLAP, Sound.ENTITY_RAVAGER_ATTACK,
                "&c&l날개 강타 — 용의 날개가 휩쓴다.", 60, 3, 0);
    }

    // ──────────────────── 해양 풀 (4) ────────────────────
    private static void seedOceanFull() {
        s("ocean_current_dash", SkillSignature.ParticlePattern.BEAM, Particle.WATER_SPLASH,
                Particle.BUBBLE_POP, Sound.ENTITY_DOLPHIN_AMBIENT_WATER, null,
                "&3해류 돌진.", 0, 0, 0);
        s("ocean_pearl_heal", SkillSignature.ParticlePattern.AURA_OUT, Particle.NAUTILUS,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, Sound.ENTITY_PLAYER_LEVELUP,
                "&b&l진주 치유 — 진주의 빛.", 0, 0, 0);
        s("ocean_summon_kraken", SkillSignature.ParticlePattern.STAR_BURST, Particle.WATER_BUBBLE,
                Particle.SQUID_INK, Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_GHAST_SCREAM,
                "&5&l크라켄 소환 — 심해의 군주가 강림한다.", 600, 4, 100);
        s("ocean_tidal_wave", SkillSignature.ParticlePattern.WAVE_FRONT, Particle.WATER_SPLASH,
                Particle.WATER_BUBBLE, Sound.ENTITY_PLAYER_SPLASH, Sound.ENTITY_DROWNED_AMBIENT,
                "&3&l해일 — 거대한 파도가 휩쓴다.", 100, 3, 0);
    }

    // ──────────────────── 지구 헌터 (6) ────────────────────
    private static void seedEarthHunter() {
        s("hunter_basic", SkillSignature.ParticlePattern.AURA_OUT, Particle.ENCHANTMENT_TABLE,
                null, Sound.BLOCK_ANVIL_USE, null,
                "&7헌터 기본기.", 0, 0, 0);
        s("earth_adrenaline", SkillSignature.ParticlePattern.AURA_OUT, Particle.HEART,
                Particle.SPELL_INSTANT, Sound.ITEM_BOTTLE_FILL, null,
                "&c아드레날린 부스트.", 200, 2, 0);
        s("earth_battle_drone", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.REDSTONE, Sound.BLOCK_PISTON_EXTEND, Sound.ENTITY_PHANTOM_FLAP,
                "&7전투 드론 소환.", 0, 0, 0);
        s("earth_combat_roll", SkillSignature.ParticlePattern.AURA_IN, Particle.CLOUD,
                Particle.CRIT, Sound.ENTITY_GENERIC_SMALL_FALL, null,
                "&7전투 굴리기.", 40, 0, 0);
        s("earth_grenade", SkillSignature.ParticlePattern.EXPLOSION, Particle.FLAME,
                Particle.EXPLOSION_LARGE, Sound.ENTITY_TNT_PRIMED, Sound.ENTITY_GENERIC_EXPLODE,
                "&c&l수류탄.", 0, 0, 0);
        s("earth_rapid_fire", SkillSignature.ParticlePattern.BEAM, Particle.CRIT,
                Particle.SMOKE_NORMAL, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, null,
                "&7연발 사격.", 0, 0, 0);
        s("earth_snipe", SkillSignature.ParticlePattern.BEAM, Particle.CRIT,
                Particle.SMOKE_NORMAL, Sound.ENTITY_FIREWORK_ROCKET_BLAST, Sound.ENTITY_PLAYER_ATTACK_CRIT,
                "&c&l저격 — 한 발에 끝낸다.", 0, 0, 0);
        s("gun_handling", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_INSTANT,
                null, Sound.BLOCK_ANVIL_USE, null,
                "&7총기 숙련.", 0, 0, 0);
        s("basic_firearm", SkillSignature.ParticlePattern.BEAM, Particle.CRIT,
                Particle.SMOKE_NORMAL, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, Sound.ENTITY_PLAYER_HURT,
                "&7기본 총격.", 0, 0, 0);
        s("survival_kit", SkillSignature.ParticlePattern.AURA_OUT, Particle.VILLAGER_HAPPY,
                Particle.HEART, Sound.ITEM_BOTTLE_FILL, Sound.BLOCK_ANVIL_USE,
                "&a생존 키트 — 응급 처치.", 0, 0, 0);
    }

    // ──────────────────── 마도공학 (5) ────────────────────
    private static void seedMagitechFull() {
        s("magitech_emp", SkillSignature.ParticlePattern.EXPLOSION, Particle.ELECTRIC_SPARK,
                Particle.REDSTONE, Sound.BLOCK_PISTON_EXTEND, Sound.BLOCK_END_PORTAL_FRAME_FILL,
                "&5&lEMP — 모든 기계가 멈춘다.", 100, 4, 0);
        s("magitech_laser", SkillSignature.ParticlePattern.BEAM, Particle.END_ROD,
                Particle.FLAME, Sound.ITEM_TRIDENT_THUNDER, Sound.ENTITY_BLAZE_SHOOT,
                "&c&l마도 레이저.", 0, 0, 60);
        s("magitech_mana_cannon", SkillSignature.ParticlePattern.BEAM, Particle.SPELL_WITCH,
                Particle.END_ROD, Sound.ENTITY_GENERIC_EXPLODE, Sound.ITEM_TRIDENT_THROW,
                "&d&l마나 캐넌.", 60, 4, 0);
        s("magitech_overdrive", SkillSignature.ParticlePattern.AURA_OUT, Particle.ELECTRIC_SPARK,
                Particle.FLAME, Sound.BLOCK_BEACON_POWER_SELECT, null,
                "&c&l과부하 — 출력 200%.", 200, 4, 0);
        s("magitech_turret", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.REDSTONE,
                Particle.ELECTRIC_SPARK, Sound.BLOCK_PISTON_EXTEND, null,
                "&7마도 터렛 설치.", 0, 0, 0);
    }

    // ──────────────────── 정령 풀 (7) ────────────────────
    private static void seedSpiritFull() {
        s("spirit_frost_nova", SkillSignature.ParticlePattern.EXPLOSION, Particle.SNOWFLAKE,
                Particle.CLOUD, Sound.BLOCK_GLASS_BREAK, Sound.BLOCK_PACKED_ICE_BREAK,
                "&b&l얼음 폭발 — 반경 동결.", 100, 4, 0);
        s("spirit_gale_step", SkillSignature.ParticlePattern.AURA_OUT, Particle.CLOUD,
                Particle.SWEEP_ATTACK, Sound.ENTITY_PHANTOM_FLAP, null,
                "&a강풍 보법.", 200, 3, 0);
        s("spirit_harmony", SkillSignature.ParticlePattern.AURA_OUT, Particle.SPELL_MOB,
                Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null,
                "&b&l정령 조화 — 모든 원소 친화.", 600, 1, 0);
        s("spirit_summon_lord", SkillSignature.ParticlePattern.STAR_BURST, Particle.END_ROD,
                Particle.SPELL_MOB, Sound.BLOCK_BEACON_ACTIVATE, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&a&l정령왕 강림.", 0, 0, 200);
        s("spirit_thunder", SkillSignature.ParticlePattern.VERTICAL_PILLAR, Particle.ELECTRIC_SPARK,
                Particle.END_ROD, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, null,
                "&e&l정령 천둥.", 60, 4, 0);
    }

    // ──────────────────── 잡 (마지막 정리) ────────────────────
    private static void seedMisc() {
        s("nightmare_grimoire2", SkillSignature.ParticlePattern.AURA_IN, Particle.SOUL,
                Particle.SPELL_WITCH, Sound.ENTITY_PHANTOM_AMBIENT, null,
                "&8&l악몽 — 잠들수 없다.", 400, 5, 0);
    }

    private static void s(String id, SkillSignature.ParticlePattern p, Particle prim, Particle sec,
                          Sound castS, Sound hitS, String flavor, int sdur, int samp, int trail) {
        SkillSignature sig = new SkillSignature(id, p, prim, sec, castS, hitS, flavor, sdur, samp, trail);
        // 추가 status는 sigma별 직접 add (간소화하려고 일단 빈)
        SIGS.put(id, sig);
    }

    public static SkillSignature lookup(String id) { return SIGS.get(id); }

    public static int size() { return SIGS.size(); }
}
