package kr.reborn.god.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 신 인스턴스 관리자.
 *
 * 플레이어 신: TrialManager가 3 시련을 통과 검증 후 ascend()를 호출.
 * NPC 신: config의 npc-gods 섹션 자동 로드.
 */
public final class GodManager {

    private static final String NS = "RebornGod.playerGod";

    private final RebornGod plugin;
    private final Map<UUID, God> playerGods = new HashMap<>();
    private final Map<String, God> npcGods = new HashMap<>();

    public GodManager(RebornGod p) {
        this.plugin = p;
        loadNpcGods();
        loadPlayerGods();
    }

    private void loadPlayerGods() {
        try {
            var data = kr.reborn.core.RebornCore.get().kv().loadNamespace(NS);
            // owner=playerUUID, key=field, value=...
            for (var ownerEntry : data.entrySet()) {
                String ownerStr = ownerEntry.getKey();
                if (ownerStr.isEmpty()) continue;
                try {
                    UUID owner = UUID.fromString(ownerStr);
                    Map<String, String> fields = ownerEntry.getValue();
                    String name = fields.getOrDefault("name", "?");
                    double divinity = parseD(fields.get("divinity"), 100);
                    God g = new God(owner, "", name, divinity);
                    g.influence = parseD(fields.get("influence"), 0);
                    g.warOpponent = fields.getOrDefault("warOpponent", "");
                    g.domainWorld = fields.getOrDefault("domainWorld", "");
                    playerGods.put(owner, g);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private double parseD(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s); } catch (Throwable ignored) { return def; }
    }

    /** 외부 호출 — 모든 플레이어 신 영구화. */
    public void saveAll() {
        var kv = kr.reborn.core.RebornCore.get().kv();
        for (God g : playerGods.values()) {
            if (g.owner == null) continue;
            kv.put(NS, g.owner, "name", g.name);
            kv.putDouble(NS, g.owner, "divinity", g.divinity);
            kv.putDouble(NS, g.owner, "influence", g.influence);
            kv.put(NS, g.owner, "warOpponent", g.warOpponent);
            kv.put(NS, g.owner, "domainWorld", g.domainWorld);
        }
    }

    private void loadNpcGods() {
        var sec = plugin.getConfig().getConfigurationSection("npc-gods");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            var s = sec.getConfigurationSection(key);
            if (s == null) continue;
            God g = new God(null, key, s.getString("name"), s.getDouble("divinity"));
            g.sealed = s.getBoolean("sealed", false);
            npcGods.put(key, g);
        }
        plugin.getLogger().info("NPC 신 " + npcGods.size() + "체 로드");
    }

    public God of(UUID player) { return playerGods.get(player); }
    public Collection<God> npcAll() { return npcGods.values(); }
    public Collection<God> playerAll() { return playerGods.values(); }

    /** 신 ID(npc:xxx 또는 player:UUID) → 인스턴스. */
    public God lookup(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;
        if (identifier.startsWith("player:")) {
            try { return of(UUID.fromString(identifier.substring(7))); }
            catch (Exception e) { return null; }
        }
        String npcId = identifier.startsWith("npc:") ? identifier.substring(4) : identifier;
        return npcGods.get(npcId);
    }

    /**
     * 신 등극 — 정상 경로는 TrialManager가 3 시련 통과 검증 후 호출.
     * 직접 호출 시(예: 콘솔/관리자) 절대자 검증만 수행.
     */
    public boolean ascend(Player p) {
        double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
        if (total < 5000) {
            Msg.error(p, "절대자(총합 5000+)만 신이 될 수 있다.");
            return false;
        }
        if (playerGods.containsKey(p.getUniqueId())) {
            Msg.warn(p, "이미 신이다."); return false;
        }
        God g = new God(p.getUniqueId(), "", p.getName() + " (신)", 100);
        playerGods.put(p.getUniqueId(), g);
        // 즉시 영구화 — 등극 사실은 절대 손실되면 안 됨
        var kv = RebornCore.get().kv();
        kv.put(NS, p.getUniqueId(), "name", g.name);
        kv.putDouble(NS, p.getUniqueId(), "divinity", g.divinity);
        RebornCore.get().api().setStat(p.getUniqueId(), StatType.DIVINITY, 100);
        Bukkit.broadcastMessage("§6§l[신격] §f" + p.getName() + "이(가) 신의 자리에 올랐다! §7(시작 신성 100)");
        return true;
    }

    public void addDivinity(Player p, double delta) {
        God g = playerGods.get(p.getUniqueId());
        if (g == null) return;
        g.divinity = Math.max(0, g.divinity + delta);
        RebornCore.get().api().setStat(p.getUniqueId(), StatType.DIVINITY, g.divinity);
        RebornCore.get().kv().putDouble(NS, p.getUniqueId(), "divinity", g.divinity);
    }

    /** 신성 등급 — God.tier 위임 (config tiers 사용). */
    public String tierOf(God g) {
        return g.tier(plugin.getConfig().getMapList("tiers"));
    }
}
