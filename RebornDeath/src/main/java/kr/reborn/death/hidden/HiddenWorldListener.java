package kr.reborn.death.hidden;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 명계·시간계·꿈·허공·신계 진입·체류·이탈 처리.
 *
 * 각 히든 월드는 고유한 규칙이 있다:
 *  - 명계: 시간 다름, 명기 성장 가능
 *  - 시간계: 매 5분 틱 속도 0.1~20배 랜덤 변동, 이동속도 비례
 *  - 꿈: 상상력=전투력 (MENTAL 보정), 정신력 깎이면 깸
 *  - 허공: 방향 감각 상실, 무작위 텔레포트 위험
 *  - 신계: 신만 가능 (DIVINITY > 0)
 */
public final class HiddenWorldListener implements Listener {

    private final RebornDeath plugin;
    private final Set<UUID> inTimeRealm = new HashSet<>();
    private final Set<UUID> inDream = new HashSet<>();
    private final Set<UUID> inVoid = new HashSet<>();
    private final Set<UUID> inGodRealm = new HashSet<>();

    public HiddenWorldListener(RebornDeath p) {
        this.plugin = p;
        // 1초마다 체류자 효과 적용
        RebornCore.get().scheduler().runTimer(this::tickResidents, 20L, 20L);
        // 5분마다 시간계 틱 속도 변동
        RebornCore.get().scheduler().runTimer(this::scrambleTimeRealm, 6000L, 6000L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() != null && e.getFrom().getWorld() != e.getTo().getWorld()) {
            handleTransition(e.getPlayer(), e.getFrom().getWorld(), e.getTo().getWorld());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getWorld() != e.getTo().getWorld()) {
            handleTransition(e.getPlayer(), e.getFrom().getWorld(), e.getTo().getWorld());
        }
    }

    private void handleTransition(Player p, World from, World to) {
        WorldType wFrom = classifyWorld(from);
        WorldType wTo = classifyWorld(to);
        if (wFrom == wTo) return;
        if (wFrom != WorldType.NORMAL) onExit(p, wFrom);
        if (wTo != WorldType.NORMAL) onEnter(p, wTo);
    }

    private WorldType classifyWorld(World w) {
        if (w == null) return WorldType.NORMAL;
        String n = w.getName().toLowerCase();
        if (n.contains("time_realm")) return WorldType.TIME_REALM;
        if (n.contains("dream")) return WorldType.DREAM;
        if (n.contains("void")) return WorldType.VOID;
        if (n.contains("god_realm") || n.startsWith("domain_")) return WorldType.GOD;
        return WorldType.NORMAL;
    }

    private void onEnter(Player p, WorldType type) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        switch (type) {
            case TIME_REALM:
                inTimeRealm.add(p.getUniqueId());
                d.worldKey(WorldKey.TIME_REALM);
                Msg.send(p, "&e&l[시간계] &f시간이 어긋난다. 5분마다 흐름이 변동.");
                break;
            case DREAM:
                inDream.add(p.getUniqueId());
                d.worldKey(WorldKey.DREAM);
                Msg.send(p, "&d&l[꿈] &f상상력이 곧 힘. 정신력이 핵심이다.");
                break;
            case VOID:
                inVoid.add(p.getUniqueId());
                d.worldKey(WorldKey.VOID);
                Msg.send(p, "&8&l[허공] &f방향이 무의미하다. 영원히 표류할 수 있다.");
                break;
            case GOD:
                if (d.getStat(StatType.DIVINITY) <= 0) {
                    Msg.error(p, "&c신만이 신계에 들어설 수 있다.");
                    var lobby = Bukkit.getWorld("lobby");
                    if (lobby != null) p.teleport(lobby.getSpawnLocation());
                    return;
                }
                inGodRealm.add(p.getUniqueId());
                d.worldKey(WorldKey.GOD);
                Msg.send(p, "&6&l[신계] &f신의 영역.");
                break;
        }
    }

    private void onExit(Player p, WorldType type) {
        switch (type) {
            case TIME_REALM: inTimeRealm.remove(p.getUniqueId()); break;
            case DREAM:      inDream.remove(p.getUniqueId()); break;
            case VOID:       inVoid.remove(p.getUniqueId()); break;
            case GOD:        inGodRealm.remove(p.getUniqueId()); break;
        }
    }

    private void tickResidents() {
        // 꿈: 정신력 < 10이면 강제 깸 (로비로)
        for (UUID id : new HashSet<>(inDream)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            PlayerData d = RebornCore.get().api().getPlayerData(id);
            if (d == null) continue;
            // 꿈 속에서 매 초 정신력 -0.1, 상상력 보너스로 매 초 INTELLIGENCE +0.05
            d.addStat(StatType.MENTAL, -0.1);
            d.addStat(StatType.INTELLIGENCE, 0.05);
            if (d.getStat(StatType.MENTAL) < 10) {
                Msg.warn(p, "&7꿈에서 깬다...");
                var lobby = Bukkit.getWorld("lobby");
                if (lobby != null) p.teleport(lobby.getSpawnLocation());
            }
        }
        // 허공: 5% 확률로 무작위 텔레포트 (같은 월드 내 ±100 블록)
        for (UUID id : inVoid) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (Rand.chance(0.01)) {
                p.teleport(p.getLocation().add(Rand.rangeD(-100, 100), 0, Rand.rangeD(-100, 100)));
                Msg.warn(p, "&8허공이 너를 다른 곳으로 이끈다.");
            }
        }
    }

    private void scrambleTimeRealm() {
        if (inTimeRealm.isEmpty()) return;
        World w = Bukkit.getWorld("time_realm");
        if (w == null) return;
        double mult = Rand.rangeD(0.1, 20.0);
        // 마인크래프트는 직접적 tick rate 변경이 불가하니, 시간만 점프 + 알림.
        w.setTime((long) (w.getTime() + mult * 1000) % 24000);
        for (UUID id : inTimeRealm) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) Msg.warn(p, "&e[시간계] &7흐름이 " + String.format("%.1f", mult) + "배로 변동");
        }
    }

    private enum WorldType { NORMAL, TIME_REALM, DREAM, VOID, GOD }
}
