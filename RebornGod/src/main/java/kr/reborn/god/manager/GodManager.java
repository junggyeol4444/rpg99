package kr.reborn.god.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GodManager {

    private final RebornGod plugin;
    private final Map<UUID, God> playerGods = new HashMap<>();
    private final Map<String, God> npcGods = new HashMap<>();

    public GodManager(RebornGod p) {
        this.plugin = p;
        loadNpcGods();
    }

    private void loadNpcGods() {
        var sec = plugin.getConfig().getConfigurationSection("npc-gods");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            var s = sec.getConfigurationSection(key);
            God g = new God(null, key, s.getString("name"), s.getDouble("divinity"));
            g.sealed = s.getBoolean("sealed", false);
            npcGods.put(key, g);
        }
    }

    public God of(UUID player) { return playerGods.get(player); }

    public boolean ascend(Player p) {
        double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
        if (total < 5000) {
            Msg.error(p, "절대자(총합 5000+)만 신이 될 수 있다.");
            return false;
        }
        if (playerGods.containsKey(p.getUniqueId())) {
            Msg.warn(p, "이미 신이다."); return false;
        }
        God g = new God(p.getUniqueId(), "", p.getName() + " (신)", 1);
        playerGods.put(p.getUniqueId(), g);
        RebornCore.get().api().setStat(p.getUniqueId(), StatType.DIVINITY, 1);
        Bukkit.broadcastMessage("§6[신격] §f" + p.getName() + "이(가) 신의 자리에 올랐다!");
        return true;
    }

    public void addDivinity(Player p, double delta) {
        God g = playerGods.get(p.getUniqueId());
        if (g == null) return;
        g.divinity = Math.max(0, g.divinity + delta);
        RebornCore.get().api().setStat(p.getUniqueId(), StatType.DIVINITY, g.divinity);
    }

    public java.util.Collection<God> npcAll() { return npcGods.values(); }
}
