package kr.reborn.clan.data;

import java.util.Objects;
import java.util.UUID;

public final class Territory {
    public final String world;
    public final int chunkX;
    public final int chunkZ;
    public UUID owner;
    public String clanId = "";

    public Territory(String world, int chunkX, int chunkZ, UUID owner) {
        this.world = world; this.chunkX = chunkX; this.chunkZ = chunkZ; this.owner = owner;
    }

    public String key() { return world + ":" + chunkX + ":" + chunkZ; }

    @Override public boolean equals(Object o) {
        return o instanceof Territory t && t.world.equals(world) && t.chunkX == chunkX && t.chunkZ == chunkZ;
    }
    @Override public int hashCode() { return Objects.hash(world, chunkX, chunkZ); }
}
