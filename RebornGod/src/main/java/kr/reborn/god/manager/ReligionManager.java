package kr.reborn.god.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class ReligionManager {

    private final RebornGod plugin;
    private final Map<String, Religion> religions = new HashMap<>();

    public ReligionManager(RebornGod p) { this.plugin = p; }

    public boolean create(Player p, String id, String name) {
        God g = plugin.gods().of(p.getUniqueId());
        if (g == null) { Msg.error(p, "신만 교단을 만들 수 있다."); return false; }
        Religion r = new Religion(id, name, p.getUniqueId());
        religions.put(id, r);
        Msg.send(p, "&6교단 창설: " + name);
        return true;
    }

    public void pray(Player p, String religionId) {
        Religion r = religions.get(religionId);
        if (r == null) return;
        r.faith += 1;
        Msg.send(p, "&7기도 — 신앙 +1");
    }

    public Religion get(String id) { return religions.get(id); }
    public java.util.Collection<Religion> all() { return religions.values(); }

    /** NPC가 창시한 교단 등록 (RebornNpcWorldImpactEvent 소비). 신 자격 검사 없음. */
    public Religion registerNpc(String id, String name, String npcId) {
        Religion existing = religions.get(id);
        if (existing != null) return existing;
        Religion r = new Religion(id, name,
                java.util.UUID.nameUUIDFromBytes(("npc:" + npcId).getBytes()));
        religions.put(id, r);
        return r;
    }

    public static final class Religion {
        public final String id;
        public final String name;
        public final java.util.UUID god;
        public double faith = 0;
        public Religion(String id, String name, java.util.UUID god) {
            this.id = id; this.name = name; this.god = god;
        }
    }
}
