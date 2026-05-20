package kr.reborn.spawn.join;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.spawn.RebornSpawn;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class FirstJoinListener implements Listener {

    private final RebornSpawn plugin;

    public FirstJoinListener(RebornSpawn p) { this.plugin = p; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        boolean fresh = d.firstJoin() == 0 || d.worldKey() == WorldKey.LOBBY && d.tier().isEmpty();
        if (!fresh) return;

        // 환생의 월드 텔레포트
        var c = plugin.getConfig().getConfigurationSection("lobby");
        World lobby = Bukkit.getWorld(c.getString("world", "lobby"));
        if (lobby != null) {
            Location loc = new Location(lobby,
                    c.getDouble("spawn.x"), c.getDouble("spawn.y"), c.getDouble("spawn.z"));
            p.teleport(loc);
        }
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.sendTitle("§6환생의 월드", "§f환영한다, 새로운 영혼이여", 10, 80, 30);

        // 초기 스탯 1로 (이미 0이면)
        for (StatType t : StatType.COMMON_8) {
            if (d.getStat(t) <= 0) d.setStat(t, 1);
        }
        d.firstJoin(System.currentTimeMillis());
    }
}
