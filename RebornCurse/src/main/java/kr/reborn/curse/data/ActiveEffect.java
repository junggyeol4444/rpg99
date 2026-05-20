package kr.reborn.curse.data;

/** 플레이어에게 활성화된 효과 1건. */
public final class ActiveEffect {
    public final String id;
    public final EffectDef.Kind kind;
    public long remainingTicks;     // -1 = 영구
    public int stacks;
    public long lastTickAt;          // 시스템 ms
    public boolean berserkActive;
    public long berserkUntil;

    public ActiveEffect(String id, EffectDef.Kind kind, long remainingTicks, int stacks) {
        this.id = id; this.kind = kind;
        this.remainingTicks = remainingTicks; this.stacks = stacks;
        this.lastTickAt = System.currentTimeMillis();
    }

    public boolean isPermanent() { return remainingTicks < 0; }
}
