package kr.reborn.skill.signature;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * 스킬 고유 시그니처 — 각 스킬마다 다른 비주얼·사운드·연출.
 *
 * SkillType이 "어떻게 작동"이면, SkillSignature는 "어떻게 보이고 들리는지".
 *
 * 적용:
 *   - particle: 시전 시 발생할 입자 패턴
 *   - cast sound: 시전 시 사운드
 *   - hit sound: 명중 시 사운드
 *   - color: 칭호/메시지 색
 *   - flavor text: 시전 시 표시될 고유 문구 (예: "검에 내공을 모았다", "혈마기가 솟구친다")
 *   - extra status: 기본 데미지 외 추가 상태이상
 */
public final class SkillSignature {

    public enum ParticlePattern {
        RING,           // 원형 입자
        BEAM,           // 직선 빔
        SPIRAL,         // 나선형
        EXPLOSION,      // 폭발
        STORM,          // 폭풍 (다수 무작위)
        METEOR_RAIN,    // 유성우
        SLASH_ARC,      // 검기 호 (베기)
        AURA_OUT,       // 자기 중심 발산
        AURA_IN,        // 자기로 흡수
        WAVE_FRONT,     // 정면 파동
        TWIN_BEAM,      // 쌍열 빔
        VERTICAL_PILLAR,// 수직 기둥
        STAR_BURST,     // 별 모양 폭발
        DRAGON_BREATH,  // 용의 입김 (콘)
        SOUL_DRAIN,     // 영혼 흡수 (선)
        TIME_RIPPLE     // 시간 파동
    }

    public final String skillId;
    public final ParticlePattern primaryPattern;
    public final Particle primaryParticle;
    /** 보조 입자 (있으면 같이 발사) */
    public final Particle secondaryParticle;
    public final Sound castSound;
    public final Sound hitSound;
    /** 시전 직전 채팅 표시 — 비어 있으면 일반 메시지 */
    public final String flavorText;
    /** 추가 상태이상 (있으면 적중 시 부여) */
    public final List<PotionEffectType> extraStatuses = new ArrayList<>();
    /** 추가 상태이상 지속 (tick) */
    public final int statusDuration;
    /** 추가 상태이상 강도 */
    public final int statusAmplifier;
    /** 시전 후 잔존 효과 (예: 검기가 5초간 바닥에 남음) */
    public final int afterTrailTicks;

    public SkillSignature(String skillId, ParticlePattern pattern, Particle primary,
                          Particle secondary, Sound castSound, Sound hitSound,
                          String flavor, int statusDur, int statusAmp, int afterTrail) {
        this.skillId = skillId;
        this.primaryPattern = pattern;
        this.primaryParticle = primary;
        this.secondaryParticle = secondary;
        this.castSound = castSound;
        this.hitSound = hitSound;
        this.flavorText = flavor;
        this.statusDuration = statusDur;
        this.statusAmplifier = statusAmp;
        this.afterTrailTicks = afterTrail;
    }
}
