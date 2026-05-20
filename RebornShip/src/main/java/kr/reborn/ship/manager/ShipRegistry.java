package kr.reborn.ship.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.ship.RebornShip;
import kr.reborn.ship.data.Ship;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

public final class ShipRegistry {

    private final RebornShip plugin;
    private final Map<UUID, List<Ship>> byOwner = new HashMap<>();
    private final List<Ship> all = new ArrayList<>();

    public ShipRegistry(RebornShip p) { this.plugin = p; }

    /**
     * 조타석을 바라보는 위치에서 등록.
     * BFS로 연결된 비공기 블록을 모두 스냅샷에 담는다.
     */
    public Ship register(Player owner, String name) {
        var helm = owner.getTargetBlockExact(5);
        if (helm == null) { Msg.error(owner, "조타석을 바라봐라."); return null; }

        Set<Block> connected = collectConnected(helm);
        if (connected.isEmpty()) { Msg.error(owner, "배 블록이 인식되지 않음."); return null; }

        // 물 위에 있는지 확인
        if (!isOnWater(connected)) {
            Msg.error(owner, "배는 물 위에 있어야 한다.");
            return null;
        }

        int grade = resolveGrade(connected.size());
        double hp = plugin.getConfig().getDouble("grades." + (grade - 1) + ".hp", 100);
        Ship s = new Ship(owner.getUniqueId(), name, grade, hp, helm.getLocation(), connected.size());
        for (Block b : connected) {
            s.blocks.put(Ship.key(b.getX(), b.getY(), b.getZ()), b.getBlockData());
        }
        all.add(s);
        byOwner.computeIfAbsent(owner.getUniqueId(), x -> new ArrayList<>()).add(s);
        Msg.send(owner, "&6배 등록: " + name + " (등급 " + grade + ", 블록 " + connected.size() + ")");
        return s;
    }

    public List<Ship> ofOwner(UUID owner) { return byOwner.getOrDefault(owner, List.of()); }
    public List<Ship> all() { return all; }

    public Ship byHelm(Block helm) {
        for (Ship s : all) {
            if (s.helm.getBlockX() == helm.getX()
                    && s.helm.getBlockY() == helm.getY()
                    && s.helm.getBlockZ() == helm.getZ()) return s;
        }
        return null;
    }

    /** 배에 속한 블록인지 빠른 lookup. */
    public boolean isShipBlock(Ship s, int x, int y, int z) {
        return s.blocks.containsKey(Ship.key(x, y, z));
    }

    /** BFS로 인접 블록 스캔 (최대 5000). */
    private Set<Block> collectConnected(Block start) {
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        q.push(start);
        seen.add(start);
        BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN,
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        while (!q.isEmpty() && seen.size() < 5000) {
            Block b = q.pop();
            for (BlockFace f : faces) {
                Block n = b.getRelative(f);
                if (seen.contains(n)) continue;
                Material m = n.getType();
                if (m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR) continue;
                if (m == Material.WATER || m == Material.LAVA) continue;
                seen.add(n);
                q.push(n);
            }
        }
        return seen;
    }

    /** 배 바닥이 한 칸이라도 물 위인지. */
    private boolean isOnWater(Set<Block> blocks) {
        for (Block b : blocks) {
            Block below = b.getRelative(BlockFace.DOWN);
            if (below.getType() == Material.WATER) return true;
        }
        return false;
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
