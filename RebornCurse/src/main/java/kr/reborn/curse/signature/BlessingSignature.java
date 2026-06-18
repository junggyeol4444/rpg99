package kr.reborn.curse.signature;

import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * 축복/저주 시그니처 — 각 effect 적용 시 고유한 시청각·플레이버.
 *
 * 기존엔 모든 축복/저주가 똑같이 "&b[축복] ..." 한 줄로 적용.
 * 시그니처는 각각:
 *   - 다른 입자/사운드
 *   - 다른 적용 메시지 (예: 정령왕의 가호는 "정령들이 너에게 다가온다")
 *   - 다른 활성 중 주기적 입자 (예: 보호막은 매 사이클 빛 발생)
 */
public final class BlessingSignature {

    public final String effectId;
    public final Particle applyParticle;
    public final Particle tickParticle;
    public final Sound applySound;
    public final String applyMessage;
    /** 활성 중 매 사이클 입자 발생 횟수 (0=없음) */
    public final int tickParticleCount;

    public BlessingSignature(String effectId, Particle apply, Particle tick,
                             Sound applySound, String msg, int tickCount) {
        this.effectId = effectId;
        this.applyParticle = apply;
        this.tickParticle = tick;
        this.applySound = applySound;
        this.applyMessage = msg;
        this.tickParticleCount = tickCount;
    }
}
