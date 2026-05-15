package kr.reborn.ship.manager;

import kr.reborn.ship.RebornShip;
import kr.reborn.ship.data.Ship;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 배 블록 구조물의 이동·회전·충돌을 처리한다.
 *
 * 핵심 알고리즘:
 *   1) 새 좌표 계산 (offset 또는 회전 행렬)
 *   2) 충돌 체크 (다른 배 블록·고체 블록·좌초 수심)
 *   3) 원위치를 WATER로 비우고, 새 위치에 배 블록 배치
 *   4) 탑승 엔티티(승객)도 같은 offset만큼 텔레포트
 */
public final class ShipMovement {

    private final RebornShip plugin;

    public ShipMovement(RebornShip p) { this.plugin = p; }

    /** dx,dz만큼 배를 평행 이동. 성공 시 true. */
    public boolean translate(Ship ship, int dx, int dy, int dz) {
        World w = ship.helm.getWorld();
        if (w == null) return false;

        // 새 좌표 계산
        Map<String, BlockData> newPositions = new LinkedHashMap<>();
        Map<String, int[]> coordMap = new HashMap<>();
        for (var e : ship.blocks.entrySet()) {
            int[] xyz = parseKey(e.getKey());
            int nx = xyz[0] + dx, ny = xyz[1] + dy, nz = xyz[2] + dz;
            String nk = Ship.key(nx, ny, nz);
            newPositions.put(nk, e.getValue());
            coordMap.put(nk, new int[]{nx, ny, nz});
        }

        // 충돌 체크: 새 좌표에 자기 배가 아닌 고체 블록이 있으면 안 됨
        for (var ne : coordMap.values()) {
            // 원래 자기 자리는 OK (제자리 이동이 없으므로 set 차집합)
            String key = Ship.key(ne[0], ne[1], ne[2]);
            if (ship.blocks.containsKey(key)) continue;
            Block b = w.getBlockAt(ne[0], ne[1], ne[2]);
            Material m = b.getType();
            if (m != Material.AIR && m != Material.CAVE_AIR && m != Material.WATER) {
                return false;
            }
        }

        // 좌초 체크 (config: combat.min-water-depth)
        int minDepth = plugin.getConfig().getInt("combat.min-water-depth", 3);
        // 배 가장 낮은 블록 아래에 minDepth 만큼 물이 있는지 (대표 1점 체크)
        int[] firstNew = coordMap.values().iterator().next();
        if (!hasWaterDepth(w, firstNew[0], firstNew[1] - 1, firstNew[2], minDepth)) {
            return false;
        }

        // 탑승 엔티티 수집 (배 위에 서 있는 엔티티)
        var passengers = collectPassengers(ship);

        // 1단계: 원래 위치 비우기 (위쪽부터 비워서 중력 sand 같은 걸 회피)
        for (String key : ship.blocks.keySet()) {
            int[] xyz = parseKey(key);
            // 원래 자리가 새 위치에 포함되지 않으면 WATER로
            if (!newPositions.containsKey(key)) {
                w.getBlockAt(xyz[0], xyz[1], xyz[2]).setType(Material.WATER, false);
            }
        }
        // 2단계: 새 위치에 배치
        for (var e : newPositions.entrySet()) {
            int[] xyz = parseKey(e.getKey());
            Block target = w.getBlockAt(xyz[0], xyz[1], xyz[2]);
            target.setBlockData(e.getValue(), false);
        }

        // 배 데이터 갱신
        ship.blocks.clear();
        ship.blocks.putAll(newPositions);
        ship.helm = ship.helm.clone().add(dx, dy, dz);

        // 탑승자 텔레포트
        for (Entity ent : passengers) {
            ent.teleport(ent.getLocation().add(dx, dy, dz));
        }
        return true;
    }

