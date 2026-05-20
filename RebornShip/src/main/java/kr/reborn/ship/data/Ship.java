package kr.reborn.ship.data;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Ship {
    public final UUID id = UUID.randomUUID();
    public final UUID owner;
    public String name;
    public int grade;
    public double hp;
    public double maxHp;
    public Location helm;
    public int blockCount;
    public State state = State.DOCKED;

    /** 배를 구성하는 블록의 (월드 절대 좌표 → BlockData) 스냅샷. 이동 시 갱신. */
    public final Map<String, BlockData> blocks = new LinkedHashMap<>();

    /** 헬름 기준 회전(0/90/180/270). 회전 시 누적. */
    public int rotation = 0;

    public Ship(UUID owner, String name, int grade, double hp, Location helm, int blockCount) {
        this.owner = owner; this.name = name; this.grade = grade;
        this.hp = hp; this.maxHp = hp; this.helm = helm; this.blockCount = blockCount;
    }

    public enum State { DOCKED, SAILING, COMBAT, SUNK }

    public static String key(int x, int y, int z) { return x + "," + y + "," + z; }
}
