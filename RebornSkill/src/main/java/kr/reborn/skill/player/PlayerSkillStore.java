package kr.reborn.skill.player;

import kr.reborn.core.RebornCore;
import kr.reborn.skill.RebornSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 플레이어별 보유 스킬 + 숙련도 + 슬롯. KV 영속화. */
public final class PlayerSkillStore {

    private static final String NS = "RebornSkill.player";
    private static final String NS_SLOT = "RebornSkill.slot";

    private final RebornSkill plugin;
    private final ConcurrentHashMap<UUID, Map<String, Integer>> proficiency = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String[]> slots = new ConcurrentHashMap<>();
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public PlayerSkillStore(RebornSkill p) { this.plugin = p; }

    private void ensureLoaded(UUID id) {
        if (loaded.add(id)) {
            // 보유 스킬 + 숙련도
            var profAll = RebornCore.get().kv().loadAll(NS, id);
            Map<String, Integer> m = new HashMap<>();
            for (var e : profAll.entrySet()) {
                try { m.put(e.getKey(), Integer.parseInt(e.getValue())); } catch (Throwable ignored) {}
            }
            if (!m.isEmpty()) proficiency.put(id, m);
            // 슬롯 — "slots" 키에 csv (빈 칸은 빈 문자열)
            String slotCsv = RebornCore.get().kv().get(NS_SLOT, id, "slots");
            if (slotCsv != null) {
                int size = plugin.getConfig().getInt("slots", 8);
                String[] arr = new String[size];
                String[] parts = slotCsv.split(",", -1);
                for (int i = 0; i < arr.length && i < parts.length; i++) {
                    arr[i] = parts[i].isEmpty() ? null : parts[i];
                }
                slots.put(id, arr);
            }
        }
    }

    public boolean has(UUID id, String skill) {
        ensureLoaded(id);
        return proficiency.getOrDefault(id, Map.of()).containsKey(skill);
    }

    public void learn(UUID id, String skill) {
        ensureLoaded(id);
        proficiency.computeIfAbsent(id, x -> new HashMap<>()).putIfAbsent(skill, 0);
        RebornCore.get().kv().putInt(NS, id, skill,
                proficiency.get(id).getOrDefault(skill, 0));
    }

    public Set<String> owned(UUID id) {
        ensureLoaded(id);
        return proficiency.getOrDefault(id, Map.of()).keySet();
    }

    public int prof(UUID id, String skill) {
        ensureLoaded(id);
        return proficiency.getOrDefault(id, Map.of()).getOrDefault(skill, 0);
    }

    public void addProf(UUID id, String skill, int delta) {
        ensureLoaded(id);
        Map<String, Integer> m = proficiency.computeIfAbsent(id, x -> new HashMap<>());
        int next = m.merge(skill, delta, (a, b) -> Math.max(0, Math.min(100, a + b)));
        RebornCore.get().kv().putInt(NS, id, skill, next);
    }

    public String[] slots(UUID id) {
        ensureLoaded(id);
        return slots.computeIfAbsent(id, x -> new String[plugin.getConfig().getInt("slots", 8)]);
    }

    public void equip(UUID id, int slot, String skillId) {
        ensureLoaded(id);
        String[] arr = slots(id);
        if (slot < 0 || slot >= arr.length) return;
        arr[slot] = skillId;
        // csv 인코딩 — null/빈 칸은 빈 문자열
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            if (arr[i] != null) sb.append(arr[i]);
        }
        RebornCore.get().kv().put(NS_SLOT, id, "slots", sb.toString());
    }
}