    /** 배를 90/180/270도 회전. helm을 축으로 한다. */
    public boolean rotate(Ship ship, int degrees) {
        if (degrees % 90 != 0) return false;
        World w = ship.helm.getWorld();
        if (w == null) return false;
        int hx = ship.helm.getBlockX(), hy = ship.helm.getBlockY(), hz = ship.helm.getBlockZ();
        int times = ((degrees / 90) % 4 + 4) % 4;

        Map<String, BlockData> newPositions = new LinkedHashMap<>();
        Map<String, int[]> coordMap = new HashMap<>();
        for (var e : ship.blocks.entrySet()) {
            int[] xyz = parseKey(e.getKey());
            int dx = xyz[0] - hx, dz = xyz[2] - hz;
            int nx = dx, nz = dz;
            for (int i = 0; i < times; i++) {
                int t = nx; nx = -nz; nz = t;  // 90도 회전 행렬
            }
            int absX = hx + nx, absY = xyz[1], absZ = hz + nz;
            String nk = Ship.key(absX, absY, absZ);
            newPositions.put(nk, e.getValue());
            coordMap.put(nk, new int[]{absX, absY, absZ});
        }

        for (var pos : coordMap.values()) {
            String k = Ship.key(pos[0], pos[1], pos[2]);
            if (ship.blocks.containsKey(k)) continue;
            Material m = w.getBlockAt(pos[0], pos[1], pos[2]).getType();
            if (m != Material.AIR && m != Material.CAVE_AIR && m != Material.WATER) return false;
        }

        for (String key : ship.blocks.keySet()) {
            if (!newPositions.containsKey(key)) {
                int[] xyz = parseKey(key);
                w.getBlockAt(xyz[0], xyz[1], xyz[2]).setType(Material.WATER, false);
            }
        }
        for (var e : newPositions.entrySet()) {
            int[] xyz = parseKey(e.getKey());
            w.getBlockAt(xyz[0], xyz[1], xyz[2]).setBlockData(e.getValue(), false);
        }
        ship.blocks.clear();
        ship.blocks.putAll(newPositions);
        ship.rotation = (ship.rotation + degrees) % 360;
        return true;
    }

    /** 배 침몰: 블록을 5초에 걸쳐 한 칸씩 아래로 내림. */
    public void sink(Ship ship) {
        ship.state = Ship.State.SUNK;
        // 단순화: 5초 후 일괄 제거
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World w = ship.helm.getWorld();
            if (w == null) return;
            for (String key : ship.blocks.keySet()) {
                int[] xyz = parseKey(key);
                w.getBlockAt(xyz[0], xyz[1], xyz[2]).setType(Material.WATER, false);
            }
            ship.blocks.clear();
        }, 100L);
    }

    private java.util.List<Entity> collectPassengers(Ship ship) {
        java.util.List<Entity> out = new java.util.ArrayList<>();
        World w = ship.helm.getWorld();
        if (w == null) return out;
        // 배 영역 bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (String key : ship.blocks.keySet()) {
            int[] xyz = parseKey(key);
            if (xyz[0] < minX) minX = xyz[0]; if (xyz[0] > maxX) maxX = xyz[0];
            if (xyz[1] < minY) minY = xyz[1]; if (xyz[1] > maxY) maxY = xyz[1];
            if (xyz[2] < minZ) minZ = xyz[2]; if (xyz[2] > maxZ) maxZ = xyz[2];
        }
        // 배 위 1~3칸까지 탑승객으로 간주
        for (Entity e : w.getNearbyEntities(
                ship.helm.toCenterLocation(),
                Math.max(1, (maxX - minX) / 2.0 + 2),
                Math.max(1, (maxY - minY) / 2.0 + 4),
                Math.max(1, (maxZ - minZ) / 2.0 + 2))) {
            int ex = e.getLocation().getBlockX(), ey = e.getLocation().getBlockY(), ez = e.getLocation().getBlockZ();
            if (ex >= minX - 1 && ex <= maxX + 1
                    && ey >= minY && ey <= maxY + 3
                    && ez >= minZ - 1 && ez <= maxZ + 1) out.add(e);
        }
        return out;
    }

    private boolean hasWaterDepth(World w, int x, int y, int z, int minDepth) {
        for (int i = 0; i < minDepth; i++) {
            if (w.getBlockAt(x, y - i, z).getType() != Material.WATER) return false;
        }
        return true;
    }

    private static int[] parseKey(String k) {
        String[] p = k.split(",");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }
}
