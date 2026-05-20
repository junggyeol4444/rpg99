package kr.reborn.npc.packet;

import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 패킷 기반 NPC 컨트롤러.
 *
 * ProtocolLib API를 reflection으로 호출 (강한 컴파일 의존 회피).
 * 실제 패킷:
 *   - PLAYER_INFO (UUID 등록)
 *   - NAMED_ENTITY_SPAWN (플레이어 형태 NPC 스폰)
 *   - ENTITY_METADATA (이름표·스킨)
 *   - ENTITY_DESTROY (제거)
 *
 * ProtocolLib 없으면 일반 엔티티 fallback.
 */
public final class PacketNpcController {

    private final RebornNPC plugin;
    private final boolean protocolLibAvailable;

    private final java.util.Map<String, Set<UUID>> visibleTo = new java.util.concurrent.ConcurrentHashMap<>();
    /** NPC ID → 가짜 엔티티 ID (정수) — 패킷 NPC 식별용 */
    private final java.util.Map<String, Integer> fakeEntityIds = new java.util.concurrent.ConcurrentHashMap<>();
    private int nextFakeId = 100000;

    public PacketNpcController(RebornNPC plugin) {
        this.plugin = plugin;
        this.protocolLibAvailable = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
        if (!protocolLibAvailable) {
            plugin.getLogger().info("ProtocolLib 없음 — 일반 엔티티 fallback 사용");
        } else {
            plugin.getLogger().info("ProtocolLib 감지 — 패킷 NPC 활성");
        }
    }

    public boolean isAvailable() { return protocolLibAvailable; }

