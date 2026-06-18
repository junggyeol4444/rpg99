package kr.reborn.skill.buff;

import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * 버프 프로파일 — 각 버프 스킬마다 고유한 효과 조합.
 *
 * 기존 buff() 함수는 5 키워드로 모든 버프를 1가지 효과 set으로 묶었지만,
 * 실제로는 각 버프가 완전히 다른 의미를 가져야 함:
 *   - "결의" ≠ "광폭" ≠ "수면" ≠ "비행"
 *   - "성광" ≠ "마기 폭주"
 *
 * 각 프로파일은:
 *   - 부여할 PotionEffect 목록 (각각 amp/dur)
 *   - 추가 메시지 (스킬마다 다른 플레이버 텍스트)
 *   - 부수 효과 (예: 불꽃 효과 부여, 비행 허용)
 */
public final class BuffProfile {

    public static final class StatusApplication {
        public final PotionEffectType type;
        public final int duration;
        public final int amplifier;
        public final boolean ambient;
        public final boolean particles;
        public StatusApplication(PotionEffectType t, int dur, int amp, boolean ambient, boolean particles) {
            this.type = t; this.duration = dur; this.amplifier = amp;
            this.ambient = ambient; this.particles = particles;
        }
        public static StatusApplication of(PotionEffectType t, int dur, int amp) {
            return new StatusApplication(t, dur, amp, false, true);
        }
    }

    public final String skillId;
    public final List<StatusApplication> statuses = new ArrayList<>();
    /** SideEffect는 코드로 직접 구현된 추가 효과 (비행, 화염 면역 등) */
    public final List<String> sideEffects = new ArrayList<>();

    public BuffProfile(String skillId) {
        this.skillId = skillId;
    }

    public BuffProfile add(StatusApplication s) {
        this.statuses.add(s);
        return this;
    }

    public BuffProfile add(PotionEffectType t, int dur, int amp) {
        return add(StatusApplication.of(t, dur, amp));
    }

    /** 4-arg variant: ambient flag (0=false, 1=true). 다른 호출자들이 ambient=0 명시할 때 사용. */
    public BuffProfile add(PotionEffectType t, int dur, int amp, int ambient) {
        return add(new StatusApplication(t, dur, amp, ambient != 0, true));
    }

    public BuffProfile addSide(String side) {
        this.sideEffects.add(side);
        return this;
    }
}
