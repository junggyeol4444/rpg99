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

    public TimeChamber(RebornTime p) { this.plugin = p; }

    public boolean enter(Player p, String chamberId) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (RebornCore.get().api().getTotalStats(p.getUniqueId())
                < plugin.getConfig().getInt("time-chamber.enter-tier-stats", 200)) {
            Msg.error(p, "중룡 이상이어야 진입 가능."); return false;
        }
        if (d.getStat(StatType.DRAGON_POWER)
                < plugin.getConfig().getInt("time-chamber.enter-dragon-power", 500)) {
            Msg.error(p, "용력 500 이상 필요."); return false;
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
        Msg.send(p, "&5시간의 방 — 내부 10년 = 외부 1년.");
        return true;
    }

    public void exit(Player p) {
        Long e = entryAt.remove(p.getUniqueId());
        if (e == null) return;
        long realMs = System.currentTimeMillis() - e;
        int ratio = plugin.getConfig().getInt("time-chamber.ratio", 10);
        int internalYears = (int) ((realMs / 3_600_000.0) * ratio);
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        d.dragonAge(d.dragonAge() + internalYears);
        lastExit.put(p.getUniqueId(), System.currentTimeMillis());
        // 본세계로
        World w = Bukkit.getWorld("dragon");
        if (w != null) p.teleport(w.getSpawnLocation());
        Msg.send(p, "&5시간의 방 퇴장 — 내부 " + internalYears + "년 경과. 드래곤 나이: " + d.dragonAge());
    }
}
