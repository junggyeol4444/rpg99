package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Marriage;
import kr.reborn.core.util.Msg;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MarriageManager {

    private final RebornClan plugin;
    private final Map<UUID, Marriage> marriages = new HashMap<>();
    private final Map<UUID, UUID> proposals = new HashMap<>();

    public MarriageManager(RebornClan p) { this.plugin = p; }

    public Marriage of(UUID id) { return marriages.get(id); }

    public void propose(Player a, Player b) {
        if (marriages.containsKey(a.getUniqueId()) || marriages.containsKey(b.getUniqueId())) {
            Msg.error(a, "이미 결혼한 사람이 있다.");
            return;
        }
        proposals.put(b.getUniqueId(), a.getUniqueId());
        Msg.send(b, "&d" + a.getName() + "이(가) 청혼했다. /marry accept");
    }

    public void accept(Player b) {
        UUID aId = proposals.remove(b.getUniqueId());
        if (aId == null) { Msg.warn(b, "청혼이 없다."); return; }
        Marriage m = new Marriage(aId, b.getUniqueId(), "", System.currentTimeMillis());
        marriages.put(aId, m);
        marriages.put(b.getUniqueId(), m);
        Msg.send(b, "&6결혼 성립.");
    }

    public void marryNpc(Player p, String npcId) {
        Marriage m = new Marriage(p.getUniqueId(), UUID.randomUUID(), npcId, System.currentTimeMillis());
        marriages.put(p.getUniqueId(), m);
        Msg.send(p, "&6NPC와 결혼: " + npcId);
    }

    public void divorce(Player p) {
        Marriage m = marriages.remove(p.getUniqueId());
        if (m == null) { Msg.warn(p, "결혼하지 않았다."); return; }
        marriages.remove(m.a);
        marriages.remove(m.b);
        Msg.send(p, "&7이혼이 성립되었다.");
    }
}
