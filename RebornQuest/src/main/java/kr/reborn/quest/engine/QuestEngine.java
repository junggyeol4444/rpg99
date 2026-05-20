package kr.reborn.quest.engine;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.event.RebornQuestCompleteEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.quest.RebornQuest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestEngine {

    private final RebornQuest plugin;
    private final ConcurrentHashMap<UUID, Map<String, Progress>> active = new ConcurrentHashMap<>();

    public QuestEngine(RebornQuest p) { this.plugin = p; }

    public boolean accept(Player p, String questId) {
        Quest q = plugin.registry().get(questId);
        if (q == null) return false;
        active.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>())
                .put(questId, new Progress(0, 0));
        Msg.send(p, "&a퀘스트 수락: " + q.name);
        return true;
    }

    public void onKill(Player p, String mobId) {
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map == null) return;
        for (var e : map.entrySet()) {
            Quest q = plugin.registry().get(e.getKey());
            if (q == null) continue;
            if (!"KILL".equals(q.type)) continue;
            if (q.target != null && !q.target.equalsIgnoreCase(mobId) && !q.target.isEmpty()) continue;
            e.getValue().count++;
            if (e.getValue().count >= q.amount) complete(p, q);
        }
    }

    public void complete(Player p, Quest q) {
        active.computeIfPresent(p.getUniqueId(), (k, v) -> { v.remove(q.id); return v; });
        Msg.send(p, "&6퀘스트 완료: " + q.name);
        // 보상
        applyRewards(p, q);
        Bukkit.getPluginManager().callEvent(new RebornQuestCompleteEvent(p, q.id));
        // 연동 퀘스트
        if (q.linkedQuestId != null) {
            for (Player op : Bukkit.getOnlinePlayers()) {
                if (op != p) {
                    var d = RebornCore.get().api().getPlayerData(op.getUniqueId());
                    Quest linked = plugin.registry().get(q.linkedQuestId);
                    if (linked != null && linked.world != null && !linked.world.isEmpty()
                            && d.worldKey().name().equalsIgnoreCase(linked.world)) {
                        accept(op, q.linkedQuestId);
                    }
                }
            }
        }
    }

    private void applyRewards(Player p, Quest q) {
        Object stats = q.rewards.get("stat");
        if (stats instanceof Map<?, ?> m) {
            for (var e : m.entrySet()) {
                try {
                    StatType t = StatType.valueOf(String.valueOf(e.getKey()));
                    double v = ((Number) e.getValue()).doubleValue();
                    RebornCore.get().api().addStat(p.getUniqueId(), t, v, "quest:" + q.id);
                } catch (Exception ignored) {}
            }
        }
        Object title = q.rewards.get("title");
        if (title != null) Msg.send(p, "&6칭호 획득: " + title);
    }

    public static final class Progress {
        public int count;
        public int phase;
        public Progress(int c, int p) { this.count = c; this.phase = p; }
    }

    public Map<String, Progress> activeFor(UUID id) { return active.getOrDefault(id, Map.of()); }
}
