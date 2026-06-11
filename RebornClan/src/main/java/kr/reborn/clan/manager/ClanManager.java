package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Clan;
import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.util.Msg;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanManager {

    private static final String NS = "RebornClan.clan";

    private final RebornClan plugin;
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    /** clanId → 초대된 플레이어 UUID 셋. 초대 없으면 가입 차단. */
    private final Map<String, Set<UUID>> invites = new ConcurrentHashMap<>();
    /** 초대 만료 시각 (clanId + ":" + uuid → expireAt ms). 기본 5분. */
    private final Map<String, Long> inviteExpiry = new ConcurrentHashMap<>();
    private static final long INVITE_TTL_MS = 300_000L;

    public ClanManager(RebornClan p) {
        this.plugin = p;
        loadAll();
        // 1분마다 자동 저장
        RebornCore.get().scheduler().runTimerAsync(this::saveAll, 1200L, 1200L);
    }

    /** 기존 가문들을 KV에서 로드. */
    private void loadAll() {
        try {
            var data = RebornCore.get().kv().loadNamespace(NS);
            for (var entry : data.entrySet()) {
                // owner는 가문 id (UUID 형식이 아니라 빈 문자열 사용 — 가문은 global key)
                // 대신 key가 "clan:<id>.field" 형식으로 저장
            }
            // global namespace 사용 — 가문 ID별로 owner=빈 UUID, key prefix 사용
            String all = RebornCore.get().kv().get(NS, null, "all_ids");
            if (all == null || all.isEmpty()) return;
            for (String cid : all.split(",")) {
                String name = RebornCore.get().kv().get(NS, null, cid + ".name");
                String leader = RebornCore.get().kv().get(NS, null, cid + ".leader");
                if (name == null || leader == null) continue;
                Clan c = new Clan(cid, name, UUID.fromString(leader));
                c.level = RebornCore.get().kv().getInt(NS, null, cid + ".level", 1);
                c.xp = RebornCore.get().kv().getLong(NS, null, cid + ".xp", 0);
                c.treasury = RebornCore.get().kv().getDouble(NS, null, cid + ".treasury", 0);
                c.lineage = RebornCore.get().kv().get(NS, null, cid + ".lineage");
                if (c.lineage == null) c.lineage = "";
                c.kingdomId = RebornCore.get().kv().get(NS, null, cid + ".kingdomId");
                if (c.kingdomId == null) c.kingdomId = "";
                String mems = RebornCore.get().kv().get(NS, null, cid + ".members");
                if (mems != null && !mems.isEmpty()) {
                    for (String mid : mems.split(",")) {
                        try { c.members.add(UUID.fromString(mid)); } catch (Exception ignored) {}
                    }
                }
                String elds = RebornCore.get().kv().get(NS, null, cid + ".elders");
                if (elds != null && !elds.isEmpty()) {
                    for (String mid : elds.split(",")) {
                        try { c.elders.add(UUID.fromString(mid)); } catch (Exception ignored) {}
                    }
                }
                clans.put(cid, c);
            }
            plugin.getLogger().info("Clan 로드: " + clans.size() + "개");
        } catch (Throwable t) {
            plugin.getLogger().warning("Clan loadAll 실패: " + t.getMessage());
        }
    }

    public void saveAll() {
        try {
            String allIds = String.join(",", clans.keySet());
            RebornCore.get().kv().put(NS, null, "all_ids", allIds);
            for (Clan c : clans.values()) {
                RebornCore.get().kv().put(NS, null, c.id + ".name", c.name);
                RebornCore.get().kv().put(NS, null, c.id + ".leader", c.leader.toString());
                RebornCore.get().kv().putInt(NS, null, c.id + ".level", c.level);
                RebornCore.get().kv().putLong(NS, null, c.id + ".xp", c.xp);
                RebornCore.get().kv().putDouble(NS, null, c.id + ".treasury", c.treasury);
                RebornCore.get().kv().put(NS, null, c.id + ".lineage", c.lineage);
                RebornCore.get().kv().put(NS, null, c.id + ".kingdomId", c.kingdomId);
                StringBuilder sbM = new StringBuilder();
                for (UUID u : c.members) {
                    if (sbM.length() > 0) sbM.append(",");
                    sbM.append(u);
                }
                RebornCore.get().kv().put(NS, null, c.id + ".members", sbM.toString());
                StringBuilder sbE = new StringBuilder();
                for (UUID u : c.elders) {
                    if (sbE.length() > 0) sbE.append(",");
                    sbE.append(u);
                }
                RebornCore.get().kv().put(NS, null, c.id + ".elders", sbE.toString());
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Clan saveAll 실패: " + t.getMessage());
        }
    }

    public Clan get(String id) { return clans.get(id); }
    public java.util.Collection<Clan> all() { return clans.values(); }

    public boolean create(Player leader, String id, String name) {
        if (clans.containsKey(id)) { Msg.error(leader, "이미 존재하는 가문 ID."); return false; }
        double total = RebornCore.get().api().getTotalStats(leader.getUniqueId());
        double need = plugin.getConfig().getDouble("clan.create-min-total-stats", 300);
        if (total < need) {
            Msg.error(leader, "가문 창설에는 총합 " + need + " 이상이 필요하다.");
            return false;
        }
        Clan c = new Clan(id, name, leader.getUniqueId());
        clans.put(id, c);
        PlayerData d = RebornCore.get().api().getPlayerData(leader.getUniqueId());
        if (d != null) d.clanId(id);
        Msg.send(leader, "&6가문 창설: " + name);
        return true;
    }

    public Clan ofPlayer(UUID id) {
        for (Clan c : clans.values()) if (c.members.contains(id)) return c;
        return null;
    }

    /** 가문주/장로가 초대를 발행 — 5분 유효. */
    public boolean invite(Player issuer, Player target) {
        Clan c = ofPlayer(issuer.getUniqueId());
        if (c == null) { Msg.error(issuer, "가문에 소속되어 있지 않습니다."); return false; }
        if (!issuer.getUniqueId().equals(c.leader) && !c.elders.contains(issuer.getUniqueId())) {
            Msg.error(issuer, "초대 권한 없음 (가문주/장로만).");
            return false;
        }
        if (c.members.contains(target.getUniqueId())) {
            Msg.error(issuer, target.getName() + "은(는) 이미 가문 소속.");
            return false;
        }
        invites.computeIfAbsent(c.id, k -> ConcurrentHashMap.newKeySet()).add(target.getUniqueId());
        inviteExpiry.put(c.id + ":" + target.getUniqueId(), System.currentTimeMillis() + INVITE_TTL_MS);
        return true;
    }

    /** 초대 확인 — 만료된 항목은 자동 제거. */
    private boolean hasValidInvite(String clanId, UUID player) {
        Set<UUID> set = invites.get(clanId);
        if (set == null || !set.contains(player)) return false;
        Long exp = inviteExpiry.get(clanId + ":" + player);
        if (exp == null || System.currentTimeMillis() > exp) {
            set.remove(player);
            inviteExpiry.remove(clanId + ":" + player);
            return false;
        }
        return true;
    }

    public boolean join(Clan c, Player p) {
        // 초대 검증 — 무단 가입 차단
        if (!hasValidInvite(c.id, p.getUniqueId())) {
            Msg.error(p, "초대가 없거나 만료되었습니다.");
            return false;
        }
        // 기존 가문 자동 탈퇴 (다중 가입 방지)
        Clan prev = ofPlayer(p.getUniqueId());
        if (prev != null) {
            if (prev.id.equals(c.id)) {
                Msg.warn(p, "이미 같은 가문 소속.");
                return false;
            }
            prev.members.remove(p.getUniqueId());
            prev.elders.remove(p.getUniqueId());
        }
        c.members.add(p.getUniqueId());
        // 초대 소비
        Set<UUID> inv = invites.get(c.id);
        if (inv != null) inv.remove(p.getUniqueId());
        inviteExpiry.remove(c.id + ":" + p.getUniqueId());
        var pd = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (pd != null) pd.clanId(c.id);
        return true;
    }

    public boolean leave(Player p) {
        Clan c = ofPlayer(p.getUniqueId());
        if (c == null) return false;
        c.members.remove(p.getUniqueId());
        c.elders.remove(p.getUniqueId());
        if (p.getUniqueId().equals(c.leader)) {
            // 가장 오래 머문 elder에게 양도, 없으면 가문 해체
            if (c.elders.isEmpty()) clans.remove(c.id);
            else c.leader = c.elders.iterator().next();
        }
        var pd = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (pd != null) pd.clanId("");
        return true;
    }

    public void addXp(Clan c, long xp) {
        c.xp += xp;
        List<Integer> table = plugin.getConfig().getIntegerList("clan.level-thresholds");
        while (c.level < table.size() - 1 && c.xp >= table.get(c.level)) c.level++;
    }

    /**
     * HiddenClass CLAN_RANK 조건 체크용 외부 API.
     * 랭크: "MEMBER" / "ELDER" / "LEADER" — 상위 권한 포함.
     */
    public boolean hasRankAtLeast(UUID p, String requiredRank) {
        Clan c = ofPlayer(p);
        if (c == null) return false;
        if (p.equals(c.leader)) return true; // LEADER는 모든 랭크 포함
        if ("LEADER".equalsIgnoreCase(requiredRank)) return false;
        if (c.elders.contains(p)) return true; // ELDER는 ELDER/MEMBER 포함
        if ("ELDER".equalsIgnoreCase(requiredRank)) return false;
        // MEMBER만 요구되면 멤버이기만 하면 OK
        return c.members.contains(p);
    }

    /** HiddenClass CLAN_RANK 조건 — 가문 인원 수. */
    public int clanMemberCount(UUID p) {
        Clan c = ofPlayer(p);
        return c == null ? 0 : c.members.size();
    }
}
