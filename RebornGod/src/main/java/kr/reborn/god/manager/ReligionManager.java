package kr.reborn.god.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import kr.reborn.god.data.Religion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 교단 관리자 — 등록·신도·신앙 흐름의 진입점.
 *
 * config의 religions: 섹션을 자동 로드해 NPC 교단 등록.
 * 플레이어 신은 /god religion create로 새 교단 창설.
 * NPC가 종교를 창시하면(WorldImpact) registerNpc()로 등록.
 */
public final class ReligionManager {

    private static final String NS = "RebornGod.religion";

    private final RebornGod plugin;
    /** 교단 맵 — FaithEngine tick + /god pray + create 동시 호출 가능. */
    private final Map<String, Religion> religions = new java.util.concurrent.ConcurrentHashMap<>();

    public ReligionManager(RebornGod plugin) {
        this.plugin = plugin;
        loadFromConfig();
        loadFromKV();  // NPC·플레이어 창설 교단 복원
    }

    private void loadFromKV() {
        try {
            var data = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            for (var e : data.entrySet()) {
                Religion r = decode(e.getKey(), e.getValue());
                if (r != null) religions.put(r.id, r);
            }
        } catch (Throwable ignored) {}
    }

    /** 외부 호출 — 모든 동적 교단(KV에 저장된) 일괄 저장. */
    public void saveAll() {
        for (Religion r : religions.values()) {
            // config로 로드된 교단은 KV에 저장 안 함 — config가 권위
            if (plugin.getConfig().getConfigurationSection("religions." + r.id) != null) continue;
            persist(r);
        }
    }

    private void persist(Religion r) {
        try {
            String enc = r.name + "|" + r.godIdentifier + "|"
                    + (r.doctrine == null ? "" : r.doctrine.replace('|', ' ')) + "|"
                    + r.faith + "|" + r.npcFollowerCount + "|"
                    + (r.antiReligion == null ? "" : r.antiReligion) + "|"
                    + (r.forbidden ? "1" : "0") + "|" + (r.protective ? "1" : "0") + "|"
                    + r.followers.stream().map(java.util.UUID::toString)
                            .collect(java.util.stream.Collectors.joining(",")) + "|"
                    + String.join(",", r.allyReligions);
            kr.reborn.core.RebornCore.get().kv().put(NS, null, r.id, enc);
        } catch (Throwable ignored) {}
    }

    private Religion decode(String id, String value) {
        try {
            String[] parts = value.split("\\|", -1);
            if (parts.length < 10) return null;
            Religion r = new Religion(id, parts[0], parts[1], parts[2],
                    (int) Double.parseDouble(parts[3]));
            r.faith = Double.parseDouble(parts[3]);
            r.npcFollowerCount = Integer.parseInt(parts[4]);
            r.antiReligion = parts[5];
            r.forbidden = "1".equals(parts[6]);
            r.protective = "1".equals(parts[7]);
            for (String u : parts[8].split(",")) {
                if (u.isEmpty()) continue;
                try { r.followers.add(java.util.UUID.fromString(u)); } catch (Throwable ignored) {}
            }
            for (String a : parts[9].split(",")) {
                if (!a.isEmpty()) r.allyReligions.add(a);
            }
            return r;
        } catch (Throwable t) { return null; }
    }

    private void loadFromConfig() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("religions");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Religion r = new Religion(id,
                    s.getString("name", id),
                    s.getString("god", ""),
                    s.getString("doctrine", ""),
                    s.getInt("base-faith", 1000));
            r.forbidden  = s.getBoolean("forbidden", false);
            r.protective = s.getBoolean("protective", false);
            String anti = s.getString("anti", "");
            if (!anti.isEmpty()) r.antiReligion = anti;
            religions.put(id, r);
        }
        plugin.getLogger().info("교단 " + religions.size() + "개 로드");
    }

    /** 플레이어 신이 새 교단 창설. */
    public boolean create(Player p, String id, String name) {
        God g = plugin.gods().of(p.getUniqueId());
        if (g == null) { Msg.error(p, "신만 교단을 만들 수 있다."); return false; }
        if (religions.containsKey(id)) { Msg.error(p, "이미 존재하는 교단 ID."); return false; }
        Religion r = new Religion(id, name, g.identifier(), "신 " + p.getName() + "의 가르침", 100);
        religions.put(id, r);
        persist(r);
        Msg.send(p, "&6교단 창설: " + name);
        return true;
    }

    /** NPC가 만든 교단 자동 등록 (RebornNpcWorldImpactEvent.RELIGION_FOUNDED). */
    public Religion registerNpc(String id, String name, String npcId) {
        Religion existing = religions.get(id);
        if (existing != null) return existing;
        Religion r = new Religion(id, name, "npc:" + npcId,
                "NPC " + npcId + "이(가) 세운 교단", 500);
        religions.put(id, r);
        persist(r);
        return r;
    }

    /** 플레이어 기도 — FaithEngine으로 위임. */
    public boolean pray(Player p, String religionId) {
        return plugin.faith().pray(p, religionId);
    }

    public Religion get(String id) { return religions.get(id); }
    public Collection<Religion> all() { return religions.values(); }
}
