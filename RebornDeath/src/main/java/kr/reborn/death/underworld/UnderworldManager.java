package kr.reborn.death.underworld;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UnderworldManager {

    private final RebornDeath plugin;
    private final ConcurrentHashMap<UUID, Long> arrivalTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> deathPoint = new ConcurrentHashMap<>();

    public UnderworldManager(RebornDeath p) { this.plugin = p; }

    public void sendToUnderworld(Player p) {
        deathPoint.put(p.getUniqueId(), p.getLocation());
        var c = plugin.getConfig().getConfigurationSection("underworld");
        World w = Bukkit.getWorld(c.getString("world", "underworld"));
        if (w == null) {
            Msg.error(p, "명계 월드가 없습니다 — 부활됨.");
            return;
        }
        Location arrive = new Location(w, c.getDouble("arrive.x"), c.getDouble("arrive.y"), c.getDouble("arrive.z"));
        p.spigot().respawn();
        Bukkit.getScheduler().runTask(plugin, () -> p.teleport(arrive));
        arrivalTime.put(p.getUniqueId(), System.currentTimeMillis());

        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        WorldKey prev = d.worldKey();
        d.worldKey(WorldKey.UNDERWORLD);
        Bukkit.getPluginManager().callEvent(
                new kr.reborn.core.event.RebornWorldChangeEvent(p, prev, WorldKey.UNDERWORLD));
        Msg.send(p, "&8너의 영혼은 명계에 도달했다.");
        Msg.send(p, "&7/underworld revive | reincarnate | stay");
    }

    public boolean revive(Player p) {
        Long arrived = arrivalTime.get(p.getUniqueId());
        if (arrived == null) return false;
        long min = plugin.getConfig().getLong("underworld.revive-min-seconds", 300) * 1000;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        boolean canByTime = System.currentTimeMillis() - arrived >= min;
        boolean canByKi = d.getStat(StatType.UNDERWORLD_KI) >= plugin.getConfig().getInt("underworld.revive-min-underworld-ki", 50);
        if (!canByTime && !canByKi) {
            long left = (min - (System.currentTimeMillis() - arrived)) / 1000;
            Msg.warn(p, "환혼 불가 — " + left + "초 더 필요하거나 명기 50 이상 필요.");
            return false;
        }
        Location dp = deathPoint.remove(p.getUniqueId());
        WorldKey prev = d.worldKey();
        if (dp != null) {
            // 사망 전 세계로 환원
            try { d.worldKey(WorldKey.valueOf(dp.getWorld().getName().toUpperCase())); }
            catch (Exception ignored) { d.worldKey(WorldKey.FANTASY); }
            p.teleport(dp);
        }
        arrivalTime.remove(p.getUniqueId());
        Msg.send(p, "&6환혼 — 너의 영혼은 다시 육신을 얻었다.");
        Bukkit.getPluginManager().callEvent(
                new kr.reborn.core.event.RebornWorldChangeEvent(p, prev, d.worldKey()));
        return true;
    }

    public void reincarnate(Player p) {
        Long arrived = arrivalTime.get(p.getUniqueId());
        if (arrived == null) return;
        long min = plugin.getConfig().getLong("underworld.reincarnate-min-seconds", 1800) * 1000;
        if (System.currentTimeMillis() - arrived < min) {
            Msg.warn(p, "윤회 대기 시간 부족."); return;
        }
        // 명왕 심판 — 명기 100 이상 또는 명계 체류 60분 이상이면 통과
        PlayerData chk = RebornCore.get().api().getPlayerData(p.getUniqueId());
        boolean passedJudgment = chk.getStat(kr.reborn.core.data.StatType.UNDERWORLD_KI) >= 100
                || System.currentTimeMillis() - arrived >= 3_600_000L;
        if (!passedJudgment) {
            Msg.warn(p, "&8명왕의 심판이 너를 인정하지 않는다. 명기 100 또는 1시간 체류 필요.");
            return;
        }
        org.bukkit.Bukkit.broadcastMessage("§8§l[명왕 심판] §f" + p.getName()
                + "의 영혼이 윤회를 인정받았다.");
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        // 이전 경지 보너스
        var bonusMap = plugin.getConfig().getConfigurationSection("reincarnation.bonus-by-tier");
        if (bonusMap != null) {
            String tier = d.tier();
            var b = bonusMap.getConfigurationSection(tier);
            if (b == null) b = bonusMap.getConfigurationSection("default");
            if (b != null) {
                for (String key : b.getKeys(false)) {
                    try { d.addStat(StatType.valueOf(key), b.getDouble(key)); } catch (Exception ignored) {}
                }
            }
        }
        // 모든 비영구 데이터 초기화는 RebornSpawn 룰렛에서 자연스럽게 진행되므로
        // 여기서는 필수 플래그만 정리.
        d.reincarnations(d.reincarnations() + 1);
        d.tier("");
        d.titleId("");
        d.clanId("");
        d.lineage("");
        d.gymUsed(false);
        d.childStart(false);
        d.worldKey(WorldKey.LOBBY);
        for (StatType t : StatType.COMMON_8) d.setStat(t, 1);
        d.setStat(StatType.UNDERWORLD_KI, 0);

        // 환생의 월드 이동
        World lobby = Bukkit.getWorld("lobby");
        if (lobby != null) p.teleport(lobby.getSpawnLocation());
        Msg.send(p, "&6윤회 — 모든 것이 초기화되었다.");
        arrivalTime.remove(p.getUniqueId());
        deathPoint.remove(p.getUniqueId());
    }

    public void stay(Player p) {
        Msg.send(p, "&7명계에 잔류한다. 명기를 키워라.");
    }

    public Location deathPointOf(UUID id) { return deathPoint.get(id); }
    public Long arrivedAt(UUID id) { return arrivalTime.get(id); }
}
