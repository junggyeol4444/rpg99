package kr.reborn.mob.dungeon;

import kr.reborn.core.data.WorldKey;

import java.util.ArrayList;
import java.util.List;

/**
 * 던전 정의 — 다층 미궁의 구조.
 *
 * 각 던전은 N개의 층(Floor)으로 구성.
 * 각 층은:
 *   - mobIds: 일반 몬스터 목록 (스폰될 후보)
 *   - bossId: 층 보스 (처치 시 다음 층 해금)
 *   - rewardPool: 처치 보상
 *
 * 예: earth_labyrinth (지구계 미궁) — 100층, 매 10층마다 중간 보스
 * 예: dragon_cavern (드래곤 동굴) — 5층, 매 층마다 용
 */
public final class Dungeon {

    public final String id;
    public final String name;
    public final WorldKey world;
    public final int totalFloors;
    public final List<Floor> floors = new ArrayList<>();
    /** 입장 조건: 최소 스탯 총합 */
    public final long minTotalStats;
    /** 클리어 시 영구 보상 — config "rewards.completion" */
    public final List<String> completionRewards = new ArrayList<>();

    public Dungeon(String id, String name, WorldKey world, int totalFloors, long minTotalStats) {
        this.id = id; this.name = name; this.world = world;
        this.totalFloors = totalFloors; this.minTotalStats = minTotalStats;
    }

    public Floor floor(int n) {
        if (n < 1 || n > floors.size()) return null;
        return floors.get(n - 1);
    }

    public static final class Floor {
        public final int number;
        public final String label;
        public final List<String> mobIds = new ArrayList<>();
        /** 0 = 일반층, otherwise 보스 mobId */
        public String bossId;
        public final List<String> rewardItems = new ArrayList<>();
        public long goldReward;

        public Floor(int number, String label) {
            this.number = number; this.label = label;
        }
    }
}
