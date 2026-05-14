package kr.reborn.ship.data;

import org.bukkit.Location;

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

    public Ship(UUID owner, String name, int grade, double hp, Location helm, int blockCount) {
        this.owner = owner; this.name = name; this.grade = grade;
        this.hp = hp; this.maxHp = hp; this.helm = helm; this.blockCount = blockCount;
    }

    public enum State { DOCKED, SAILING, COMBAT, SUNK }
}
