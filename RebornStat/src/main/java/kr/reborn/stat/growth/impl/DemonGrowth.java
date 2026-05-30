package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 마계 성장: 마기·영혼 흡수가 핵심.
 *
 * 마기 누적 단계:
 *   0~999    범마: 안정
 *   1000~4999 마인: 침식 1% 확률 (정신 -1)
 *   5000~19999 마군: 침식 3% 확률 + 외형 변화 (GLOWING)
 *   20000+    마장: 영구 demon_erosion 저주 자동 부여, 단 STR ×2
 *
 * 외부 호출:
 *   onSoulAbsorb(p, soulPower) — 큰 영혼 흡수 (보스 처치 등)
 *   stabilize(p) — 마기 안정화 의식 (NPC 의뢰)
 */
public final class DemonGrowth implements GrowthStrategy {

    /** uuid → 영혼 흡수 카운트 (살생) */
    private final Map<UUID, Integer> soulAbsorbCount = new ConcurrentHashMap<>();
    /** uuid → 마지막 외형 단계 적용 */
    private final Map<UUID, Integer> appliedStage = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.DEMON; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DEMON_KI, 1.0 + Math.min(2.0, mobLevel / 30.0), "soul-absorb");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, 0.3, "kill");
        soulAbsorbCount.merge(p.getUniqueId(), 1, Integer::sum);
        checkErosion(p, d);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DEMON_KI, 4 * weight, "quest");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DEMON_KI, 3 * quality, "meditate");
        // 낮은 quality = 침식 가속
        if (quality < 0.5 && Rand.chance(0.05)) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -2, "demon-erosion-meditate");
            Msg.warn(p, "&8마기가 정신을 잠식한다 — 정신 -2");
        }
        checkErosion(p, d);
    }

    /** 외부 호출 — 큰 영혼 흡수 (보스 처치 등). */
    public void onSoulAbsorb(Player p, double soulPower) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DEMON_KI, soulPower, "soul-absorb-boss");
        Msg.send(p, "&5영혼 흡수 — 마기 +" + (int)soulPower);
        soulAbsorbCount.merge(p.getUniqueId(), 5, Integer::sum);
    }

    /** 외부 호출 — 마기 안정화 의식 (마기 -30%, 정신 +5). */
    public void stabilize(Player p) {
        double cur = RebornCore.get().api().getStat(p.getUniqueId(), StatType.DEMON_KI);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DEMON_KI, -cur * 0.3, "demon-stabilize");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 5, "demon-stabilize");
        Msg.send(p, "&b마기 안정화 — 마기 -30%, 정신 +5");
    }

    private void checkErosion(Player p, PlayerData d) {
        double ki = RebornCore.get().api().getStat(p.getUniqueId(), StatType.DEMON_KI);
        int stage = stageOf(ki);
        int prev = appliedStage.getOrDefault(p.getUniqueId(), -1);
        if (stage > prev) {
            appliedStage.put(p.getUniqueId(), stage);
            applyStageEffects(p, stage);
        }
        // 침식 확률 적용
        double erosionChance = switch (stage) {
            case 1 -> 0.01;
            case 2 -> 0.03;
            case 3 -> 0.06;
            default -> 0;
        };
        if (erosionChance > 0 && Rand.chance(erosionChance)) {
            try {
                var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                if (cp != null) {
                    Object effects = cp.getClass().getMethod("effects").invoke(cp);
                    effects.getClass().getMethod("apply", Player.class, String.class)
                            .invoke(effects, p, "demon_erosion");
                }
            } catch (Throwable ignored) {}
        }
    }

    private int stageOf(double ki) {
        if (ki >= 20000) return 3;
        if (ki >= 5000) return 2;
        if (ki >= 1000) return 1;
        return 0;
    }

    private void applyStageEffects(Player p, int stage) {
        String label = switch (stage) {
            case 1 -> "마인";
            case 2 -> "마군";
            case 3 -> "마장";
            default -> "범마";
        };
        Bukkit.broadcastMessage("§5§l[마계] §f" + p.getName() + " §7가 §c" + label + " §7단계에 도달!");
        if (stage == 3) {
            // STR ×2 보너스 + 영구 마기 침식
            double curStr = RebornCore.get().api().getStat(p.getUniqueId(), StatType.STRENGTH);
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, curStr, "demon-stage-3");
            try {
                var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                if (cp != null) {
                    Object effects = cp.getClass().getMethod("effects").invoke(cp);
                    effects.getClass().getMethod("apply", Player.class, String.class)
                            .invoke(effects, p, "demon_erosion");
                }
            } catch (Throwable ignored) {}
        }
    }

    public int stageOf(UUID p) {
        return appliedStage.getOrDefault(p, 0);
    }
}
