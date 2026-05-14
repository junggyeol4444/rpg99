package kr.reborn.ship.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.ship.RebornShip;
import kr.reborn.ship.data.Ship;
import org.bukkit.Block;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

public final class ShipRegistry {

    private final RebornShip plugin;
    private final Map<UUID, List<Ship>> byOwner = new HashMap<>();
    private final List<Ship> all = new ArrayList<>();

    public ShipRegistry(RebornShip p) { this.plugin = p; }

    public Ship register(Player owner, String name) {
        var helm = owner.getTargetBlockExact(5);
        if (helm == null) { Msg.error(owner, "조타석을 바라봐라."); return null; }
        int blocks = countConnected(helm);
        if (blocks <= 0) { Msg.error(owner, "배 블록이 인식되지 않음."); return null; }
        int grade = resolveGrade(blocks);
        double hp = plugin.getConfig().getDouble("grades." + (grade - 1) + ".hp", 100);
        Ship s = new Ship(owner.getUniqueId(), name, grade, hp, helm.getLocation(), blocks);
        all.add(s);
        byOwner.computeIfAbsent(owner.getUniqueId(), x -> new ArrayList<>()).add(s);
        Msg.send(owner, "&6배 등록: " + name + " (등급 " + grade + ", 블록 " + blocks + ")");
        return s;
    }

    public List<Ship> ofOwner(UUID owner) {
        return byOwner.getOrDefault(owner, List.of());
    }

    public List<Ship> all() { return all; }

    /** 매우 단순한 BFS: 인접 비공기 블록을 최대 5000까지 스캔. */
    private int countConnected(org.bukkit.block.Block start) {
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        q.push(start);
        seen.add(start);
        while (!q.isEmpty() && seen.size() < 5000) {
            var b = q.pop();
            for (BlockFace f : new BlockFace[]{BlockFace.UP, BlockFace.DOWN,
                    BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                var n = b.getRelative(f);
                if (seen.contains(n)) continue;
                if (n.getType() == Material.AIR || n.getType() == Material.WATER) continue;
                seen.add(n);
                q.push(n);
            }
        }
        return seen.size();
    }

    private int resolveGrade(int blocks) {
        var arr = plugin.getConfig().getMapList("grades");
        for (var e : arr) {
            int min = ((Number) e.get("min-blocks")).intValue();
            int max = ((Number) e.get("max-blocks")).intValue();
            if (blocks >= min && blocks <= max) return ((Number) e.get("id")).intValue();
        }
        return 1;
    }
}
