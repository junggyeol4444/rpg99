package kr.reborn.quest.engine;

import kr.reborn.quest.RebornQuest;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QuestRegistry {

    private final RebornQuest plugin;
    private final Map<String, Quest> defs = new java.util.concurrent.ConcurrentHashMap<>();

    public QuestRegistry(RebornQuest p) { this.plugin = p; }

    public void load() {
        defs.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("quests");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Quest q = new Quest(id,
                    s.getString("name", id),
                    s.getString("type", "KILL"),
                    s.getString("target", ""),
                    s.getInt("amount", 1),
                    s.getString("world", ""),
                    s.getString("linked", null),
                    s.getConfigurationSection("rewards") == null
                            ? Map.of() : s.getConfigurationSection("rewards").getValues(false));
            List<Map<?, ?>> phases = s.getMapList("phases");
            for (Map<?, ?> ph : phases) {
                q.phases.add(new Quest.Phase(
                        String.valueOf(ph.get("name")),
                        String.valueOf(ph.get("target")),
                        ph.get("amount") == null ? 1 : ((Number) ph.get("amount")).intValue(),
                        ph.get("target-id") == null ? "" : String.valueOf(ph.get("target-id"))));
            }
            defs.put(id, q);
        }
    }

    public Quest get(String id) { return defs.get(id); }
    public java.util.Collection<Quest> all() { return defs.values(); }

    /** 런타임 동적 등록 — 자기 생성 퀘스트 / NPC 자율 의뢰 / WorldAI 발행. */
    public void register(Quest q) { defs.put(q.id, q); }
    public boolean has(String id) { return defs.containsKey(id); }
}
