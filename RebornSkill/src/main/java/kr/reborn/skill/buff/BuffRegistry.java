package kr.reborn.skill.buff;

import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * 40+ 고유 버프 프로파일.
 *
 * 각 버프 스킬이 진짜 의미하는 것을 코드로 표현:
 *   "결의" = 추가 데미지 + 흡수 (적과 정면승부)
 *   "광폭" = 데미지 +, 방어 - (위험 감수)
 *   "수면 수련" = 식사 게이지 하한 + 정신 회복 (낮은 활동에서 효율)
 *   "검선" = 모든 검 스킬 데미지 ×1.5 (조건부)
 *   ...
 */
public final class BuffRegistry {

    private static final Map<String, BuffProfile> P = new HashMap<>();

    static {
        seedDefense();
        seedOffense();
        seedMovement();
        seedSpecial();
        seedMartial();
        seedHoly();
        seedDemon();
        seedElement();
        seedTransformations();
        seedShieldVariants();
    }

    private static BuffProfile reg(String id) {
        BuffProfile p = new BuffProfile(id);
        P.put(id, p);
        return p;
    }

    // ──────────── 방어형 (8) ────────────
    private static void seedDefense() {
        // 결의 - 흡수 + 저항
        reg("warrior_resolve")
                .add(PotionEffectType.ABSORPTION, 200, 3)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 1);
        // 철벽 - 절대 방어
        reg("iron_wall")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 4)
                .add(PotionEffectType.SLOW, 200, 2);
        // 금강불괴 - 무공 방어
        reg("vajra_body")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 400, 2)
                .add(PotionEffectType.ABSORPTION, 400, 5)
                .add(PotionEffectType.SLOW, 400, 0);
        // 신성한 방패
        reg("divine_shield")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 100, 5)
                .add(PotionEffectType.ABSORPTION, 100, 3);
        // 빛의 방패
        reg("shield_of_light")
                .add(PotionEffectType.ABSORPTION, 200, 3)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 1)
                .add(PotionEffectType.GLOWING, 200, 0);
        // 부동의 기둥 (축복)
        reg("unmoving_pillar_buff")
                .add(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, 0)
                .add(PotionEffectType.SLOW, Integer.MAX_VALUE, 0);
        // 카오스 방벽
        reg("chaos_barrier")
                .add(PotionEffectType.ABSORPTION, 200, 4)
                .add(PotionEffectType.FIRE_RESISTANCE, 200, 0);
        // 천계 가호
        reg("celestial_grace")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 600, 2)
                .add(PotionEffectType.REGENERATION, 600, 0);
    }

    // ──────────── 공격형 (10) ────────────
    private static void seedOffense() {
        // 광폭 - 데미지 +, 방어 -
        reg("berserk")
                .add(PotionEffectType.INCREASE_DAMAGE, 200, 3)
                .add(PotionEffectType.SPEED, 200, 1)
                .add(PotionEffectType.WEAKNESS, 200, 0);
        // 분노
        reg("rage")
                .add(PotionEffectType.INCREASE_DAMAGE, 300, 2)
                .add(PotionEffectType.FAST_DIGGING, 300, 1);
        // 검선 - 검 스킬 강화
        reg("sword_immortal_aura")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 2)
                .add(PotionEffectType.GLOWING, 600, 0);
        // 마기 폭주
        reg("demon_ki_overflow")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 4)
                .add(PotionEffectType.SPEED, 600, 2)
                .add(PotionEffectType.NIGHT_VISION, 600, 0);
        // 변신
        reg("transform")
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 2)
                .add(PotionEffectType.SPEED, 1200, 1)
                .add(PotionEffectType.HEALTH_BOOST, 1200, 2)
                .add(PotionEffectType.JUMP, 1200, 1);
        // 신의 강림
        reg("avatar_descent")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 4)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 600, 3)
                .add(PotionEffectType.REGENERATION, 600, 2)
                .add(PotionEffectType.GLOWING, 600, 0);
        // 영웅의 후광
        reg("hero_aura")
                .add(PotionEffectType.LUCK, Integer.MAX_VALUE, 1, 0)
                .add(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, 0);
        // 자기 강화 (간단)
        reg("self_buff")
                .add(PotionEffectType.INCREASE_DAMAGE, 200, 1);
        // 검에 마기
        reg("demon_blade")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 2)
                .add(PotionEffectType.WITHER, 600, 0); // 휘두를 때 위더 같이 부여
        // 폭주 (forbidden_power 변형)
        reg("forbidden_power")
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 5)
                .add(PotionEffectType.SPEED, 1200, 3)
                .add(PotionEffectType.JUMP, 1200, 2)
                .add(PotionEffectType.WITHER, 1400, 0);
    }

    // ──────────── 이동/속도 (6) ────────────
    private static void seedMovement() {
        // 헤이스트 — 일반 신속
        reg("haste")
                .add(PotionEffectType.SPEED, 400, 1)
                .add(PotionEffectType.FAST_DIGGING, 400, 1);
        // 신경 가속 (사이버)
        reg("neural_accel")
                .add(PotionEffectType.SPEED, 600, 2)
                .add(PotionEffectType.JUMP, 600, 1)
                .add(PotionEffectType.NIGHT_VISION, 600, 0);
        // 천마군림보 (도)
        reg("cheonma_step")
                .add(PotionEffectType.SPEED, 200, 4)
                .add(PotionEffectType.INVISIBILITY, 200, 0);
        // 무명인 은신
        reg("nameless_stealth")
                .add(PotionEffectType.INVISIBILITY, 220, 0, 0)
                .add(PotionEffectType.SPEED, 220, 1, 0);
        // 비행
        reg("flight")
                .addSide("ALLOW_FLIGHT");
        // 용의 비행
        reg("dragon_breath_aura")
                .addSide("ALLOW_FLIGHT")
                .add(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, 0);
    }

    // ──────────── 특수 (8) ────────────
    private static void seedSpecial() {
        // 행운의 미소
        reg("fortune_smile")
                .add(PotionEffectType.LUCK, Integer.MAX_VALUE, 4, 0);
        // 시간 정지 — 자신만 풀스피드
        reg("time_stop")
                .add(PotionEffectType.SPEED, 100, 5)
                .add(PotionEffectType.JUMP, 100, 3)
                .add(PotionEffectType.FAST_DIGGING, 100, 5);
        // 정령 친화
        reg("spirit_friend")
                .add(PotionEffectType.DOLPHINS_GRACE, 1200, 1)
                .add(PotionEffectType.NIGHT_VISION, 1200, 0);
        // 카오스 (모든 원소)
        reg("chaos_aura")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 2)
                .add(PotionEffectType.FIRE_RESISTANCE, 600, 0)
                .add(PotionEffectType.WATER_BREATHING, 600, 0);
        // 검사 시야
        reg("spirit_sight")
                .add(PotionEffectType.NIGHT_VISION, 1200, 0)
                .add(PotionEffectType.GLOWING, 1200, 0);
        // 게이트 보너스
        reg("gate_resonance")
                .add(PotionEffectType.INCREASE_DAMAGE, 220, 0, 0)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 220, 0, 0)
                .add(PotionEffectType.SPEED, 220, 0, 0);
        // 사이버 면역
        reg("cyber_immune")
                .addSide("REMOVE_CONFUSION_POISON");
        // 꿈의 지배자 패시브
        reg("dream_lord_passive")
                .addSide("DREAM_TRAINING");
    }

    // ──────────── 무공 자기 강화 (4) ────────────
    private static void seedMartial() {
        // 구양진경 (양강)
        reg("guyang_jin")
                .add(PotionEffectType.INCREASE_DAMAGE, 200, 1)
                .add(PotionEffectType.FIRE_RESISTANCE, 200, 0);
        // 천마신공
        reg("cheonma_singong")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 4)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 600, 2)
                .add(PotionEffectType.HEALTH_BOOST, 600, 4);
        // 태극권 (음양 조화)
        reg("taegeuk")
                .add(PotionEffectType.REGENERATION, 200, 1)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 1);
        // 만류귀종 (절기)
        reg("manryu")
                .add(PotionEffectType.INCREASE_DAMAGE, 200, 4)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 3)
                .add(PotionEffectType.SPEED, 200, 2)
                .add(PotionEffectType.REGENERATION, 200, 2)
                .add(PotionEffectType.JUMP, 200, 1);
    }

    // ──────────── 신성 자기 강화 (3) ────────────
    private static void seedHoly() {
        // 성광
        reg("holy_radiance")
                .add(PotionEffectType.GLOWING, 200, 0)
                .add(PotionEffectType.INCREASE_DAMAGE, 200, 1);
        // 천계 가호
        reg("cheonje_grace_buff")
                .add(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, 0)
                .add(PotionEffectType.LUCK, Integer.MAX_VALUE, 0, 0);
        // 정화 (Buff 형태)
        reg("purify_orb")
                .add(PotionEffectType.HEALTH_BOOST, 100, 1)
                .add(PotionEffectType.REGENERATION, 100, 1);
    }

    // ──────────── 마계 자기 강화 (2) ────────────
    private static void seedDemon() {
        // 악마 계약
        reg("demon_pact")
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 3)
                .add(PotionEffectType.SPEED, 600, 1)
                .add(PotionEffectType.WITHER, 600, 0); // 대가
        // 마왕 위압
        reg("demon_lord_aura")
                .add(PotionEffectType.INCREASE_DAMAGE, 200, 2);
    }

    // ──────────── 원소 자기 강화 (4) ────────────
    private static void seedElement() {
        // 화염 갑옷
        reg("flame_armor")
                .add(PotionEffectType.FIRE_RESISTANCE, 600, 0)
                .add(PotionEffectType.INCREASE_DAMAGE, 600, 0);
        // 얼음 갑옷
        reg("frost_armor")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 600, 1)
                .add(PotionEffectType.SLOW, 600, 0); // 자기도 느려짐
        // 번개 가속
        reg("lightning_dash")
                .add(PotionEffectType.SPEED, 200, 4)
                .add(PotionEffectType.JUMP, 200, 2);
        // 대지의 끈기
        reg("earth_endurance")
                .add(PotionEffectType.HEALTH_BOOST, 600, 4)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 600, 1)
                .add(PotionEffectType.SLOW, 600, 1);
    }

    // ──────────── 변신 (8) — yokai/dragon/demon/ocean 컨셉 차별 ────────────
    private static void seedTransformations() {
        // 요수 변신 — 야성 본능 (속도·점프·시야)
        reg("yokai_beast_form")
                .add(PotionEffectType.SPEED, 1200, 2)
                .add(PotionEffectType.JUMP, 1200, 2)
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 1)
                .add(PotionEffectType.NIGHT_VISION, 1200, 0);
        // 인간 변신 — 시민 활동용 (방어 + 위장)
        reg("transform_human")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 600, 1)
                .add(PotionEffectType.HEALTH_BOOST, 600, 1);
        // 요왕 변신 — 광역 위압 (종합 강화)
        reg("transform_roar")
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 3)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 1200, 2)
                .add(PotionEffectType.REGENERATION, 1200, 2)
                .add(PotionEffectType.HEALTH_BOOST, 1200, 3);
        // 용 변신 (가상) — 화염 면역 + 비행
        reg("dragon_form")
                .add(PotionEffectType.FIRE_RESISTANCE, 1200, 0)
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 3)
                .add(PotionEffectType.HEALTH_BOOST, 1200, 4)
                .addSide("ALLOW_FLIGHT");
        // 마기 변신 (수라) — 데미지 + 위더 자기 부담
        reg("demon_transform")
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 4)
                .add(PotionEffectType.SPEED, 1200, 2)
                .add(PotionEffectType.WITHER, 1200, 0); // 대가
        // 인어 변신 — 수중 활성
        reg("ocean_form")
                .add(PotionEffectType.WATER_BREATHING, 1200, 0)
                .add(PotionEffectType.DOLPHINS_GRACE, 1200, 1)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 1200, 1);
        // 정령 융합 — 4원소 친화
        reg("spirit_fusion")
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 2)
                .add(PotionEffectType.FIRE_RESISTANCE, 1200, 0)
                .add(PotionEffectType.WATER_BREATHING, 1200, 0)
                .add(PotionEffectType.SPEED, 1200, 1);
        // 천계 강림 — 신성 + 비행
        reg("angel_form")
                .add(PotionEffectType.REGENERATION, 1200, 2)
                .add(PotionEffectType.INCREASE_DAMAGE, 1200, 2)
                .add(PotionEffectType.GLOWING, 1200, 0)
                .addSide("ALLOW_FLIGHT");
    }

    // ──────────── 보호막 변종 (6) — 종류별 차별 ────────────
    private static void seedShieldVariants() {
        // 마기 방패 — 마기로 두름 (저항 + 위더 부여 시도)
        reg("demon_maggi_shield")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 2)
                .add(PotionEffectType.ABSORPTION, 200, 4);
        // 에너지 실드 — 데미지 흡수 강화
        reg("energy_shield")
                .add(PotionEffectType.ABSORPTION, 200, 5)
                .add(PotionEffectType.GLOWING, 200, 0);
        // 마기 결계
        reg("magic_barrier")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 2)
                .add(PotionEffectType.FIRE_RESISTANCE, 200, 0);
        // 정령 보호 — 4원소 면역
        reg("spirit_shield")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 1)
                .add(PotionEffectType.FIRE_RESISTANCE, 200, 0)
                .add(PotionEffectType.WATER_BREATHING, 200, 0);
        // 사이버 실드 (energy_shield 별칭)
        reg("cyber_shield")
                .add(PotionEffectType.ABSORPTION, 200, 4)
                .add(PotionEffectType.DAMAGE_RESISTANCE, 200, 1);
        // 도사 결계 — 사악 차단 (성격상 정신·시야 보호)
        reg("tao_barrier")
                .add(PotionEffectType.DAMAGE_RESISTANCE, 400, 2)
                .add(PotionEffectType.ABSORPTION, 400, 3)
                .addSide("REMOVE_CONFUSION_POISON");
    }

    public static BuffProfile get(String skillId) {
        return P.get(skillId);
    }

    public static int size() { return P.size(); }
}
