package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Clan;
import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.util.Msg;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClanManager {

    private final RebornClan plugin;
    private final Map<String, Clan> clans = new HashMap<>();

    public ClanManager(RebornClan p) { this.plugin = p; }

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
        d.clanId(id);
        Msg.send(leader, "&6가문 창설: " + name);
        return true;
    }

    public Clan ofPlayer(UUID id) {
        for (Clan c : clans.values()) if (c.members.contains(id)) return c;
        return null;
    }

    public boolean join(Clan c, Player p) {
        c.members.add(p.getUniqueId());
        RebornCore.get().api().getPlayerData(p.getUniqueId()).clanId(c.id);
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
        RebornCore.get().api().getPlayerData(p.getUniqueId()).clanId("");
        return true;
    }

    public void addXp(Clan c, long xp) {
        c.xp += xp;
        List<Integer> table = plugin.getConfig().getIntegerList("clan.level-thresholds");
        while (c.level < table.size() - 1 && c.xp >= table.get(c.level)) c.level++;
    }
}
