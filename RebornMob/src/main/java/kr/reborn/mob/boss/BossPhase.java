package kr.reborn.mob.boss;

import java.util.ArrayList;
import java.util.List;

/**
 * 보스 페이즈 — HP% 임계 + 시전 패턴 + 모디파이어.
 *
 * 예: 100~50% phase 1, 50~25% phase 2, 25~0% phase 3 (분노)
 */
public final class BossPhase {

    public final int index;
    /** 진입 HP% (예: 100→1, 50→2, 25→3) */
    public final double enterHpPercent;
    public final List<BossPattern> patterns = new ArrayList<>();
    /** 데미지 모디파이어 (1.0 = 정상, 1.5 = 분노) */
    public final double damageMultiplier;
    /** 받는 데미지 모디파이어 */
    public final double damageTakenMultiplier;
    public final String label;

    public BossPhase(int index, double enterHpPct, double dmgMult, double dmgTakenMult, String label) {
        this.index = index; this.enterHpPercent = enterHpPct;
        this.damageMultiplier = dmgMult;
        this.damageTakenMultiplier = dmgTakenMult;
        this.label = label;
    }
}
