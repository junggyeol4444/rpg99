package kr.reborn.worldai.migration;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.worldai.RebornWorldAI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NPC 세계간 이주 시뮬레이터.
 *
 * 이주 유형:
 *   REFUGEE  — 전쟁/재해 발생 세계에서 이탈
 *   TRADER   — 시장가 차이가 큰 세계로 상인 이동
 *   CONQUEROR — 야망 큰 세력의 정복자 이동 (적국에 잠입)
 *   EXILE    — 봉기 진압 후 추방
 *   PILGRIM  — 신앙/축제로 다른 세계 방문
 *
 * 매 사이클:
 *   - 출발지 결정: 긴장도·재해 발생률·시장 부족 등으로 가중치
 *   - 목적지: 안정도 높고 시장 풍요로운 곳
 *   - 이주자 수 = 1~5명 (broadcast + RebornNPC reflection 이주 처리)
 */
public final class MigrationEngine {

    public enum Kind { REFUGEE, TRADER, CONQUEROR, EXILE, PILGRIM }

    private final RebornWorldAI plugin;
    private final Map<WorldKey, Map<Kind, Integer>> outflow = new HashMap<>();
    private final Map<WorldKey, Map<Kind, Integer>> inflow = new HashMap<>();

    public MigrationEngine(RebornWorldAI plugin) {
        this.plugin = plugin;
        for (WorldKey w : WorldKey.values()) {
            outflow.put(w, new HashMap<>());
            inflow.put(w, new HashMap<>());
        }
    }

    public void cycle() {
        if (!Rand.chance(0.3)) return; // 사이클 30% 확률로 이주 발생
        // 출발지 선택: 가장 긴장 높은 세계
        WorldKey origin = pickOrigin();
        if (origin == null) return;
        WorldKey destination = pickDestination(origin);
        if (destination == null || origin == destination) return;
        Kind kind = pickKind(origin);
        int count = 1 + Rand.range(0, 4);

        outflow.get(origin).merge(kind, count, Integer::sum);
        inflow.get(destination).merge(kind, count, Integer::sum);

        String label = switch (kind) {
            case REFUGEE -> "난민";
            case TRADER -> "상인";
            case CONQUEROR -> "정복자 척후";
            case EXILE -> "추방자";
            case PILGRIM -> "순례자";
        };
        Bukkit.broadcastMessage("§b[이주] §f" + origin + " §7→ §f" + destination
                + " §7| " + label + " §f" + count + "명");
        // 실제 NPC 스폰을 시도 (선택적, 도착지 부 NPC 1명만 — 너무 많이 스폰하지 않게)
        if (kind == Kind.TRADER || kind == Kind.PILGRIM) {
            spawnArrivedNpc(destination, kind, origin);
        }
    }

    private WorldKey pickOrigin() {
        WorldKey best = null;
        double bestScore = 0;
        for (var ai : plugin.all()) {
            double score = ai.state().tension + (100 - ai.state().stability);
            if (score > bestScore) { bestScore = score; best = ai.world(); }
        }
        return best;
    }

    private WorldKey pickDestination(WorldKey origin) {
        WorldKey best = null;
        double bestScore = 0;
        for (var ai : plugin.all()) {
            if (ai.world() == origin) continue;
            double score = ai.state().stability + (100 - ai.state().tension);
            if (score > bestScore) { bestScore = score; best = ai.world(); }
        }
        return best;
    }

    private Kind pickKind(WorldKey origin) {
        var ai = plugin.of(origin);
        if (ai == null) return Kind.TRADER;
        double t = ai.state().tension;
        double s = ai.state().stability;
        if (t > 80) return Rand.chance(0.7) ? Kind.REFUGEE : Kind.CONQUEROR;
        if (s < 30) return Rand.chance(0.5) ? Kind.EXILE : Kind.REFUGEE;
        if (s > 70 && Rand.chance(0.3)) return Kind.PILGRIM;
        return Kind.TRADER;
    }

    private void spawnArrivedNpc(WorldKey dest, Kind kind, WorldKey origin) {
        try {
            Plugin np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np == null) return;
            Object registry = np.getClass().getMethod("registry").invoke(np);
            String id = "migrant_" + kind.name().toLowerCase() + "_"
                    + (System.currentTimeMillis() % 100000);
            String name = (kind == Kind.TRADER ? "§e[이주 상인] " : "§b[순례자] ")
                    + origin.name() + "에서 옴";
            // 위치는 plugin 내부 결정 — 단순화: 첫번째 온라인 플레이어 위치
            org.bukkit.entity.Player firstP = null;
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                if (kr.reborn.core.RebornCore.get().api().getPlayerData(p.getUniqueId())
                        .worldKey() == dest) { firstP = p; break; }
            }
            if (firstP == null) return;
            Object loc = firstP.getLocation();
            registry.getClass().getMethod("spawn",
                            String.class, String.class, WorldKey.class,
                            Class.forName("org.bukkit.Location"), String.class, String.class)
                    .invoke(registry, id, name, dest, loc, "neutral",
                            kind == Kind.TRADER ? "MERCHANT" : "VILLAGER");
        } catch (Throwable ignored) {}
    }

    public Map<Kind, Integer> outflow(WorldKey w) {
        return outflow.getOrDefault(w, java.util.Collections.emptyMap());
    }

    public Map<Kind, Integer> inflow(WorldKey w) {
        return inflow.getOrDefault(w, java.util.Collections.emptyMap());
    }
}
