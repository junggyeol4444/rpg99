package kr.reborn.clan.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Clan {
    public final String id;
    public String name;
    public UUID leader;
    public final Set<UUID> elders = new HashSet<>();
    public final Set<UUID> members = new HashSet<>();
    public int level = 1;
    public long xp = 0;
    public double treasury = 0;
    public String lineage = "";
    public String kingdomId = ""; // 소속 왕국

    public Clan(String id, String name, UUID leader) {
        this.id = id; this.name = name; this.leader = leader;
        members.add(leader);
    }
}
