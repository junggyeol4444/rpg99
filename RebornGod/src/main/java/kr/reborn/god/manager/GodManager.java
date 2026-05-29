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

    private final RebornGod plugin;
    private final Map<UUID, God> playerGods = new HashMap<>();
    private final Map<String, God> npcGods = new HashMap<>();

    public GodManager(RebornGod p) {
        this.plugin = p;
        loadNpcGods();
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
        RebornCore.get().api().setStat(p.getUniqueId(), StatType.DIVINITY, 100);
        Bukkit.broadcastMessage("§6§l[신격] §f" + p.getName() + "이(가) 신의 자리에 올랐다! §7(시작 신성 100)");
        return true;
    }

    public void addDivinity(Player p, double delta) {
        God g = playerGods.get(p.getUniqueId());
        if (g == null) return;
        g.divinity = Math.max(0, g.divinity + delta);
        RebornCore.get().api().setStat(p.getUniqueId(), StatType.DIVINITY, g.divinity);
    }

    /** 신성 등급 — God.tier 위임 (config tiers 사용). */
    public String tierOf(God g) {
        return g.tier(plugin.getConfig().getMapList("tiers"));
    }
}
