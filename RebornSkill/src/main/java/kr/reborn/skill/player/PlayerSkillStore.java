package kr.reborn.skill.player;

import kr.reborn.skill.RebornSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 플레이어별 보유 스킬 + 숙련도 + 슬롯. 인메모리. */
public final class PlayerSkillStore {

    private final RebornSkill plugin;
    private final ConcurrentHashMap<UUID, Map<String, Integer>> proficiency = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String[]> slots = new ConcurrentHashMap<>();

    public PlayerSkillStore(RebornSkill p) { this.plugin = p; }

    public boolean has(UUID id, String skill) {
        return proficiency.getOrDefault(id, Map.of()).containsKey(skill);
    }

    public void learn(UUID id, String skill) {
        proficiency.computeIfAbsent(id, x -> new HashMap<>()).putIfAbsent(skill, 0);
    }

    public Set<String> owned(UUID id) {
        return proficiency.getOrDefault(id, Map.of()).keySet();
    }

    public int prof(UUID id, String skill) {
        return proficiency.getOrDefault(id, Map.of()).getOrDefault(skill, 0);
    }

    public void addProf(UUID id, String skill, int delta) {
        proficiency.computeIfAbsent(id, x -> new HashMap<>())
                .merge(skill, delta, (a, b) -> Math.max(0, Math.min(100, a + b)));
    }

    public String[] slots(UUID id) {
        return slots.computeIfAbsent(id, x -> new String[plugin.getConfig().getInt("slots", 8)]);
    }

    public void equip(UUID id, int slot, String skillId) {
        String[] arr = slots(id);
        if (slot < 0 || slot >= arr.length) return;
        arr[slot] = skillId;
    }
}
