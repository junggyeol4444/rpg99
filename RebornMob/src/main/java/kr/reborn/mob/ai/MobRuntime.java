package kr.reborn.mob.ai;

import org.bukkit.Location;

/**
 * 활성 커스텀 몹 1마리의 런타임 상태 (AI가 틱마다 읽고 갱신).
 * MobDef는 불변 정의, MobRuntime은 가변 인스턴스 상태.
 */
public final class MobRuntime {
    public final String defId;
    /** 보스 페이즈 (1~4). */
    public int phase = 1;
    /** 다음 능력 사용 가능 시각(ms). */
    public long nextAction = 0;
    /** 매복형 은신 여부. */
    public boolean hidden = false;
    /** 광폭화 진입 알림 1회용. */
    public boolean enraged = false;
    /** 소환형 누적 소환 수. */
    public int summonCount = 0;
    /** 스폰 위치 (영역형 복귀 기준). */
    public Location home;
    /** 마지막으로 엔티티가 살아있는 것을 확인한 시각 — 디스폰 누수 정리용. */
    public long lastSeen = System.currentTimeMillis();

    public MobRuntime(String defId) { this.defId = defId; }
}
