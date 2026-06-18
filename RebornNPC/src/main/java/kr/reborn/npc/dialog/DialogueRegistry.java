package kr.reborn.npc.dialog;

import kr.reborn.npc.RebornNPC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 대화 트리 정의 로더. dialogues.yml에서 모든 Dialogue 인스턴스 생성.
 *
 * YAML 구조:
 *   dialogues:
 *     common_villager:
 *       root: greet
 *       nodes:
 *         greet:
 *           text: "안녕하시오, 외지인."
 *           choices:
 *             - { label: "안녕!", next: "friendly_response" }
 *             - { label: "...", next: "silent_response" }
 *         friendly_response:
 *           text: "반갑소. 무엇이 필요한가?"
 *           actions: [ "addFavor:5" ]
 */
public final class DialogueRegistry {

    private final RebornNPC plugin;
    private final Map<String, Dialogue> dialogues = new HashMap<>();
    /** NPC 직업 → 디폴트 대화 ID 매핑 */
    private final Map<String, String> jobDefaults = new HashMap<>();

    public DialogueRegistry(RebornNPC plugin) {
        this.plugin = plugin;
        load();
        seedJobDefaults();
    }

    private void load() {
        File f = new File(plugin.getDataFolder(), "dialogues.yml");
        if (!f.exists()) {
            plugin.saveResource("dialogues.yml", false);
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection root = y.getConfigurationSection("dialogues");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection ds = root.getConfigurationSection(id);
            if (ds == null) continue;
            Dialogue d = parse(id, ds);
            if (d != null) dialogues.put(id, d);
        }
        plugin.getLogger().info("Dialogue 로드: " + dialogues.size() + "종");
    }

    private Dialogue parse(String id, ConfigurationSection ds) {
        String rootNodeId = ds.getString("root", "greet");
        Dialogue d = new Dialogue(id, rootNodeId);
        ConfigurationSection nodesSec = ds.getConfigurationSection("nodes");
        if (nodesSec == null) return d;
        for (String nodeId : nodesSec.getKeys(false)) {
            ConfigurationSection ns = nodesSec.getConfigurationSection(nodeId);
            if (ns == null) continue;
            DialogueNode n = new DialogueNode(nodeId, ns.getString("text", "..."));
            n.autoNext = ns.getString("auto_next", null);
            n.fallbackNodeId = ns.getString("fallback", null);
            n.actions.addAll(ns.getStringList("actions"));
            n.conditions.addAll(ns.getStringList("conditions"));
            for (Map<?, ?> raw : ns.getMapList("choices")) {
                String label = String.valueOf(raw.getOrDefault("label", "..."));
                String next = String.valueOf(raw.getOrDefault("next", ""));
                if (next.isEmpty() || "null".equals(next)) next = null;
                DialogueChoice c = new DialogueChoice(label, next);
                Object cond = raw.get("conditions");
                if (cond instanceof java.util.List<?> l) {
                    for (Object o : l) c.conditions.add(String.valueOf(o));
                }
                Object act = raw.get("actions");
                if (act instanceof java.util.List<?> l) {
                    for (Object o : l) c.actions.add(String.valueOf(o));
                }
                n.choices.add(c);
            }
            d.nodes.put(nodeId, n);
        }
        return d;
    }

    /** 디폴트 직업 → 대화 매핑. */
    private void seedJobDefaults() {
        jobDefaults.put("VILLAGER", "common_villager");
        jobDefaults.put("FARMER", "common_villager");
        jobDefaults.put("MERCHANT", "common_merchant");
        jobDefaults.put("BLACKSMITH", "common_blacksmith");
        jobDefaults.put("GUARD", "common_guard");
        jobDefaults.put("KING", "court_king");
        jobDefaults.put("EMPEROR", "court_king");
        jobDefaults.put("DEMON_LORD", "demon_lord");
        jobDefaults.put("CULT_MASTER", "cult_master");
        jobDefaults.put("ALLIANCE_MASTER", "wulin_alliance_master");
        jobDefaults.put("DRAGON_LORD", "dragon_lord");
        jobDefaults.put("HUNTER", "common_hunter");
        jobDefaults.put("HERMIT", "common_hermit");
        jobDefaults.put("PRIEST", "common_priest");
        // 유명 NPC 직업 — 기존 트리 중 가장 가까운 페르소나로 매핑.
        // 성직 계열
        jobDefaults.put("ABBOT", "common_priest");
        jobDefaults.put("ARCHANGEL", "common_priest");
        jobDefaults.put("PRIEST_KING", "common_priest");
        // 은둔·도가 계열
        jobDefaults.put("TAOIST", "common_hermit");
        jobDefaults.put("ELDER", "common_hermit");
        // 통치자·궁정 계열 → 왕 트리
        jobDefaults.put("QUEEN", "court_king");
        jobDefaults.put("PRINCESS", "court_king");
        jobDefaults.put("CEO", "court_king");
        jobDefaults.put("ADMIRAL", "court_king");
        jobDefaults.put("COUNCIL", "court_king");
        // 강대 보스·반체제 → 마왕 트리 (위압형)
        jobDefaults.put("BOSS", "demon_lord");
        jobDefaults.put("WARLORD", "demon_lord");
        jobDefaults.put("PIRATE_KING", "demon_lord");
        jobDefaults.put("FALLEN_ANGEL", "demon_lord");
        jobDefaults.put("ELEMENTAL_KING", "demon_lord");
        jobDefaults.put("GHOST_CAPTAIN", "demon_lord");
        jobDefaults.put("PRIMORDIAL", "demon_lord");
        // 지도자급 → 무림맹주 트리 (학자·지도자형)
        jobDefaults.put("ARCHMAGE", "wulin_alliance_master");
        jobDefaults.put("MASTER", "wulin_alliance_master");
        jobDefaults.put("PALACE_MASTER", "wulin_alliance_master");
        jobDefaults.put("ASSOC_HEAD", "wulin_alliance_master");
        jobDefaults.put("GUILD_MASTER", "wulin_alliance_master");
        // 용족 장로
        jobDefaults.put("ELDER_DRAGON", "dragon_lord");
        // 정보 수집형 → 헌터
        jobDefaults.put("HACKER", "common_hunter");
    }

    public Dialogue get(String id) { return dialogues.get(id); }

    public Dialogue resolveForJob(String job) {
        String id = jobDefaults.get(job);
        return id == null ? dialogues.get("common_villager") : dialogues.get(id);
    }

    public Map<String, Dialogue> all() { return dialogues; }
}
