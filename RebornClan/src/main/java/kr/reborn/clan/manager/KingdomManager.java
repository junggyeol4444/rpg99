package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KingdomManager {

    private final RebornClan plugin;
    private final Map<String, Kingdom> kingdoms = new HashMap<>();
    /** kingdomId → {ALLY, ENEMY, AT_WAR, NEUTRAL} */
    private final Map<String, Map<String, Relation>> relations = new HashMap<>();

    public KingdomManager(RebornClan p) { this.plugin = p; }

    public boolean create(Player king, String id, String name) {
        var clan = plugin.clans().ofPlayer(king.getUniqueId());
        if (clan == null) { Msg.error(king, "가문이 없다."); return false; }
        int reqLv = plugin.getConfig().getInt("kingdom.required-clan-level", 7);
        if (clan.level < reqLv) { Msg.error(king, "가문 Lv " + reqLv + " 이상 필요."); return false; }
        if (kingdoms.containsKey(id)) { Msg.error(king, "이미 존재하는 왕국 ID."); return false; }
        Kingdom k = new Kingdom(id, name, king.getUniqueId());
        k.clans.add(clan.id);
        clan.kingdomId = id;
        kingdoms.put(id, k);
        Bukkit.broadcastMessage("§6§l[왕국 건국] §f" + king.getName() + "이(가) " + name + " 왕국을 세웠다!");
        Msg.send(king, "&6왕국 건설: " + name);
        return true;
    }

    public Kingdom get(String id) { return kingdoms.get(id); }

    public Kingdom ofPlayer(UUID p) {
        var clan = plugin.clans().ofPlayer(p);
        if (clan == null || clan.kingdomId == null || clan.kingdomId.isEmpty()) return null;
        return kingdoms.get(clan.kingdomId);
    }

    public java.util.Collection<Kingdom> all() { return kingdoms.values(); }

    /** NPC가 초월(ASCEND)하여 세운 왕국 등록 (RebornNpcWorldImpactEvent 소비). 가문 조건 없음. */
    public Kingdom registerNpc(String id, String name, String npcId) {
        Kingdom existing = kingdoms.get(id);
        if (existing != null) return existing;
        Kingdom k = new Kingdom(id, name, UUID.nameUUIDFromBytes(("npc:" + npcId).getBytes()));
        kingdoms.put(id, k);
        return k;
    }

    public void ally(Player king, String otherKingdomId) {
        Kingdom mine = ofPlayer(king.getUniqueId());
        Kingdom other = kingdoms.get(otherKingdomId);
        if (mine == null || other == null) { Msg.error(king, "왕국 없음"); return; }
        if (!king.getUniqueId().equals(mine.king)) { Msg.error(king, "왕만 외교 가능."); return; }
        setRelation(mine.id, other.id, Relation.ALLY);
        Bukkit.broadcastMessage("§a§l[동맹] §f" + mine.name + " ↔ " + other.name);
    }

    public void declareWar(Player king, String otherKingdomId) {
        Kingdom mine = ofPlayer(king.getUniqueId());
        Kingdom other = kingdoms.get(otherKingdomId);
        if (mine == null || other == null) { Msg.error(king, "왕국 없음"); return; }
        if (!king.getUniqueId().equals(mine.king)) { Msg.error(king, "왕만 외교 가능."); return; }
        setRelation(mine.id, other.id, Relation.AT_WAR);
        Bukkit.broadcastMessage("§4§l[왕국 전쟁 선포] §f" + mine.name + " → " + other.name);
    }

    public void treaty(Player king, String otherKingdomId) {
        Kingdom mine = ofPlayer(king.getUniqueId());
        Kingdom other = kingdoms.get(otherKingdomId);
        if (mine == null || other == null) return;
        setRelation(mine.id, other.id, Relation.NEUTRAL);
        Bukkit.broadcastMessage("§e[휴전] §f" + mine.name + " — " + other.name);
    }

    public void politicalMarriage(Player king, String otherKingdomId, String royalNpcId) {
        Kingdom mine = ofPlayer(king.getUniqueId());
        Kingdom other = kingdoms.get(otherKingdomId);
        if (mine == null || other == null) return;
        if (!king.getUniqueId().equals(mine.king)) { Msg.error(king, "왕만 가능."); return; }
        // 정략 결혼 = 자동 동맹
        setRelation(mine.id, other.id, Relation.ALLY);
        plugin.marriages().marryNpc(king, royalNpcId);
        Bukkit.broadcastMessage("§d§l[정략 결혼] §f" + mine.name + " ❤ " + other.name);
    }

    public Relation getRelation(String a, String b) {
        return relations.getOrDefault(a, Map.of()).getOrDefault(b, Relation.NEUTRAL);
    }

    private void setRelation(String a, String b, Relation r) {
        relations.computeIfAbsent(a, k -> new HashMap<>()).put(b, r);
        relations.computeIfAbsent(b, k -> new HashMap<>()).put(a, r);
    }

    public enum Relation { ALLY, NEUTRAL, ENEMY, AT_WAR }

    public static final class Kingdom {
        public final String id;
        public String name;
        public final UUID king;
        public final Set<String> clans = new HashSet<>();
        public Kingdom(String id, String name, UUID king) {
            this.id = id; this.name = name; this.king = king;
        }
    }
}
