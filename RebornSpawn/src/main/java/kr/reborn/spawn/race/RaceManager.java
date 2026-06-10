package kr.reborn.spawn.race;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.spawn.RebornSpawn;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 종족 매니저.
 *
 * 환생 룰렛 직후 호출 → 세계 가능 종족 중 weight 가중 추첨 → 영구 스탯 보너스 적용.
 * 외부에서 raceOf(p), assignRace(p, w) 등으로 조회/조작.
 *
 * RebornStat FantasyGrowth와 별개 — 이쪽이 더 광범위 (13세계 전체).
 */
public final class RaceManager {

    private static final String NS = "RebornSpawn.race";

    private final RebornSpawn plugin;
    private final Map<UUID, Race> playerRace = new ConcurrentHashMap<>();

    public RaceManager(RebornSpawn plugin) { this.plugin = plugin; }

    /** 룰렛 결과 호출 직후 — 세계에 따라 종족 추첨. */
    public Race assignRandom(Player p, WorldKey world) {
        List<Race> candidates = Race.availableFor(world);
        if (candidates.isEmpty()) return null;
        Race chosen = weightedPick(candidates);
        // 환생 반복 호출로 종족 보너스가 누적되던 결함 방지 — 직전 종족 효과 회수
        Race prev = raceOf(p.getUniqueId());
        if (prev != null && prev != chosen) removeBonuses(p, prev);
        playerRace.put(p.getUniqueId(), chosen);
        applyBonuses(p, chosen);
        persist(p.getUniqueId(), chosen);
        announce(p, chosen);
        return chosen;
    }

    /** 명시적 종족 설정 (관리자 명령 또는 특수 이벤트). */
    public void setRace(Player p, Race r) {
        Race prev = raceOf(p.getUniqueId());
        if (prev != null && prev != r) removeBonuses(p, prev);
        playerRace.put(p.getUniqueId(), r);
        applyBonuses(p, r);
        persist(p.getUniqueId(), r);
        Msg.send(p, "&6종족이 §6" + r.koreanName + " §7으로 설정되었다.");
    }

    public Race raceOf(UUID p) {
        Race r = playerRace.get(p);
        if (r != null) return r;
        // 캐시에 없으면 DB에서 로드
        String stored = RebornCore.get().kv().get(NS, p, "race");
        if (stored != null) {
            try {
                r = Race.valueOf(stored);
                playerRace.put(p, r);
                return r;
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private void persist(UUID p, Race r) {
        try { RebornCore.get().kv().put(NS, p, "race", r.name()); }
        catch (Throwable ignored) {}
    }

    private Race weightedPick(List<Race> list) {
        double total = list.stream().mapToDouble(r -> r.weight).sum();
        double pick = Math.random() * total;
        double acc = 0;
        for (Race r : list) {
            acc += r.weight;
            if (acc >= pick) return r;
        }
        return list.get(list.size() - 1);
    }

    private void applyBonuses(Player p, Race r) {
        for (var e : r.bonus.entrySet()) {
            try {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        e.getKey(), e.getValue(), "race:" + r.name());
            } catch (Throwable ignored) {}
        }
    }

    /** 종족 효과 회수 — 환생·재배정 시 호출. */
    private void removeBonuses(Player p, Race r) {
        for (var e : r.bonus.entrySet()) {
            try {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        e.getKey(), -e.getValue(), "race-revoke:" + r.name());
            } catch (Throwable ignored) {}
        }
    }

    private void announce(Player p, Race r) {
        if (r.weight <= 0.1) {
            // 초레어 종족 - 전체 브로드캐스트
            Bukkit.broadcastMessage("§6§l[종족 강림] §f" + p.getName()
                    + " §7가 희귀 종족 §6" + r.koreanName + " §7으로 환생했다! "
                    + "§e(가중치 " + r.weight + ")");
        } else {
            Msg.send(p, "&6종족: §e" + r.koreanName);
        }
        try {
            p.sendTitle("§6환생 종족", "§f" + r.koreanName, 10, 60, 20);
        } catch (Throwable ignored) {}
    }

    /** /race info 등에서 호출. */
    public Map<UUID, Race> allAssignments() { return playerRace; }
}
