package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class KingdomManager {

    private final RebornClan plugin;
    private final Map<String, Kingdom> kingdoms = new HashMap<>();

    public KingdomManager(RebornClan p) { this.plugin = p; }

    public boolean create(Player king, String id, String name) {
        var clan = plugin.clans().ofPlayer(king.getUniqueId());
        if (clan == null) { Msg.error(king, "가문이 없다."); return false; }
        int reqLv = plugin.getConfig().getInt("kingdom.required-clan-level", 7);
        if (clan.level < reqLv) { Msg.error(king, "가문 Lv " + reqLv + " 이상 필요."); return false; }
        Kingdom k = new Kingdom(id, name, king.getUniqueId());
        k.clans.add(clan.id);
        clan.kingdomId = id;
        kingdoms.put(id, k);
        Msg.send(king, "&6왕국 건설: " + name);
        return true;
    }

    public Kingdom get(String id) { return kingdoms.get(id); }

    public static final class Kingdom {
        public final String id;
        public String name;
        public final java.util.UUID king;
        public final Set<String> clans = new HashSet<>();
        public Kingdom(String id, String name, java.util.UUID king) {
            this.id = id; this.name = name; this.king = king;
        }
    }
}
