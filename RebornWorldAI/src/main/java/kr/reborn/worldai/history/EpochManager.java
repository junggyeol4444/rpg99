package kr.reborn.worldai.history;

import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.RebornWorldAI;
import org.bukkit.Bukkit;

import java.util.EnumMap;
import java.util.Map;

/**
 * 세계 시대 (epoch) 판정 — 최근 200개 사건에서 추세를 보고 5단계 분류.
 *
 *   GOLDEN_AGE  — 축제 다수, 전쟁/재해 없음, 영향력 안정
 *   PEACE_ERA   — 평시, 약간의 외교 활동
 *   TENSION_ERA — 전쟁 임박, 봉기·이주 증가
 *   WAR_ERA     — 전쟁 다수, 재해 빈번
 *   DARK_AGE    — 재해 + 전쟁 + 봉기 모두 다수, 보스 강림 등
 *
 * 시대 전환 시 broadcast + history 기록.
 */
public final class EpochManager {

    public enum Epoch { GOLDEN_AGE, PEACE_ERA, TENSION_ERA, WAR_ERA, DARK_AGE }

    private final RebornWorldAI plugin;
    private final Map<WorldKey, Epoch> currentEpoch = new EnumMap<>(WorldKey.class);

    public EpochManager(RebornWorldAI plugin) {
        this.plugin = plugin;
        for (WorldKey w : WorldKey.values()) currentEpoch.put(w, Epoch.PEACE_ERA);
    }

    public void cycle(WorldKey world) {
        WorldHistory h = plugin.history();
        int wars = h.countRecent(world, WorldHistory.EventKind.WAR_START, 50);
        int disasters = h.countRecent(world, WorldHistory.EventKind.DISASTER, 50);
        int festivals = h.countRecent(world, WorldHistory.EventKind.FESTIVAL, 50);
        int revolts = h.countRecent(world, WorldHistory.EventKind.REVOLT, 50);
        int bosses = h.countRecent(world, WorldHistory.EventKind.BOSS_DESCENT, 50);

        Epoch next;
        if (wars >= 3 && disasters >= 2 && (revolts + bosses) >= 2) {
            next = Epoch.DARK_AGE;
        } else if (wars >= 2 || disasters >= 3) {
            next = Epoch.WAR_ERA;
        } else if (wars >= 1 || revolts >= 1) {
            next = Epoch.TENSION_ERA;
        } else if (festivals >= 3 && wars == 0 && disasters == 0) {
            next = Epoch.GOLDEN_AGE;
        } else {
            next = Epoch.PEACE_ERA;
        }

        Epoch prev = currentEpoch.get(world);
        if (prev != next) {
            currentEpoch.put(world, next);
            String label = label(next);
            Bukkit.broadcastMessage("§6§l[" + world + " 시대 전환] §f"
                    + label(prev) + " §7→ §6" + label);
            plugin.history().record(world, WorldHistory.EventKind.SPECIAL, "시대 전환: " + label);
            applyEpochBuff(world, next);
        }
    }

    /** 시대에 따라 세계 단위 스탯 보정 적용 (RebornStat reflection). */
    private void applyEpochBuff(WorldKey world, Epoch epoch) {
        try {
            var ai = plugin.of(world);
            if (ai == null) return;
            switch (epoch) {
                case GOLDEN_AGE -> {
                    ai.state().stability = Math.min(100, ai.state().stability + 20);
                    ai.state().inflation = Math.max(50, ai.state().inflation - 20);
                }
                case PEACE_ERA -> {
                    ai.state().tension = Math.max(0, ai.state().tension - 10);
                }
                case TENSION_ERA -> {
                    ai.state().tension = Math.min(100, ai.state().tension + 10);
                }
                case WAR_ERA -> {
                    ai.state().tension = Math.min(100, ai.state().tension + 25);
                    ai.state().stability = Math.max(0, ai.state().stability - 15);
                }
                case DARK_AGE -> {
                    ai.state().tension = 90;
                    ai.state().stability = 10;
                    ai.state().inflation = Math.min(300, ai.state().inflation + 50);
                    ai.state().mobBalance = 2.0;
                }
            }
        } catch (Throwable ignored) {}
    }

    public Epoch of(WorldKey w) {
        return currentEpoch.getOrDefault(w, Epoch.PEACE_ERA);
    }

    public String label(Epoch e) {
        switch (e) {
            case GOLDEN_AGE: return "황금기";
            case PEACE_ERA: return "평화기";
            case TENSION_ERA: return "긴장기";
            case WAR_ERA: return "전쟁기";
            case DARK_AGE: return "암흑기";
        }
        return e.name();
    }
}
