package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 요계 성장: 밤 3배, 낮 0.5배, 보름달 10배. 변신·꼬리.
 *
 * 변신 단계 (꼬리 개수): 1꼬리 → 9꼬리
 * 요기 1000마다 꼬리 +1, 9꼬리 = 구미호 단계 (절대자 후보)
 *
 * 외부 호출:
 *   onTransform(p): 변신 모드 — 30초간 STR ×2 + AGI ×1.5 (요기 소모)
 *   onMoonRitual(p): 보름달 의식 — 요기 ×10 누적
 */
public final class YokaiGrowth implements GrowthStrategy {

    /** uuid → 꼬리 단계 */
    private final Map<UUID, Integer> tails = new ConcurrentHashMap<>();
    /** uuid → 변신 중 (1회 사용 후 쿨다운) */
    private final Map<UUID, Long> transformCooldown = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.YOKAI; }

    private double timeMult(Player p) {
        long t = p.getWorld().getTime();
        boolean night = t >= 13000 && t <= 23000;
        boolean fullMoon = p.getWorld().getFullTime() / 24000L % 8 == 0;
        var c = RebornStat.get().getConfig();
        if (fullMoon && night) return c.getDouble("growth.yokai.full-moon-mult", 10.0);
        return night ? c.getDouble("growth.yokai.night-mult", 3.0)
                     : c.getDouble("growth.yokai.day-mult", 0.5);
    }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        double mult = timeMult(p);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.YOKAI_KI, 1.0 * mult, "yokai-kill");
        checkTails(p);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.YOKAI_KI, 5 * weight * timeMult(p), "quest");
        checkTails(p);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.YOKAI_KI, 3 * quality * timeMult(p), "meditate");
        checkTails(p);
    }

    /** 외부 호출 — 변신. */
    public boolean onTransform(Player p) {
        long cd = transformCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (System.currentTimeMillis() < cd) {
            Msg.warn(p, "변신 쿨다운 " + ((cd - System.currentTimeMillis()) / 1000) + "초 남음.");
            return false;
        }
        double ki = RebornCore.get().api().getStat(p.getUniqueId(), StatType.YOKAI_KI);
        if (ki < 100) {
            Msg.error(p, "요기 부족 (필요 100, 보유 " + (int)ki + ")");
            return false;
        }
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.YOKAI_KI, -100, "transform");
        transformCooldown.put(p.getUniqueId(), System.currentTimeMillis() + 120_000L);
        // 30초 변신 효과
        try {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.INCREASE_DAMAGE, 600, 1));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 600, 1));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.JUMP, 600, 1));
        } catch (Throwable ignored) {}
        Msg.send(p, "&5변신! 30초간 STR ×2, AGI ×1.5");
        return true;
    }

    /** 외부 호출 — 보름달 의식. */
    public void onMoonRitual(Player p) {
        double bonus = 500 * timeMult(p);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.YOKAI_KI, bonus, "moon-ritual");
        Msg.send(p, "&5보름달 의식 — 요기 +" + (int)bonus);
        checkTails(p);
    }

    private void checkTails(Player p) {
        double ki = RebornCore.get().api().getStat(p.getUniqueId(), StatType.YOKAI_KI);
        int targetTails = Math.min(9, (int)(ki / 1000));
        int curTails = tails.getOrDefault(p.getUniqueId(), 0);
        if (targetTails > curTails) {
            tails.put(p.getUniqueId(), targetTails);
            // 꼬리 보너스
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.AGILITY, 10 * (targetTails - curTails), "tail-bonus");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.CHARM, 5 * (targetTails - curTails), "tail-bonus");
            Bukkit.broadcastMessage("§5§l[요계] §f" + p.getName() + " §7가 §6"
                    + targetTails + "꼬리 §7에 도달!");
            if (targetTails == 9) {
                Bukkit.broadcastMessage("§5§l✦ 구미호 강림! §f" + p.getName()
                        + " §7요계 절대자 단계 §6");
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.YOKAI_KI, 5000, "9-tails");
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.CHARISMA, 50, "9-tails");
            }
        }
    }

    public int tailsOf(UUID p) { return tails.getOrDefault(p, 0); }
}
