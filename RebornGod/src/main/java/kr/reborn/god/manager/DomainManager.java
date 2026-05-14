package kr.reborn.god.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

public final class DomainManager {

    private final RebornGod plugin;

    public DomainManager(RebornGod p) { this.plugin = p; }

    public boolean create(Player p) {
        God g = plugin.gods().of(p.getUniqueId());
        if (g == null) { Msg.error(p, "신이 아니다."); return false; }
        String name = "domain_" + p.getName().toLowerCase();
        if (Bukkit.getWorld(name) != null) {
            Msg.warn(p, "이미 신역이 있다."); return false;
        }
        World w = new WorldCreator(name).createWorld();
        if (w == null) { Msg.error(p, "월드 생성 실패."); return false; }
        g.domainWorld = name;
        Msg.send(p, "&6신역 생성: " + name);
        return true;
    }

    public void enter(Player p) {
        God g = plugin.gods().of(p.getUniqueId());
        if (g == null || g.domainWorld.isEmpty()) { Msg.error(p, "신역 없음."); return; }
        World w = Bukkit.getWorld(g.domainWorld);
        if (w != null) p.teleport(w.getSpawnLocation());
    }
}
