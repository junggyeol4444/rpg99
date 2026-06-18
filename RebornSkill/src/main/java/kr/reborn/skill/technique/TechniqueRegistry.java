package kr.reborn.skill.technique;

import kr.reborn.skill.RebornSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비급 → 초식 목록 로드 및 시전 시 초식 선택.
 *
 * techniques.yml: skill_id 별로 초식 배열.
 * 시전 시 플레이어별 사이클 인덱스를 진행시켜 초식이 순환된다 (예: 독고구검 9회 시전 = 9개 초식 한 바퀴).
 * 숙련도(prof)에 따라 상위 초식이 해금된다 — prof 10마다 1개씩 추가, 최소 1개.
 */
public final class TechniqueRegistry {

    private final RebornSkill plugin;
    private final Map<String, List<Technique>> bySkill = new HashMap<>();
    /** playerUUID → (skillId → 다음 초식 인덱스). */
    private final Map<UUID, Map<String, Integer>> cycle = new ConcurrentHashMap<>();

    public TechniqueRegistry(RebornSkill plugin) { this.plugin = plugin; }

    public void load() {
        bySkill.clear();
        // jar 내장본을 dataFolder에 풀어둔 뒤 거기서 로드 (기본/사용자 수정본 모두 지원)
        plugin.saveResource("techniques.yml", false);
        File f = new File(plugin.getDataFolder(), "techniques.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection root = y.getConfigurationSection("techniques");
        if (root == null) return;
        for (String skillId : root.getKeys(false)) {
            List<Map<?, ?>> arr = root.getMapList(skillId);
            if (arr.isEmpty()) continue;
            List<Technique> list = new ArrayList<>();
            for (Map<?, ?> raw : arr) {
                String name = String.valueOf(raw.getOrDefault("name", "초식"));
                double mult = raw.get("mult") == null ? 1.0 : ((Number) raw.get("mult")).doubleValue();
                String elem = raw.get("element") == null ? "" : String.valueOf(raw.get("element"));
                String desc = raw.get("desc") == null ? "" : String.valueOf(raw.get("desc"));
                list.add(new Technique(name, mult, elem, desc));
            }
            bySkill.put(skillId, list);
        }
        int totalTech = bySkill.values().stream().mapToInt(List::size).sum();
        plugin.getLogger().info("초식 로드: " + bySkill.size() + "개 비급, 총 " + totalTech + " 초식");
    }

    /** 비급의 모든 초식. */
    public List<Technique> of(String skillId) {
        return bySkill.getOrDefault(skillId, List.of());
    }

    /** 숙련도에 따른 해금 초식 — 최소 1개, prof 10마다 +1 (cap = 비급 총 초식 수). */
    public List<Technique> unlocked(String skillId, int prof) {
        List<Technique> all = of(skillId);
        if (all.isEmpty()) return List.of();
        int n = Math.min(all.size(), Math.max(1, 1 + prof / 10));
        return all.subList(0, n);
    }

    /** 시전 시 다음 초식 선택 + 사이클 진행. 비급에 초식 데이터가 없으면 null. */
    public Technique nextFor(UUID player, String skillId, int prof) {
        List<Technique> avail = unlocked(skillId, prof);
        if (avail.isEmpty()) return null;
        Map<String, Integer> m = cycle.computeIfAbsent(player, x -> new HashMap<>());
        int idx = m.getOrDefault(skillId, 0) % avail.size();
        m.put(skillId, idx + 1);
        return avail.get(idx);
    }

    public int total() { return bySkill.values().stream().mapToInt(List::size).sum(); }
    public int skillCount() { return bySkill.size(); }
}
