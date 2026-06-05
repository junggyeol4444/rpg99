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
    /** uuid → 현재 적용 중인 부부 보너스 (스탯별 양). 다음 tick에 정확히 차감 후 재적용. */
    private final Map<UUID, java.util.EnumMap<StatType, Double>> lastCoupleBonus = new ConcurrentHashMap<>();
    /** 이번 tick에서 buff 받은 uuid set — tick 종료 후 미수신 자에게서 회수. */
    private final java.util.Set<UUID> boostedThisTick = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        // 양쪽 부부 보너스 즉시 회수
        revokeCoupleBoost(m.a);
        revokeCoupleBoost(m.b);
        Msg.send(p, "&7이혼이 성립되었다.");
        save();
    }

    /** 30블록 내 배우자 함께 있으면 양쪽 모두 공통 스탯 +5%. */
    private void tickCoupleBuff() {
        double radius = plugin.getConfig().getDouble("marriage.buff-radius", 30);
        double percent = plugin.getConfig().getDouble("marriage.buff-stat-percent", 5) / 100.0;
        boostedThisTick.clear();
        for (Marriage m : marriages.values()) {
            if (!m.npcId.isEmpty()) continue; // NPC 결혼은 거리 체크 안 함
            Player pa = Bukkit.getPlayer(m.a);
            Player pb = Bukkit.getPlayer(m.b);
            if (pa == null || pb == null) continue;
            if (pa.getWorld() != pb.getWorld()) continue;
            if (pa.getLocation().distance(pb.getLocation()) > radius) continue;
            applyCoupleBoost(pa, percent);
            applyCoupleBoost(pb, percent);
            boostedThisTick.add(pa.getUniqueId());
            boostedThisTick.add(pb.getUniqueId());
        }
        // 이전엔 받았지만 이번 tick엔 못 받은 사람 — 보너스 회수 (거리 벗어남)
        for (UUID id : new java.util.HashSet<>(lastCoupleBonus.keySet())) {
            if (!boostedThisTick.contains(id)) revokeCoupleBoost(id);
        }
    }

    /** 이전 보너스를 정확히 차감 후 새 보너스 적용 — 누적·잔여 폭주 방지. */
    private void applyCoupleBoost(Player p, double percent) {
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;

        var prev = lastCoupleBonus.computeIfAbsent(p.getUniqueId(),
                k -> new java.util.EnumMap<>(StatType.class));
        var fresh = new java.util.EnumMap<StatType, Double>(StatType.class);

        for (var st : StatType.COMMON_8) {
            double base = d.getStat(st);
            double prevBonus = prev.getOrDefault(st, 0.0);
            // base에는 이전 보너스가 포함됨 → 자연 base 추출
            double naturalBase = Math.max(0, base - prevBonus);
            if (naturalBase <= 0) continue;
            double newBonus = naturalBase * percent;
            double delta = newBonus - prevBonus;
            if (Math.abs(delta) > 0.01) {
                RebornCore.get().api().addStat(p.getUniqueId(), st, delta, "couple-buff");
            }
            fresh.put(st, newBonus);
        }
        lastCoupleBonus.put(p.getUniqueId(), fresh);

        // 시각 효과
        try {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE, 700, 0, true, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 700, 0, true, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, 700, 0, true, false));
        } catch (Throwable ignored) {}
    }

    /** 거리 벗어남·이혼·오프라인 시 보너스 회수. */
    public void revokeCoupleBoost(UUID uuid) {
        var prev = lastCoupleBonus.remove(uuid);
        if (prev == null) return;
        for (var e : prev.entrySet()) {
            if (e.getValue() != 0) {
                RebornCore.get().api().addStat(uuid, e.getKey(), -e.getValue(), "couple-buff-revoke");
            }
        }
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
