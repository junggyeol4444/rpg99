package kr.reborn.death.abyss;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 심연계 진입·체류 처리.
 * - 진입 시 모든 스탯 50% 즉시 감소 (delta 기록)
 * - 체류 중 매 분마다 심연 내성 -1, 0이 되면 영혼 소멸
 * - 퇴장 시 진입 때 기록한 delta만큼 정확히 환원 (×2 인플레이션 차단)
 */
public final class AbyssWorld implements Listener {

    private static final String NS = "RebornDeath.abyss";

    private final RebornDeath plugin;
    /** 심연 체류 중인 플레이어 — tick + 이동 이벤트 동시 접근, ConcurrentHashSet. */
    private final Set<UUID> insideAbyss = ConcurrentHashMap.newKeySet();
    /** 진입 시 차감한 스탯 양 기록 — 퇴장 시 동일 양만 복원. ×2 인플레이션 익스플로잇 차단. */
    private final java.util.Map<UUID, java.util.EnumMap<StatType, Double>> appliedPenalty
            = new ConcurrentHashMap<>();

    public AbyssWorld(RebornDeath p) {
        this.plugin = p;
        // 1분마다 체류자 정신 잠식
        RebornCore.get().scheduler().runTimer(this::tickAbyssResidents, 1200L, 1200L);
    }

    /** 플레이어 join 시 — 심연 월드에 있으면 자동으로 insideAbyss에 등록 + KV에서 페널티 복원. */
    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (isAbyss(p.getWorld())) {
            insideAbyss.add(p.getUniqueId());
            // 재시작 후 페널티가 휘발되면 exit 시 스탯 복원 불가 → KV에서 복원.
            loadPenaltyFromKV(p.getUniqueId());
        }
    }

    /** 진입 시 KV에 페널티 저장 — 재시작 후에도 exit 정확 환원 가능. */
    private void persistPenalty(UUID id, java.util.EnumMap<StatType, Double> penalty) {
        try {
            StringBuilder sb = new StringBuilder();
            for (var e : penalty.entrySet()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(e.getKey().name()).append(':').append(e.getValue());
            }
            RebornCore.get().kv().put(NS, id, "penalty", sb.toString());
        } catch (Throwable ignored) {}
    }

    private void loadPenaltyFromKV(UUID id) {
        try {
            String enc = RebornCore.get().kv().get(NS, id, "penalty");
            if (enc == null || enc.isEmpty()) return;
            java.util.EnumMap<StatType, Double> map = new java.util.EnumMap<>(StatType.class);
            for (String pair : enc.split(",")) {
                int colon = pair.indexOf(':');
                if (colon <= 0) continue;
                try {
                    StatType t = StatType.valueOf(pair.substring(0, colon));
                    double v = Double.parseDouble(pair.substring(colon + 1));
                    map.put(t, v);
                } catch (Throwable ignored) {}
            }
            if (!map.isEmpty()) appliedPenalty.put(id, map);
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) return;
        boolean wasIn = isAbyss(e.getFrom().getWorld());
        boolean nowIn = isAbyss(e.getTo().getWorld());
        if (!wasIn && nowIn) onEnter(e.getPlayer());
        else if (wasIn && !nowIn) onExit(e.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getWorld() == e.getTo().getWorld()) return;
        boolean wasIn = isAbyss(e.getFrom().getWorld());
        boolean nowIn = isAbyss(e.getTo().getWorld());
        if (!wasIn && nowIn) onEnter(e.getPlayer());
        else if (wasIn && !nowIn) onExit(e.getPlayer());
    }

    private boolean isAbyss(World w) {
        return w != null && "abyss".equalsIgnoreCase(w.getName());
    }

    private void onEnter(Player p) {
        // 이미 심연 set에 있으면 페널티 중복 적용 안 함
        if (!insideAbyss.add(p.getUniqueId())) return;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        // 각 스탯의 50% 차감량을 정확히 기록 — 퇴장 시 동일량 환원.
        // (이전엔 ×0.5 / ×2.0 패턴이라 체류 중 스탯 획득 시 인플레이션 발생했음.)
        java.util.EnumMap<StatType, Double> penalty = new java.util.EnumMap<>(StatType.class);
        for (StatType t : StatType.COMMON_8) {
            double cur = d.getStat(t);
            double cut = cur * 0.5;
            d.setStat(t, cur - cut);
            penalty.put(t, cut);
        }
        appliedPenalty.put(p.getUniqueId(), penalty);
        persistPenalty(p.getUniqueId(), penalty);  // 재시작 후에도 복원 가능
        // 심연 내성 초기 100 부여
        if (d.getStat(StatType.ABYSS_RESISTANCE) <= 0) {
            d.setStat(StatType.ABYSS_RESISTANCE, 100);
        }
        Bukkit.broadcastMessage("§0§l[심연] §f" + p.getName() + "이(가) 심연에 발을 들였다.");
        Msg.error(p, "&0심연 — 모든 스탯 50% 감소. 매 분 정신 잠식.");
        d.worldKey(WorldKey.ABYSS);
    }

    private void onExit(Player p) {
        // 심연 set에서 제거 — 없었으면 페널티도 없었던 것이므로 복원 안 함
        if (!insideAbyss.remove(p.getUniqueId())) return;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        // 진입 시 기록한 정확한 차감량을 다시 더함 (×2.0 인플레이션 차단).
        java.util.EnumMap<StatType, Double> penalty = appliedPenalty.remove(p.getUniqueId());
        if (penalty != null) {
            for (var e : penalty.entrySet()) {
                d.setStat(e.getKey(), d.getStat(e.getKey()) + e.getValue());
            }
        }
        try { RebornCore.get().kv().remove(NS, p.getUniqueId(), "penalty"); } catch (Throwable ignored) {}
        Msg.send(p, "&7심연을 벗어났다. 스탯 복원.");
    }

    private void tickAbyssResidents() {
        for (UUID id : insideAbyss) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            PlayerData d = RebornCore.get().api().getPlayerData(id);
            if (d == null) continue;
            d.addStat(StatType.ABYSS_RESISTANCE, -1);
            d.addStat(StatType.MENTAL, -2);
            if (d.getStat(StatType.ABYSS_RESISTANCE) <= 0) {
                // 영혼 흡수 — 영구 사망
                Bukkit.broadcastMessage("§0§l[심연 흡수] §f" + p.getName() + "의 영혼이 심연에 흡수되었다.");
                Bukkit.getPluginManager().callEvent(
                        new RebornDeathEvent(p, p.getLocation(), null, "ABYSS_CONSUMED"));
                p.setHealth(0);
                // 심연계는 윤회 강제 (RebornDeath UnderworldManager에서 처리)
            }
        }
    }
}
