package kr.reborn.time.chamber;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TimeChamber {

    private final RebornTime plugin;
    private final Map<UUID, Long> lastExit = new HashMap<>();
    private final Map<UUID, Long> entryAt = new HashMap<>();
    /** uuid → 진입한 chamber 종류 (적용할 스탯 결정) */
    private final Map<UUID, String> entryChamber = new HashMap<>();

    public TimeChamber(RebornTime p) {
        this.plugin = p;
        // 매 1분마다 내부 인원에게 stat tick
        RebornCore.get().scheduler().runTimer(this::tickInside, 1200L, 1200L);
    }

    public boolean enter(Player p, String chamberId) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (RebornCore.get().api().getTotalStats(p.getUniqueId())
                < plugin.getConfig().getInt("time-chamber.enter-tier-stats", 200)) {
            Msg.error(p, "중룡 이상이어야 진입 가능."); return false;
        }
        long cd = plugin.getConfig().getLong("time-chamber.reentry-cooldown-real-hours", 24) * 3_600_000L;
        Long le = lastExit.get(p.getUniqueId());
        if (le != null && System.currentTimeMillis() - le < cd) {
            Msg.error(p, "재진입 대기 중."); return false;
        }
        World w = Bukkit.getWorld(chamberId);
        if (w == null) { Msg.error(p, "방 없음."); return false; }
        p.teleport(w.getSpawnLocation());
        entryAt.put(p.getUniqueId(), System.currentTimeMillis());
        entryChamber.put(p.getUniqueId(), chamberId);
        Msg.send(p, "&5" + chamberLabel(chamberId) + " 진입 — 내부 " + ratioOf(chamberId) + "× 가속.");
        return true;
    }

    public void exit(Player p) {
        Long e = entryAt.remove(p.getUniqueId());
        String chamber = entryChamber.remove(p.getUniqueId());
        if (e == null) return;
        long realMs = System.currentTimeMillis() - e;
        int ratio = ratioOf(chamber);
        int internalYears = (int) ((realMs / 3_600_000.0) * ratio);
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        // chamber 종류별 출구 스탯
        applyExitBonus(p, chamber, internalYears);
        d.dragonAge(d.dragonAge() + internalYears);
        lastExit.put(p.getUniqueId(), System.currentTimeMillis());
        World w = Bukkit.getWorld(exitWorldOf(chamber));
        if (w != null) p.teleport(w.getSpawnLocation());
        Msg.send(p, "&5" + chamberLabel(chamber) + " 퇴장 — 내부 " + internalYears + "년 경과.");
    }

    /** 매 1분 = 내부 (ratio)분 = 스탯 미세 +. */
    private void tickInside() {
        for (var entry : entryAt.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            String chamber = entryChamber.get(entry.getKey());
            int ratio = ratioOf(chamber);
            try {
                StatType primary = primaryStatOf(chamber);
                RebornCore.get().api().addStat(p.getUniqueId(), primary, ratio * 0.5, "chamber:" + chamber);
            } catch (Throwable ignored) {}
        }
    }

    private int ratioOf(String chamberId) {
        if (chamberId == null) return 10;
        return plugin.getConfig().getInt("time-chamber.ratios." + chamberId,
                plugin.getConfig().getInt("time-chamber.ratio", 10));
    }

    private String chamberLabel(String chamberId) {
        return switch (chamberId) {
            case "dragon_chamber" -> "용계 시간방";
            case "immortal_seclusion" -> "선계 폐관 동굴";
            case "martial_cliff" -> "전설의 절벽";
            case "demon_tower" -> "마계 마기탑";
            case "mind_palace" -> "정신의 궁전";
            case "spirit_grove" -> "정령의 숲";
            default -> "시간의 방";
        };
    }

    private String exitWorldOf(String chamberId) {
        return switch (chamberId) {
            case "immortal_seclusion" -> "immortal";
            case "martial_cliff" -> "martial";
            case "demon_tower" -> "demon";
            case "spirit_grove" -> "spirit";
            case "mind_palace" -> "lobby";
            default -> "dragon";
        };
    }

    private StatType primaryStatOf(String chamberId) {
        return switch (chamberId) {
            case "immortal_seclusion" -> StatType.IMMORTAL_KI;
            case "martial_cliff" -> StatType.INNER_KI;
            case "demon_tower" -> StatType.DEMON_KI;
            case "spirit_grove" -> StatType.SPIRIT_POWER;
            case "mind_palace" -> StatType.MENTAL;
            default -> StatType.DRAGON_POWER;
        };
    }

    private void applyExitBonus(Player p, String chamberId, int years) {
        try {
            StatType primary = primaryStatOf(chamberId);
            RebornCore.get().api().addStat(p.getUniqueId(),
                    primary, years * 10, "chamber-exit:" + chamberId);
            if ("immortal_seclusion".equals(chamberId) && years >= 100) {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MENTAL, 20, "seclusion-100y");
            }
            if ("martial_cliff".equals(chamberId) && years >= 30) {
                Bukkit.broadcastMessage("§5§l[절벽 수련] §f"
                        + p.getName() + " §7이(가) 깨달음을 얻고 내려왔다.");
            }
        } catch (Throwable ignored) {}
    }
}
