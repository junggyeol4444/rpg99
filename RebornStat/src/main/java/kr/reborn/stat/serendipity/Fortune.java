package kr.reborn.stat.serendipity;

import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;

import java.util.Map;

/**
 * 기연(奇緣) 한 건. 기획서 5장 각 세계의 "기연" — 탐험·사냥·채굴·운기 중 극히 낮은 확률로
 * 발견되어 영구 보상(스탯·스킬)을 주는 일회성 사건.
 */
public final class Fortune {

    public enum Trigger { MINE, KILL, EXPLORE, MEDITATE, MANUAL }

    public final String id;
    public final String name;
    public final WorldKey world;     // null = 모든 세계
    public final Trigger trigger;
    public final String param;        // 블록 Material / 몹 id / 바이옴 (비어있으면 무관)
    public final double chance;        // 적격 이벤트당 발동 확률
    public final boolean broadcast;    // 큰 기연은 전체 공지
    public final String skillReward;   // 지급 스킬 id (없으면 "")
    public final String message;
    public final Map<StatType, Double> statRewards;

    public Fortune(String id, String name, WorldKey world, Trigger trigger, String param,
                   double chance, boolean broadcast, String skillReward, String message,
                   Map<StatType, Double> statRewards) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.trigger = trigger;
        this.param = param == null ? "" : param;
        this.chance = chance;
        this.broadcast = broadcast;
        this.skillReward = skillReward == null ? "" : skillReward;
        this.message = message == null ? "" : message;
        this.statRewards = statRewards;
    }
}
