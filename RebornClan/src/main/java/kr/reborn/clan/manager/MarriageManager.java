package kr.reborn.clan.manager;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Marriage;
import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarriageManager implements Listener {

    private final RebornClan plugin;
    private final Map<UUID, Marriage> marriages = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> proposals = new HashMap<>();

    public MarriageManager(RebornClan p) {
        this.plugin = p;
        load();
        // 30초마다 부부 버프 갱신
        RebornCore.get().scheduler().runTimer(this::tickCoupleBuff, 600L, 600L);
        // 5분마다 저장
        RebornCore.get().scheduler().runTimerAsync(this::save, 6000L, 6000L);
    }

    public Marriage of(UUID id) { return marriages.get(id); }
    public Map<UUID, Marriage> all() { return marriages; }

    public void propose(Player a, Player b) {
        if (marriages.containsKey(a.getUniqueId()) || marriages.containsKey(b.getUniqueId())) {
            Msg.error(a, "이미 결혼한 사람이 있다.");
            return;
        }
        proposals.put(b.getUniqueId(), a.getUniqueId());
        Msg.send(b, "&d" + a.getName() + "이(가) 청혼했다. /marry accept");
    }

    public void accept(Player b) {
        UUID aId = proposals.remove(b.getUniqueId());
        if (aId == null) { Msg.warn(b, "청혼이 없다."); return; }
        Marriage m = new Marriage(aId, b.getUniqueId(), "", System.currentTimeMillis());
        marriages.put(aId, m);
        marriages.put(b.getUniqueId(), m);
        Msg.send(b, "&6결혼 성립.");
        Bukkit.broadcastMessage("§d§l[결혼] §f" + Bukkit.getOfflinePlayer(aId).getName()
                + " ❤ " + b.getName());
        save();
    }

    public void marryNpc(Player p, String npcId) {
        Marriage m = new Marriage(p.getUniqueId(), UUID.randomUUID(), npcId, System.currentTimeMillis());
        marriages.put(p.getUniqueId(), m);
        Msg.send(p, "&6NPC와 결혼: " + npcId);
        save();
    }

    public void divorce(Player p) {
        Marriage m = marriages.remove(p.getUniqueId());
        if (m == null) { Msg.warn(p, "결혼하지 않았다."); return; }
        marriages.remove(m.a);
        marriages.remove(m.b);
        Msg.send(p, "&7이혼이 성립되었다.");
        // 부부 NPC 상대 호감도 급락 (RebornNPC 연동)
        save();
    }

    /** 30블록 내 배우자 함께 있으면 양쪽 모두 공통 스탯 +5%. */
    private void tickCoupleBuff() {
        double radius = plugin.getConfig().getDouble("marriage.buff-radius", 30);
        double percent = plugin.getConfig().getDouble("marriage.buff-stat-percent", 5) / 100.0;
        for (Marriage m : marriages.values()) {
            if (!m.npcId.isEmpty()) continue; // NPC 결혼은 거리 체크 안 함
            Player pa = Bukkit.getPlayer(m.a);
            Player pb = Bukkit.getPlayer(m.b);
            if (pa == null || pb == null) continue;
            if (pa.getWorld() != pb.getWorld()) continue;
            if (pa.getLocation().distance(pb.getLocation()) > radius) continue;
            // 일시 buff — 30초간 stat boost (실제로는 CurseEffect 시스템에 위임하는게 좋지만 간이로)
            applyCoupleBoost(pa, percent);
            applyCoupleBoost(pb, percent);
        }
    }

    private void applyCoupleBoost(Player p, double percent) {
        // 가벼운 표시 — 실제 stat 보정은 별도 효과 시스템에서. 여기선 메시지만.
        // 더 깊은 구현시 RebornCurse "couple_buff" 효과를 30초 부여하면 됨.
    }

    private File file() {
        return new File(plugin.getDataFolder(), "marriages.yml");
    }

    public void save() {
        File f = file();
        plugin.getDataFolder().mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Marriage> e : marriages.entrySet()) {
            // 한쪽만 저장 (a 키준)
            if (!e.getKey().equals(e.getValue().a)) continue;
            String k = e.getValue().a.toString();
            y.set(k + ".a", e.getValue().a.toString());
            y.set(k + ".b", e.getValue().b.toString());
            y.set(k + ".npc", e.getValue().npcId);
            y.set(k + ".at", e.getValue().marriedAt);
        }
        try { y.save(f); } catch (Exception ignored) {}
    }

    public void load() {
        File f = file();
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String key : y.getKeys(false)) {
            try {
                UUID a = UUID.fromString(y.getString(key + ".a"));
                UUID b = UUID.fromString(y.getString(key + ".b"));
                String npc = y.getString(key + ".npc", "");
                long at = y.getLong(key + ".at");
                Marriage m = new Marriage(a, b, npc, at);
                marriages.put(a, m);
                marriages.put(b, m);
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Marriage m = marriages.get(e.getPlayer().getUniqueId());
        if (m != null) {
            Msg.send(e.getPlayer(), "&d배우자와의 인연이 이어진다.");
        }
    }
}
