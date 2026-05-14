package kr.reborn.stat.growth;

import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import org.bukkit.entity.Player;

/** 세계별 성장 전략. Strategy 패턴. */
public interface GrowthStrategy {
    WorldKey world();

    /** 몬스터 처치 시 호출. */
    void onMonsterKill(Player p, PlayerData d, double mobLevel);

    /** 퀘스트 완료 시 호출. */
    void onQuestComplete(Player p, PlayerData d, double weight);

    /** 수련(운기조식·명상 등) 시 호출. */
    void onMeditate(Player p, PlayerData d, double quality);
}
