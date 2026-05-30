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
 * 천계 성장: 신앙·임무·기도 중심. 사냥 효율 매우 낮음 (살생 금기).
 *
 * 신성 단계:
 *   0~99   필멸자급
 *   100~499 천인견습
 *   500~1999 천인
 *   2000+   대천사 후보 — 마계 NPC가 호의 -50, 신성 NPC 호의 +50
 *
 * 금기:
 *   - 살생: 매 monsterKill마다 1% 확률로 monk_oath_break 저주
 *   - 거짓말: NPC 호의도 떨어뜨림 (외부 hook)
 *
 * 외부 호출:
 *   onPrayer(p, npcId, quality): 기도 효율 (정도에 따라 신성 누적)
 *   onSinAccrued(p, weight): 살생/거짓말 등 죄목 누적 → 천벌 위험
 */
public final class HeavenGrowth implements GrowthStrategy {

    /** uuid → 살생 죄목 점수 */
    private final Map<UUID, Double> sinScore = new ConcurrentHashMap<>();
    /** uuid → 마지막 신성 단계 적용 */
    private final Map<UUID, Integer> appliedStage = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.HEAVEN; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        // 천계는 사냥 효율 매우 낮음
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.HEAVEN_KI, 0.1, "kill");
        // 살생 죄목 누적 (악마 처치는 죄 없음 — mobLevel >= 100 = 악마 가정)
        if (mobLevel < 100) {
            onSinAccrued(p, 0.5);
            if (Rand.chance(0.01)) {
                applyOathBreak(p);
            }
        }
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.HEAVEN_KI, 6 * weight, "duty");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DIVINITY, 0.5 * weight, "duty");
        checkStage(p);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.HEAVEN_KI, 4 * quality, "prayer");
        if (quality > 0.9) {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.DIVINITY, 0.3, "prayer-perfect");
        }
        checkStage(p);
    }

    /** 외부 호출 — NPC 신전 기도. */
    public void onPrayer(Player p, String npcId, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.HEAVEN_KI, 10 * quality, "npc-prayer");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DIVINITY, 1.0 * quality, "npc-prayer");
        Msg.send(p, "&b" + npcId + " 신전에 기도 — 천기 +" + (int)(10*quality)
                + " 신성 +" + String.format("%.1f", quality));
    }

    /** 외부 호출 — 죄목 누적. */
    public void onSinAccrued(Player p, double weight) {
        double cur = sinScore.merge(p.getUniqueId(), weight, Double::sum);
        if (cur >= 100) {
            // 천벌 자동 발동
            sinScore.put(p.getUniqueId(), 0.0);
            applyDivinePunishment(p);
        }
    }

    private void applyOathBreak(Player p) {
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object effects = cp.getClass().getMethod("effects").invoke(cp);
                effects.getClass().getMethod("apply", Player.class, String.class)
                        .invoke(effects, p, "monk_oath_break");
            }
        } catch (Throwable ignored) {}
    }

    private void applyDivinePunishment(Player p) {
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object effects = cp.getClass().getMethod("effects").invoke(cp);
                effects.getClass().getMethod("apply", Player.class, String.class)
                        .invoke(effects, p, "divine_punishment");
            }
        } catch (Throwable ignored) {}
        Bukkit.broadcastMessage("§e§l[천벌] §f" + p.getName() + " §7가 천벌을 받았다 — 죄목 100 누적!");
    }

    private void checkStage(Player p) {
        double ki = RebornCore.get().api().getStat(p.getUniqueId(), StatType.HEAVEN_KI);
        int stage = ki >= 2000 ? 3 : ki >= 500 ? 2 : ki >= 100 ? 1 : 0;
        int prev = appliedStage.getOrDefault(p.getUniqueId(), -1);
        if (stage > prev) {
            appliedStage.put(p.getUniqueId(), stage);
            String label = switch (stage) {
                case 1 -> "천인견습";
                case 2 -> "천인";
                case 3 -> "대천사 후보";
                default -> "필멸자";
            };
            Bukkit.broadcastMessage("§e§l[천계] §f" + p.getName() + " §7가 §6" + label + " §7에 도달!");
            if (stage == 3) {
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.DIVINITY, 50, "heaven-stage-3");
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.CHARISMA, 20, "heaven-stage-3");
            }
        }
    }

    public double sinOf(UUID p) { return sinScore.getOrDefault(p, 0.0); }
    public int stageOf(UUID p) { return appliedStage.getOrDefault(p, 0); }
}
