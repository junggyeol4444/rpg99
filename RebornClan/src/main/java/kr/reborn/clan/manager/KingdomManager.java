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

    private static final String NS = "RebornClan.kingdom";

    private final RebornClan plugin;
    private final Map<String, Kingdom> kingdoms = new java.util.concurrent.ConcurrentHashMap<>();
    /** kingdomId → {ALLY, ENEMY, AT_WAR, NEUTRAL} */
    private final Map<String, Map<String, Relation>> relations = new java.util.concurrent.ConcurrentHashMap<>();

    public KingdomManager(RebornClan p) {
        this.plugin = p;
        loadAll();
    }

    private void loadAll() {
        try {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            // key: "k.<id>" → "name|king|clans_csv"  또는 "r.<a>.<b>" → "RELATION"
            for (var e : all.entrySet()) {
                if (e.getKey().startsWith("k.")) {
                    String id = e.getKey().substring(2);
                    String[] parts = e.getValue().split("\\|", -1);
                    if (parts.length < 3) continue;
                    try {
                        UUID king = UUID.fromString(parts[1]);
                        Kingdom k = new Kingdom(id, parts[0], king);
                        for (String c : parts[2].split(",")) if (!c.isEmpty()) k.clans.add(c);
                        kingdoms.put(id, k);
                    } catch (Throwable ignored) {}
                } else if (e.getKey().startsWith("r.")) {
                    String[] parts = e.getKey().substring(2).split("\\.", 2);
                    if (parts.length != 2) continue;
                    try {
                        Relation r = Relation.valueOf(e.getValue());
                        relations.computeIfAbsent(parts[0], k -> new java.util.concurrent.ConcurrentHashMap<>()).put(parts[1], r);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    public void saveAll() {
        var kv = kr.reborn.core.RebornCore.get().kv();
        for (Kingdom k : kingdoms.values()) {
            String enc = k.name + "|" + k.king + "|" + String.join(",", k.clans);
            kv.put(NS, null, "k." + k.id, enc);
        }
        for (var e : relations.entrySet()) {
            for (var re : e.getValue().entrySet()) {
                kv.put(NS, null, "r." + e.getKey() + "." + re.getKey(), re.getValue().name());
            }
        }
    }

    private void persistKingdom(Kingdom k) {
        try {
            String enc = k.name + "|" + k.king + "|" + String.join(",", k.clans);
            kr.reborn.core.RebornCore.get().kv().put(NS, null, "k." + k.id, enc);
        } catch (Throwable ignored) {}
    }

    public boolean create(Player king, String id, String name) {
        var clan = plugin.clans().ofPlayer(king.getUniqueId());
        if (clan == null) { Msg.error(king, "가문이 없다."); return false; }
        // 가문주만 왕국 창설 — 일반 멤버가 가문을 왕국 산하로 끌어들이는 행위 차단.
        if (!king.getUniqueId().equals(clan.leader)) {
            Msg.error(king, "가문주만 왕국을 창설할 수 있다.");
            return false;
        }
        // 이미 다른 왕국 소속 가문이면 차단 (두 왕국 동시 소속 방지)
        if (clan.kingdomId != null && !clan.kingdomId.isEmpty()) {
            Msg.error(king, "가문이 이미 " + clan.kingdomId + " 왕국 소속.");
            return false;
        }
        int reqLv = plugin.getConfig().getInt("kingdom.required-clan-level", 7);
        if (clan.level < reqLv) { Msg.error(king, "가문 Lv " + reqLv + " 이상 필요."); return false; }
        if (kingdoms.containsKey(id)) { Msg.error(king, "이미 존재하는 왕국 ID."); return false; }
        Kingdom k = new Kingdom(id, name, king.getUniqueId());
        k.clans.add(clan.id);
        clan.kingdomId = id;
        kingdoms.put(id, k);
        persistKingdom(k);
        Bukkit.broadcastMessage("§6§l[왕국 건국] §f" + king.getName() + "이(가) " + name + " 왕국을 세웠다!");
        Msg.send(king, "&6왕국 건설: " + name);
        return true;
    }

    /**
     * 다른 가문주가 기존 왕국 산하로 자기 가문을 편입.
     * 왕국 왕은 아닌 가문주 한정. 가문주 본인 가문만 가입 가능.
     */
    public boolean join(Player clanLeader, String kingdomId) {
        var clan = plugin.clans().ofPlayer(clanLeader.getUniqueId());
        if (clan == null) { Msg.error(clanLeader, "가문이 없다."); return false; }
        if (!clanLeader.getUniqueId().equals(clan.leader)) {
            Msg.error(clanLeader, "가문주만 가입 가능."); return false;
        }
        if (clan.kingdomId != null && !clan.kingdomId.isEmpty()) {
            Msg.error(clanLeader, "이미 " + clan.kingdomId + " 왕국 소속. 먼저 /kingdom leave."); return false;
        }
        Kingdom k = kingdoms.get(kingdomId);
        if (k == null) { Msg.error(clanLeader, "왕국 없음: " + kingdomId); return false; }
        k.clans.add(clan.id);
        clan.kingdomId = k.id;
        persistKingdom(k);
        Bukkit.broadcastMessage("§6[왕국 가입] §f" + clan.name + " §7가문이 §6"
                + k.name + " §7왕국에 합류.");
        return true;
    }

    /** 가문이 현재 왕국에서 이탈. 왕은 이탈 불가 (왕국 해체는 별도 절차). */
    public boolean leave(Player clanLeader) {
        var clan = plugin.clans().ofPlayer(clanLeader.getUniqueId());
        if (clan == null) { Msg.error(clanLeader, "가문이 없다."); return false; }
        if (!clanLeader.getUniqueId().equals(clan.leader)) {
            Msg.error(clanLeader, "가문주만 이탈 가능."); return false;
        }
        if (clan.kingdomId == null || clan.kingdomId.isEmpty()) {
            Msg.warn(clanLeader, "왕국 소속 없음."); return false;
        }
        Kingdom k = kingdoms.get(clan.kingdomId);
        if (k != null) {
            if (clanLeader.getUniqueId().equals(k.king)) {
                Msg.error(clanLeader, "왕은 이탈 불가. 다른 가문주에게 양위 필요."); return false;
            }
            k.clans.remove(clan.id);
            persistKingdom(k);
            Bukkit.broadcastMessage("§7[왕국 이탈] §f" + clan.name
                    + " §7가문이 §6" + k.name + " §7왕국을 떠났다.");
        }
        clan.kingdomId = "";
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
        persistKingdom(k);
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
        relations.computeIfAbsent(a, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(b, r);
        relations.computeIfAbsent(b, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(a, r);
        try {
            var kv = kr.reborn.core.RebornCore.get().kv();
            kv.put(NS, null, "r." + a + "." + b, r.name());
            kv.put(NS, null, "r." + b + "." + a, r.name());
        } catch (Throwable ignored) {}
    }

    public enum Relation { ALLY, NEUTRAL, ENEMY, AT_WAR }

    public static final class Kingdom {
        public final String id;
        public String name;
        public final UUID king;
        public final Set<String> clans = java.util.concurrent.ConcurrentHashMap.newKeySet();
        public Kingdom(String id, String name, UUID king) {
            this.id = id; this.name = name; this.king = king;
        }
    }
}
