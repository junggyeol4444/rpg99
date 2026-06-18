package kr.reborn.skill.technique;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;

/**
 * 유명 초식별 입자·사운드 매핑.
 *
 * 초식 이름(name) prefix 매칭으로 찾는다.
 * 예: "파검식" 시작하면 SWEEP_ATTACK + 강한 베기
 *     "파기식" 시작하면 SOUL_FIRE_FLAME + 천둥 (검기를 파해함)
 *
 * 등록 안 된 초식은 부모 스킬 시그니처 사용 (fallback).
 */
public final class TechniqueFlavor {

    public static final class Flavor {
        public final Particle particle;
        public final Sound sound;
        public final String flavor;
        public Flavor(Particle p, Sound s, String f) {
            this.particle = p; this.sound = s; this.flavor = f;
        }
    }

    private static final Map<String, Flavor> M = new HashMap<>();

    static {
        // ─── 독고구검 9초식 ───
        register("총결식", Particle.END_ROD, Sound.BLOCK_BELL_RESONATE,
                "&7검의 모든 흐름이 보인다 …");
        register("파검식", Particle.SWEEP_ATTACK, Sound.ITEM_TRIDENT_THUNDER,
                "&3검을 파한다 — 검결의 첫 변.");
        register("파도식", Particle.CRIT, Sound.ITEM_TRIDENT_RIPTIDE_2,
                "&3도를 파한다 — 도의 길을 끊는다.");
        register("파창식", Particle.SOUL_FIRE_FLAME, Sound.ITEM_TRIDENT_THROW,
                "&6창을 파한다 — 길이를 무시한다.");
        register("파편식", Particle.CRIT_MAGIC, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&d편을 파한다 — 휘어지는 모든 것을 베어낸다.");
        register("파삭식", Particle.SQUID_INK, Sound.ENTITY_PLAYER_ATTACK_STRONG,
                "&8삭을 파한다 — 묶임을 끊는다.");
        register("파장식", Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                "&c장을 파한다 — 거대한 손바닥도 갈라낸다.");
        register("파전식", Particle.CRIT, Sound.ENTITY_FIREWORK_ROCKET_SHOOT,
                "&7전을 파한다 — 날아오는 것을 베어낸다.");
        register("파기식", Particle.LAVA, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&c&l기를 파한다 — 검기마저도 베어낸다.");

        // ─── 항룡십팔장 18장 (대표 6) ───
        register("잠룡물용", Particle.SOUL_FIRE_FLAME, Sound.BLOCK_BEACON_AMBIENT,
                "&6용이 잠겨 있다 — 힘을 모은다.");
        register("견룡재전", Particle.END_ROD, Sound.ITEM_TRIDENT_RIPTIDE_1,
                "&6용이 들에 나타났다 — 정정당당.");
        register("종일건건", Particle.FLAME, Sound.ITEM_TRIDENT_RIPTIDE_2,
                "&6쉼 없이 굳세고 굳세다 — 연속의 일격.");
        register("혹약재연", Particle.WATER_SPLASH, Sound.ENTITY_DOLPHIN_AMBIENT_WATER,
                "&3혹은 연못에 뛰어든다 — 변화막측.");
        register("비룡재천", Particle.END_ROD, Sound.ENTITY_ENDER_DRAGON_FLAP,
                "&6&l용이 하늘을 난다 — 정점의 일격.");
        register("항룡유회", Particle.EXPLOSION_HUGE, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                "&c&l지나친 용은 후회한다 — 18장의 절기.");

        // ─── 구양진경 ───
        register("구양", Particle.FLAME, Sound.BLOCK_FIRE_AMBIENT,
                "&e양강의 정수가 단전에 모인다.");
        register("양강", Particle.LAVA, Sound.BLOCK_FIRE_AMBIENT,
                "&c순양의 기운이 폭발한다.");

        // ─── 태극권 ───
        register("음양", Particle.SOUL_FIRE_FLAME, Sound.BLOCK_BELL_USE,
                "&7음양이 맞물린다.");
        register("태극", Particle.SOUL, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                "&7태극의 도리가 흐른다.");

        // ─── 천마신공 ───
        register("천마", Particle.SQUID_INK, Sound.ENTITY_WITHER_AMBIENT,
                "&5&l천마의 기운이 강림한다.");
        register("강림", Particle.DRAGON_BREATH, Sound.ENTITY_ENDER_DRAGON_GROWL,
                "&5&l마의 신이 너에게 깃든다.");

        // ─── 빙잠독경 ───
        register("빙심", Particle.SNOWFLAKE, Sound.BLOCK_GLASS_BREAK,
                "&b만년빙잠의 한기.");
        register("독장", Particle.SPELL_WITCH, Sound.ENTITY_SPIDER_HURT,
                "&2독이 손바닥에서 흘러나온다.");

        // ─── 검선기 ───
        register("검선", Particle.END_ROD, Sound.BLOCK_BEACON_AMBIENT,
                "&e&l검에 천기가 깃든다.");
        register("선기", Particle.END_ROD, Sound.BLOCK_BEACON_AMBIENT,
                "&e선기가 검을 감싼다.");

        // ─── 일위도강 ───
        register("일위", Particle.WATER_SPLASH, Sound.ENTITY_DOLPHIN_AMBIENT_WATER,
                "&3한 자루 검으로 강을 건넌다.");

        // ─── 만류귀종 ───
        register("귀종", Particle.TOTEM, Sound.UI_TOAST_CHALLENGE_COMPLETE,
                "&5&l모든 길은 하나로 통한다.");

        // ─── 항룡 절기 추가 ───
        register("이견대인", Particle.HEART, Sound.ENTITY_VILLAGER_TRADE,
                "&6큰 사람을 본다 — 인덕의 일장.");
        register("홍점우륙", Particle.HAPPY_VILLAGER, Sound.ENTITY_PHANTOM_FLAP,
                "&6기러기처럼 차차 나아간다.");
        register("돌여기래", Particle.EXPLOSION_NORMAL, Sound.ENTITY_TNT_PRIMED,
                "&6갑자기 닥친다 — 막을 수 없다.");

        // ─── 칠상권 7형 ───
        register("일상", Particle.CRIT, Sound.ENTITY_PLAYER_ATTACK_CRIT, "&c첫 번째 형 — 분노.");
        register("이상", Particle.CRIT, Sound.ENTITY_PLAYER_ATTACK_STRONG, "&c두 번째 형 — 우.");
        register("삼상", Particle.CRIT_MAGIC, Sound.ENTITY_PLAYER_ATTACK_STRONG, "&5세 번째 형 — 사.");
        register("사상", Particle.SQUID_INK, Sound.ENTITY_WITHER_HURT, "&8네 번째 형 — 공포.");
        register("오상", Particle.SOUL_FIRE_FLAME, Sound.ENTITY_BLAZE_HURT, "&c다섯 번째 형 — 경악.");
        register("육상", Particle.SOUL, Sound.ENTITY_VEX_HURT, "&7여섯 번째 형 — 욕망.");
        register("칠상", Particle.TOTEM, Sound.ITEM_TOTEM_USE, "&6일곱 번째 형 — 절명.");

        // ─── 일반 ───
        register("회수", Particle.HEART, Sound.ENTITY_PLAYER_LEVELUP, "&a회수의 기운.");
        register("폭", Particle.EXPLOSION_LARGE, Sound.ENTITY_GENERIC_EXPLODE, "&c폭렬!");
        register("뇌", Particle.ELECTRIC_SPARK, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, "&e뇌격!");
        register("화", Particle.FLAME, Sound.BLOCK_FIRE_AMBIENT, "&c화염!");
        register("빙", Particle.SNOWFLAKE, Sound.BLOCK_GLASS_BREAK, "&b한기!");
        register("암", Particle.SQUID_INK, Sound.ENTITY_VEX_AMBIENT, "&0어둠!");
        register("성", Particle.END_ROD, Sound.BLOCK_BELL_USE, "&e성광!");
        register("혈", Particle.REDSTONE, Sound.ENTITY_WITCH_DRINK, "&4피의 흐름.");
        register("심", Particle.HEART, Sound.BLOCK_AMETHYST_BLOCK_CHIME, "&d마음의 검.");
    }

    private static void register(String prefix, Particle p, Sound s, String flavor) {
        M.put(prefix, new Flavor(p, s, flavor));
    }

    /** 초식 이름의 한자/한글 키워드로 flavor 찾기. */
    public static Flavor lookup(String techniqueName) {
        if (techniqueName == null) return null;
        // (한자) 부분 제거하고 한글만 추출
        String stripped = techniqueName.replaceAll("[(（].*?[)）]", "").trim();
        for (var e : M.entrySet()) {
            if (stripped.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    public static int size() { return M.size(); }
}
