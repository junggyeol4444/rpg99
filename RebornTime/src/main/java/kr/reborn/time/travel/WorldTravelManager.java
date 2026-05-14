package kr.reborn.time.travel;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornWorldChangeEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public final class WorldTravelManager {

    private final RebornTime plugin;

    public WorldTravelManager(RebornTime p) { this.plugin = p; }

    public boolean travel(Player p, WorldKey to) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        WorldKey from = d.worldKey();
        if (!canTravel(d, from, to)) {
            Msg.error(p, "이동 불가 — 같은 연결권/포탈/절대자 스킬이 필요하다.");
            return false;
        }
        World w = Bukkit.getWorld(to.name().toLowerCase());
        if (w == null) { Msg.error(p, "월드 없음: " + to); return false; }
        // 페이드 아웃 (블라인드)
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 1));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.teleport(w.getSpawnLocation());
            d.worldKey(to);
            d.visited().add(to);
            p.sendTitle("§6" + to + " §f도착", "§7" + System.currentTimeMillis(), 5, 40, 10);
            Bukkit.getPluginManager().callEvent(new RebornWorldChangeEvent(p, from, to));
        }, 40L);
        return true;
    }

    private boolean canTravel(PlayerData d, WorldKey from, WorldKey to) {
        if (from == to) return false;
        Set<WorldKey> g1 = group("group1"), g2 = group("group2");
        if (g1.contains(from) && g1.contains(to)) return true;
        if (g2.contains(from) && g2.contains(to)) return true;
        // 절대자 이상이면 자유 이동
        double total = RebornCore.get().api().getTotalStats(d.uuid());
        return total >= 5000;
    }

    private Set<WorldKey> group(String key) {
        Set<WorldKey> s = new HashSet<>();
        for (String n : plugin.getConfig().getStringList("realm-groups." + key)) {
            try { s.add(WorldKey.valueOf(n)); } catch (Exception ignored) {}
        }
        return s;
    }
}
