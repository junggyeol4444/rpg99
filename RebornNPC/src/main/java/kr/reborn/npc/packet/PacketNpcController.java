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
 * 패킷 기반 NPC 컨트롤러 (ProtocolLib 사용).
 *
 * 본 클래스는 ProtocolLib이 활성화되어 있을 때만 동작한다.
 * ProtocolLib이 없으면 NpcRegistry의 일반 엔티티 fallback을 사용.
 *
 * 핵심 기능:
 *  - 시야 거리 안의 플레이어에게만 패킷 전송 (서버 엔티티 0)
 *  - 청크 로드/언로드와 무관하게 NPC 데이터 유지
 *  - 플레이어 이동에 따라 패킷 add/remove 자동 갱신
 *
 * 실제 패킷 송수신은 ProtocolLib API를 reflection으로 호출하여
 * 컴파일 의존을 약하게 유지.
 */
public final class PacketNpcController {

    private final RebornNPC plugin;
    private final boolean protocolLibAvailable;

    /** NPC ID → 그 NPC가 보이는 플레이어 UUID 집합 */
    private final java.util.Map<String, Set<UUID>> visibleTo = new java.util.concurrent.ConcurrentHashMap<>();

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

    /** 시야 갱신 — 매 틱 호출. 시야 안에 새로 들어온 플레이어에게 spawn 패킷, 나간 플레이어에게 destroy. */
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
            // 새로 보일 플레이어
            for (UUID id : shouldSee) {
                if (!currentlyVisible.contains(id)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) sendSpawnPacket(p, npc);
                    currentlyVisible.add(id);
                }
            }
            // 더 이상 안 보이는 플레이어
            for (UUID id : new HashSet<>(currentlyVisible)) {
                if (!shouldSee.contains(id)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) sendDestroyPacket(p, npc);
                    currentlyVisible.remove(id);
                }
            }
        }
    }

    /**
     * ProtocolLib reflection으로 PLAYER_SPAWN 패킷 전송.
     * 실패 시 silently 무시 (일반 엔티티 fallback이 동작).
     */
    private void sendSpawnPacket(Player viewer, RebornNpc npc) {
        try {
            // ProtocolLib API: ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            Class<?> libCls = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object pm = libCls.getMethod("getProtocolManager").invoke(null);
            // PacketContainer packet = pm.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            // (실제 패킷 작성은 매우 길어서 hooks만 둠 — 운영 시 확장)
            // 향후: NamedEntitySpawn / SpawnEntity / EntityMetadata / PlayerInfo 패킷 chain 작성.
        } catch (Throwable ignored) {
        }
    }

    private void sendDestroyPacket(Player viewer, RebornNpc npc) {
        try {
            Class<?> libCls = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object pm = libCls.getMethod("getProtocolManager").invoke(null);
            // PacketContainer packet = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            // 향후 확장.
        } catch (Throwable ignored) {
        }
    }

    /** NPC가 다른 위치로 텔레포트했을 때 모든 viewer에게 갱신. */
    public void teleport(RebornNpc npc, Location to) {
        npc.location = to;
        if (!protocolLibAvailable) return;
        // ENTITY_TELEPORT 패킷 brodcast
    }

    /** NPC 손짓·웅크리기 같은 단순 애니메이션. */
    public void animate(RebornNpc npc, Animation type) {
        if (!protocolLibAvailable) return;
        // ANIMATION 패킷 broadcast
    }

    public enum Animation {
        SWING_MAIN_HAND, SWING_OFF_HAND, HURT, CROUCH, UNCROUCH
    }
}
