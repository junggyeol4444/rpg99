package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Territory;
import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TerritoryManager implements Listener {

    private final RebornClan plugin;
    private final Map<String, Territory> claims = new ConcurrentHashMap<>();
    /** territoryKey → 전쟁 정보 */
    private final Map<String, War> activeWars = new ConcurrentHashMap<>();
    /** PvP 활성 (영토 전쟁 중) 플레이어 UUID */
    private final Set<UUID> pvpActive = new HashSet<>();

    public TerritoryManager(RebornClan p) {
        this.plugin = p;
        load();
        // 5분마다 저장
        RebornCore.get().scheduler().runTimerAsync(this::save, 6000L, 6000L);
        // 1초마다 전쟁 깃발 점거 체크
        RebornCore.get().scheduler().runTimer(this::tickWars, 20L, 20L);
    }

    public Territory at(Chunk c) {
        return claims.get(c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ());
    }

    public boolean claim(Player p) {
        Chunk c = p.getLocation().getChunk();
        Territory existing = at(c);
        if (existing != null) { Msg.error(p, "이미 점령된 영토."); return false; }
        Territory t = new Territory(c.getWorld().getName(), c.getX(), c.getZ(), p.getUniqueId());
        var clan = plugin.clans().ofPlayer(p.getUniqueId());
        if (clan != null) t.clanId = clan.id;
        claims.put(t.key(), t);
        Msg.send(p, "&a영토 점령: " + c.getX() + "," + c.getZ());
        return true;
    }

    public boolean unclaim(Player p) {
        Chunk c = p.getLocation().getChunk();
        Territory t = at(c);
        if (t == null || !t.owner.equals(p.getUniqueId())) {
            Msg.error(p, "내 영토가 아니다.");
            return false;
        }
        claims.remove(t.key());
        Msg.send(p, "&7영토 해제.");
        return true;
    }

    /** 영토 전쟁 선포 — 1시간 준비 후 PvP 활성, 깃발 점거 5분 시 이전. */
    public void declareWar(Player attacker, Territory target) {
        if (activeWars.containsKey(target.key())) {
            Msg.error(attacker, "이미 전쟁 중인 영토.");
            return;
        }
        long startAt = System.currentTimeMillis() + 3_600_000L;
        War w = new War(target.key(), target.owner, attacker.getUniqueId(), startAt);
        activeWars.put(target.key(), w);
        Bukkit.broadcastMessage("§c§l[전쟁 선포] §f" + attacker.getName()
                + "이(가) " + target.world + " (" + target.chunkX + "," + target.chunkZ + ") 점령 시도");
        // 준비 1시간 후 PvP 활성
        RebornCore.get().scheduler().runTaskLater(() -> {
            w.pvpActive = true;
            pvpActive.add(w.attacker);
            pvpActive.add(w.defender);
            Bukkit.broadcastMessage("§4§l[전쟁 시작] §f영토 전투 개시. 깃발 5분 점거 시 승리.");
        }, 72000L);  // 1시간 * 20 ticks/sec * 60 sec/min * 60 min
    }

    private void tickWars() {
        for (War w : activeWars.values()) {
            if (!w.pvpActive) continue;
            Player atk = Bukkit.getPlayer(w.attacker);
            Player def = Bukkit.getPlayer(w.defender);
            // 공격자가 깃발(영토 청크 중심) 5분 점거 = 승리
            Territory t = claims.get(w.territoryKey);
            if (t == null) continue;
            int cx = (t.chunkX << 4) + 8, cz = (t.chunkZ << 4) + 8;
            if (atk != null) {
                int dx = atk.getLocation().getBlockX() - cx;
                int dz = atk.getLocation().getBlockZ() - cz;
                if (dx * dx + dz * dz < 64) {
                    w.attackerHoldTicks += 20;
                    if (w.attackerHoldTicks >= 6000) { // 5분
                        finishWar(w, true);
                    }
                } else {
                    w.attackerHoldTicks = Math.max(0, w.attackerHoldTicks - 10);
                }
            }
            if (def != null && def.getLocation().distance(atk == null ? def.getLocation() : atk.getLocation()) < 8) {
                // 방어자가 공격자 근처 — hold 카운트 감소
                w.attackerHoldTicks = Math.max(0, w.attackerHoldTicks - 40);
            }
        }
    }

    private void finishWar(War w, boolean attackerWon) {
        activeWars.remove(w.territoryKey);
        pvpActive.remove(w.attacker);
        pvpActive.remove(w.defender);
        Territory t = claims.get(w.territoryKey);
        if (t == null) return;
        if (attackerWon) {
            t.owner = w.attacker;
            var clan = plugin.clans().ofPlayer(w.attacker);
            if (clan != null) t.clanId = clan.id;
            Bukkit.broadcastMessage("§6§l[전쟁 승리] §f"
                    + Bukkit.getOfflinePlayer(w.attacker).getName() + "이(가) 영토를 점령했다!");
        } else {
            Bukkit.broadcastMessage("§a§l[방어 성공] §f"
                    + Bukkit.getOfflinePlayer(w.defender).getName() + "이(가) 영토를 지켰다.");
        }
    }

    public boolean isInWar(UUID p) { return pvpActive.contains(p); }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        // 전쟁 PvP 활성자 간에는 허용, 외에는 차단 (영토 전쟁 시스템 정책)
        if (!pvpActive.contains(damager.getUniqueId()) || !pvpActive.contains(victim.getUniqueId())) {
            // 일반 PvP는 RebornDeath 정책. 여기서는 영토 보호만.
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // 영토 진입 알림 등은 별도 listener에서 처리
    }

    public void save() {
        File f = new File(plugin.getDataFolder(), "territories.yml");
        plugin.getDataFolder().mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        for (Territory t : claims.values()) {
            String k = t.key();
            y.set(k + ".world", t.world);
            y.set(k + ".x", t.chunkX);
            y.set(k + ".z", t.chunkZ);
            y.set(k + ".owner", t.owner.toString());
            y.set(k + ".clan", t.clanId);
        }
        try { y.save(f); } catch (Exception ignored) {}
    }

    public void load() {
        File f = new File(plugin.getDataFolder(), "territories.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String k : y.getKeys(false)) {
            try {
                Territory t = new Territory(
                        y.getString(k + ".world"),
                        y.getInt(k + ".x"),
                        y.getInt(k + ".z"),
                        UUID.fromString(y.getString(k + ".owner")));
                t.clanId = y.getString(k + ".clan", "");
                claims.put(t.key(), t);
            } catch (Throwable ignored) {}
        }
    }

    private static final class War {
        final String territoryKey;
        final UUID defender;
        final UUID attacker;
        final long startAt;
        boolean pvpActive = false;
        long attackerHoldTicks = 0;
        War(String key, UUID def, UUID atk, long start) {
            territoryKey = key; defender = def; attacker = atk; startAt = start;
        }
    }
}
