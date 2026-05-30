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

    private final RebornSpawn plugin;
    private final Map<UUID, Race> playerRace = new ConcurrentHashMap<>();

    public RaceManager(RebornSpawn plugin) { this.plugin = plugin; }

    /** 룰렛 결과 호출 직후 — 세계에 따라 종족 추첨. */
    public Race assignRandom(Player p, WorldKey world) {
        List<Race> candidates = Race.availableFor(world);
        if (candidates.isEmpty()) return null;
        Race chosen = weightedPick(candidates);
        playerRace.put(p.getUniqueId(), chosen);
        applyBonuses(p, chosen);
        announce(p, chosen);
        return chosen;
    }

    /** 명시적 종족 설정 (관리자 명령 또는 특수 이벤트). */
    public void setRace(Player p, Race r) {
        playerRace.put(p.getUniqueId(), r);
        applyBonuses(p, r);
        Msg.send(p, "&6종족이 §6" + r.koreanName + " §7으로 설정되었다.");
    }

    public Race raceOf(UUID p) { return playerRace.get(p); }

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
