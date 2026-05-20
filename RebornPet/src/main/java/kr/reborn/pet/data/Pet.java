package kr.reborn.pet.data;

import java.util.UUID;

public final class Pet {
    public final UUID id = UUID.randomUUID();
    public final UUID owner;
    public String name;
    public String mobId;
    public int level = 1;
    public long xp = 0;
    public int bond = 0;
    public Mode mode = Mode.FOLLOW;
    public UUID activeEntityId;

    public Pet(UUID owner, String name, String mobId) {
        this.owner = owner; this.name = name; this.mobId = mobId;
    }

    public enum Mode { ATTACK, DEFEND, FOLLOW, STAY }
}