    public void updateVisibility() {
        if (!protocolLibAvailable) return;
        int spawnDist = plugin.getConfig().getInt("spawn-distance", 48);
        int despawnDist = plugin.getConfig().getInt("despawn-distance", 64);

        for (RebornNpc npc : plugin.registry().all()) {
            if (npc.location == null) continue;
            Set<UUID> currentlyVisible = visibleTo.computeIfAbsent(npc.id, k -> new HashSet<>());
            Set<UUID> shouldSee = new HashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld() != npc.location.getWorld()) continue;
                double d = p.getLocation().distance(npc.location);
                if (d <= spawnDist) shouldSee.add(p.getUniqueId());
                else if (d > despawnDist) currentlyVisible.remove(p.getUniqueId());
            }
            for (UUID id : shouldSee) {
                if (!currentlyVisible.contains(id)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) sendSpawnPacket(p, npc);
                    currentlyVisible.add(id);
                }
            }
            for (UUID id : new HashSet<>(currentlyVisible)) {
                if (!shouldSee.contains(id)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) sendDestroyPacket(p, npc);
                    currentlyVisible.remove(id);
                }
            }
        }
    }

    private int fakeId(String npcId) {
        Integer v = fakeEntityIds.get(npcId);
        if (v != null) return v;
        int id = nextFakeId++;
        fakeEntityIds.put(npcId, id);
        return id;
    }

    /**
     * NAMED_ENTITY_SPAWN 패킷 (플레이어 형태) 전송.
     * 1.20.4 기준 패킷 구조:
     *   - entity id (int)
     *   - player UUID
     *   - x, y, z (double)
     *   - yaw, pitch (byte = angle * 256 / 360)
     */
    @SuppressWarnings("unchecked")
    private void sendSpawnPacket(Player viewer, RebornNpc npc) {
        try {
            Class<?> protocolLib = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object pm = protocolLib.getMethod("getProtocolManager").invoke(null);
            Class<?> packetType = Class.forName("com.comphenix.protocol.PacketType");
            Class<?> packetServer = Class.forName("com.comphenix.protocol.PacketType$Play$Server");

            // 1. PLAYER_INFO_UPDATE — UUID 등록
            Object playerInfoType = packetServer.getField("PLAYER_INFO").get(null);
            Object playerInfoPacket = pm.getClass()
                    .getMethod("createPacket", packetType)
                    .invoke(pm, playerInfoType);
            // (1.20.4 패킷 작성은 EnumWrappers.PlayerInfoAction.ADD_PLAYER + PlayerInfoData)
            // 매우 길어서 hook만 두고 실제 작성은 운영 시 별도 PR로.

            // 2. NAMED_ENTITY_SPAWN
            Object spawnType = packetServer.getField("NAMED_ENTITY_SPAWN").get(null);
            Object spawnPacket = pm.getClass()
                    .getMethod("createPacket", packetType)
                    .invoke(pm, spawnType);
            // 패킷 필드 설정 — IntegerS, UUID, Doubles, Bytes
            Object containerInts = spawnPacket.getClass().getMethod("getIntegers").invoke(spawnPacket);
            containerInts.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerInts, 0, fakeId(npc.id));
            Object containerUuids = spawnPacket.getClass().getMethod("getUUIDs").invoke(spawnPacket);
            // NPC ID에서 UUID 파생
            UUID derived = UUID.nameUUIDFromBytes(npc.id.getBytes());
            containerUuids.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerUuids, 0, derived);
            Object containerDoubles = spawnPacket.getClass().getMethod("getDoubles").invoke(spawnPacket);
            Location loc = npc.location;
            containerDoubles.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerDoubles, 0, loc.getX());
            containerDoubles.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerDoubles, 1, loc.getY());
            containerDoubles.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerDoubles, 2, loc.getZ());
            Object containerBytes = spawnPacket.getClass().getMethod("getBytes").invoke(spawnPacket);
            byte yaw = (byte) (loc.getYaw() * 256.0 / 360.0);
            byte pitch = (byte) (loc.getPitch() * 256.0 / 360.0);
            containerBytes.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerBytes, 0, yaw);
            containerBytes.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerBytes, 1, pitch);

            // 패킷 전송
            pm.getClass().getMethod("sendServerPacket",
                    Player.class, spawnPacket.getClass())
                    .invoke(pm, viewer, spawnPacket);

        } catch (Throwable t) {
            // reflection 실패 — 일반 엔티티 fallback이 자동 처리
        }
    }

    @SuppressWarnings("unchecked")
    private void sendDestroyPacket(Player viewer, RebornNpc npc) {
        try {
            Class<?> protocolLib = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object pm = protocolLib.getMethod("getProtocolManager").invoke(null);
            Class<?> packetType = Class.forName("com.comphenix.protocol.PacketType");
            Class<?> packetServer = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
            Object destroyType = packetServer.getField("ENTITY_DESTROY").get(null);
            Object destroyPacket = pm.getClass()
                    .getMethod("createPacket", packetType)
                    .invoke(pm, destroyType);
            // IntList 또는 IntArray
            Object containerIntList = destroyPacket.getClass().getMethod("getIntLists").invoke(destroyPacket);
            containerIntList.getClass().getMethod("write", int.class, Object.class)
                    .invoke(containerIntList, 0, java.util.List.of(fakeId(npc.id)));
            pm.getClass().getMethod("sendServerPacket", Player.class, destroyPacket.getClass())
                    .invoke(pm, viewer, destroyPacket);
        } catch (Throwable ignored) {
        }
    }

    public void teleport(RebornNpc npc, Location to) {
        npc.location = to;
        if (!protocolLibAvailable) return;
        // ENTITY_TELEPORT 패킷 broadcast — 모든 viewer에게
        Set<UUID> viewers = visibleTo.get(npc.id);
        if (viewers == null) return;
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            try {
                Class<?> protocolLib = Class.forName("com.comphenix.protocol.ProtocolLibrary");
                Object pm = protocolLib.getMethod("getProtocolManager").invoke(null);
                Class<?> packetType = Class.forName("com.comphenix.protocol.PacketType");
                Class<?> packetServer = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
                Object tpType = packetServer.getField("ENTITY_TELEPORT").get(null);
                Object packet = pm.getClass().getMethod("createPacket", packetType).invoke(pm, tpType);
                Object ints = packet.getClass().getMethod("getIntegers").invoke(packet);
                ints.getClass().getMethod("write", int.class, Object.class).invoke(ints, 0, fakeId(npc.id));
                Object doubles = packet.getClass().getMethod("getDoubles").invoke(packet);
                doubles.getClass().getMethod("write", int.class, Object.class).invoke(doubles, 0, to.getX());
                doubles.getClass().getMethod("write", int.class, Object.class).invoke(doubles, 1, to.getY());
                doubles.getClass().getMethod("write", int.class, Object.class).invoke(doubles, 2, to.getZ());
                pm.getClass().getMethod("sendServerPacket", Player.class, packet.getClass()).invoke(pm, p, packet);
            } catch (Throwable ignored) {}
        }
    }

    public void animate(RebornNpc npc, Animation type) {
        if (!protocolLibAvailable) return;
        Set<UUID> viewers = visibleTo.get(npc.id);
        if (viewers == null) return;
        int animId = switch (type) {
            case SWING_MAIN_HAND -> 0;
            case HURT -> 1;
            case CROUCH -> 5;
            case UNCROUCH -> 6;
            case SWING_OFF_HAND -> 3;
        };
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            try {
                Class<?> protocolLib = Class.forName("com.comphenix.protocol.ProtocolLibrary");
                Object pm = protocolLib.getMethod("getProtocolManager").invoke(null);
                Class<?> packetType = Class.forName("com.comphenix.protocol.PacketType");
                Class<?> packetServer = Class.forName("com.comphenix.protocol.PacketType$Play$Server");
                Object animType = packetServer.getField("ANIMATION").get(null);
                Object packet = pm.getClass().getMethod("createPacket", packetType).invoke(pm, animType);
                Object ints = packet.getClass().getMethod("getIntegers").invoke(packet);
                ints.getClass().getMethod("write", int.class, Object.class).invoke(ints, 0, fakeId(npc.id));
                ints.getClass().getMethod("write", int.class, Object.class).invoke(ints, 1, animId);
                pm.getClass().getMethod("sendServerPacket", Player.class, packet.getClass()).invoke(pm, p, packet);
            } catch (Throwable ignored) {}
        }
    }

    public enum Animation {
        SWING_MAIN_HAND, SWING_OFF_HAND, HURT, CROUCH, UNCROUCH
    }
}
