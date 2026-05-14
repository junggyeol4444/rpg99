package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Territory;
import kr.reborn.core.util.Msg;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class TerritoryManager {

    private final RebornClan plugin;
    private final Map<String, Territory> claims = new HashMap<>();

    public TerritoryManager(RebornClan p) { this.plugin = p; }

    public Territory at(Chunk c) {
        return claims.get(c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ());
    }

    public boolean claim(Player p) {
        Chunk c = p.getLocation().getChunk();
        Territory existing = at(c);
        if (existing != null) { Msg.error(p, "이미 점령된 영토."); return false; }
        Territory t = new Territory(c.getWorld().getName(), c.getX(), c.getZ(), p.getUniqueId());
        var clan = plugin.clans().ofPlayer(p.getUniqueId());
        if (clan != null) t.clanId = clan.id;
        claims.put(t.key(), t);
        Msg.send(p, "&a영토 점령: " + c.getX() + "," + c.getZ());
        return true;
    }

    public boolean unclaim(Player p) {
        Chunk c = p.getLocation().getChunk();
        Territory t = at(c);
        if (t == null || !t.owner.equals(p.getUniqueId())) {
            Msg.error(p, "내 영토가 아니다.");
            return false;
        }
        claims.remove(t.key());
        Msg.send(p, "&7영토 해제.");
        return true;
    }
}
