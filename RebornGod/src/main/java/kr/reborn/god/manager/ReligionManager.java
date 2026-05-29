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

    private final RebornGod plugin;
    private final Map<String, Religion> religions = new HashMap<>();

    public ReligionManager(RebornGod plugin) {
        this.plugin = plugin;
        loadFromConfig();
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
        return r;
    }

    /** 플레이어 기도 — FaithEngine으로 위임. */
    public boolean pray(Player p, String religionId) {
        return plugin.faith().pray(p, religionId);
    }

    public Religion get(String id) { return religions.get(id); }
    public Collection<Religion> all() { return religions.values(); }
}
