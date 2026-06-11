package kr.reborn.ship.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.ship.RebornShip;
import kr.reborn.ship.data.Ship;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ShipRegistry {

    private static final String NS = "RebornShip.ship";

    private final RebornShip plugin;
    /** ownerByMap + 전체 리스트 — 이동/생성/해체가 동시에 호출될 수 있어 동시성 보장. */
    private final Map<UUID, List<Ship>> byOwner = new ConcurrentHashMap<>();
    private final List<Ship> all = new CopyOnWriteArrayList<>();

    public ShipRegistry(RebornShip p) {
        this.plugin = p;
        loadAll();
    }

    private void loadAll() {
        try {
            var data = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            for (var e : data.entrySet()) {
                Ship s = decode(e.getKey(), e.getValue());
                if (s == null) continue;
                // 침몰선은 복원하지 않고 KV에서 정리
                if (s.state == Ship.State.SUNK) {
                    try { kr.reborn.core.RebornCore.get().kv().remove(NS, null, e.getKey()); }
                    catch (Throwable ignored) {}
                    continue;
                }
                all.add(s);
                byOwner.computeIfAbsent(s.owner, k -> new CopyOnWriteArrayList<>()).add(s);
            }
        } catch (Throwable ignored) {}
    }

    /** 외부 호출 — 모든 배 영속화. plugin onDisable에서 호출. */
    public void saveAll() {
        for (Ship s : all) persist(s);
    }

    void persist(Ship s) {
        try {
            kr.reborn.core.RebornCore.get().kv().put(NS, null, s.id.toString(), encode(s));
        } catch (Throwable ignored) {}
    }

    /** 인코딩: owner|name|grade|hp|maxHp|world|hx|hy|hz|blockCount|state|rotation|blocks_csv */
    private String encode(Ship s) {
        StringBuilder blocks = new StringBuilder();
        for (var e : s.blocks.entrySet()) {
            if (blocks.length() > 0) blocks.append(';');
            blocks.append(e.getKey()).append('=').append(e.getValue().getAsString());
        }
        return s.owner + "|" + s.name + "|" + s.grade + "|" + s.hp + "|" + s.maxHp + "|"
                + (s.helm.getWorld() == null ? "world" : s.helm.getWorld().getName()) + "|"
                + s.helm.getX() + "|" + s.helm.getY() + "|" + s.helm.getZ() + "|"
                + s.blockCount + "|" + s.state.name() + "|" + s.rotation + "|" + blocks;
    }

    private Ship decode(String idStr, String value) {
        try {
            String[] parts = value.split("\\|", 13);
            if (parts.length < 13) return null;
            UUID shipId;
            try { shipId = UUID.fromString(idStr); }
            catch (Throwable t) { shipId = UUID.randomUUID(); }
            UUID owner = UUID.fromString(parts[0]);
            String name = parts[1];
            int grade = Integer.parseInt(parts[2]);
            double hp = Double.parseDouble(parts[3]);
            double maxHp = Double.parseDouble(parts[4]);
            org.bukkit.World w = org.bukkit.Bukkit.getWorld(parts[5]);
            if (w == null) return null;
            org.bukkit.Location helm = new org.bukkit.Location(w,
                    Double.parseDouble(parts[6]), Double.parseDouble(parts[7]),
                    Double.parseDouble(parts[8]));
            int blockCount = Integer.parseInt(parts[9]);
            // 저장된 id 그대로 복원 — 재시작 시 id 변경으로 KV 키가 매번 바뀌어
            // 배가 중복 적재되던 버그 방지
            Ship s = new Ship(shipId, owner, name, grade, hp, helm, blockCount);
            s.maxHp = maxHp;
            try { s.state = Ship.State.valueOf(parts[10]); } catch (Throwable ignored) {}
            s.rotation = Integer.parseInt(parts[11]);
            // 블록 디코딩 — "x,y,z=blockdata"
            if (!parts[12].isEmpty()) {
                for (String block : parts[12].split(";")) {
                    int eq = block.indexOf('=');
                    if (eq < 0) continue;
                    try {
                        String key = block.substring(0, eq);
                        String bdStr = block.substring(eq + 1);
                        BlockData bd = org.bukkit.Bukkit.createBlockData(bdStr);
                        s.blocks.put(key, bd);
                    } catch (Throwable ignored) {}
                }
            }
            return s;
        } catch (Throwable t) { return null; }
    }

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
        byOwner.computeIfAbsent(owner.getUniqueId(), x -> new CopyOnWriteArrayList<>()).add(s);
        persist(s);
        Msg.send(owner, "&6배 등록: " + name + " (등급 " + grade + ", 블록 " + connected.size() + ")");
        return s;
    }

    public List<Ship> ofOwner(UUID owner) { return byOwner.getOrDefault(owner, List.of()); }
    public List<Ship> all() { return all; }

    /** 배 등록 해제 (해체·침몰 후). */
    public void unregister(Ship s) {
        all.remove(s);
        try { kr.reborn.core.RebornCore.get().kv().remove(NS, null, s.id.toString()); }
        catch (Throwable ignored) {}
        List<Ship> own = byOwner.get(s.owner);
        if (own != null) own.remove(s);
    }

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
