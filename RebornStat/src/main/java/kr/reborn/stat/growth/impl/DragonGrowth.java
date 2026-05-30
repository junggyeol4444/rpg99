package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 드래곤계 성장: 사냥보다 잠·보물·세대(나이)가 핵심.
 *
 * - 잠(meditate) = 회복 + 용력 누적 + 나이 증가
 * - 보물 축적 = 매 100 보물마다 용력 +50 (외부 호출 onHoardItem)
 * - 나이 100세마다 단계 상승: 어린 용(0) → 청룡(100) → 장룡(500) → 고룡(2000) → 신룡(5000)
 * - 각 단계마다 영구 STR/ENDURANCE 보너스
 * - 다른 용 처치 시 그 용의 용력 30% 흡수 (외부 onDragonKill)
 */
public final class DragonGrowth implements GrowthStrategy {

    /** uuid → 보물 누적량 (gold equivalent) */
    private final java.util.Map<UUID, Long> hoard = new ConcurrentHashMap<>();
    /** uuid → 마지막 단계 (보너스 적용 시점 기준) */
    private final java.util.Map<UUID, Integer> tierApplied = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.DRAGON; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, 0.8, "dragon-kill");
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, 6 * weight, "quest");
        // 나이 +1 per quest
        ageUp(p, d, 1);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        // 잠 = 회복 + 성장 + 나이 증가
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.DRAGON_POWER, 4 * quality, "sleep");
        try { p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 5 * quality)); }
        catch (Throwable ignored) {}
        ageUp(p, d, (int) (quality * 5));
    }

    /** 외부 호출 — 보물 획득. */
    public void onHoardItem(Player p, long goldValue) {
        long cur = hoard.merge(p.getUniqueId(), goldValue, Long::sum);
        // 100마다 +50 용력
        if (cur / 100 > (cur - goldValue) / 100) {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.DRAGON_POWER, 50, "hoard-100");
            Msg.send(p, "&6보물 축적 — 용력 +50 (총 " + cur + " gold)");
        }
    }

    /** 외부 호출 — 다른 용 처치. */
    public void onDragonKill(Player killer, PlayerData kData, double victimDragonPower) {
        double absorb = victimDragonPower * 0.30;
        RebornCore.get().api().addStat(killer.getUniqueId(),
                StatType.DRAGON_POWER, absorb, "dragon-kill-absorb");
        Bukkit.broadcastMessage("§c§l[용살] §f" + killer.getName()
                + " §7가 다른 용의 용력 §6" + (int)absorb + " §7을(를) 흡수했다!");
    }

    /** 나이 증가 + 단계 보너스 적용. */
    public void ageUp(Player p, PlayerData d, int years) {
        if (years <= 0) return;
        // RebornCore PlayerData에 dragonAge 누적 — reflection으로 갱신
        try {
            int cur = d.dragonAge();
            int next = cur + years;
            // PlayerData에 setter가 없으므로 field 리플렉션
            java.lang.reflect.Field f = d.getClass().getDeclaredField("dragonAge");
            f.setAccessible(true);
            f.setInt(d, next);
            checkTierMilestone(p, next);
        } catch (Throwable ignored) {}
    }

    private void checkTierMilestone(Player p, int age) {
        int newTier = tierOf(age);
        int prev = tierApplied.getOrDefault(p.getUniqueId(), -1);
        if (newTier > prev) {
            tierApplied.put(p.getUniqueId(), newTier);
            applyTierBonus(p, newTier);
        }
    }

    private int tierOf(int age) {
        if (age >= 5000) return 4;  // 신룡
        if (age >= 2000) return 3;  // 고룡
        if (age >= 500)  return 2;  // 장룡
        if (age >= 100)  return 1;  // 청룡
        return 0;                    // 어린 용
    }

    private String labelOf(int tier) {
        switch (tier) {
            case 0: return "어린 용";
            case 1: return "청룡";
            case 2: return "장룡";
            case 3: return "고룡";
            case 4: return "신룡";
        }
        return "용";
    }

    private void applyTierBonus(Player p, int tier) {
        if (tier == 0) return;
        double str = tier * 50;
        double end = tier * 50;
        double dragonPower = tier * 100;
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.STRENGTH, str, "dragon-age-tier");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.ENDURANCE, end, "dragon-age-tier");
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER, dragonPower, "dragon-age-tier");
        Bukkit.broadcastMessage("§6§l[용계] §f" + p.getName()
                + " §7가 §6" + labelOf(tier) + " §7단계에 도달! §6STR +" + (int)str
                + " END +" + (int)end + " 용력 +" + (int)dragonPower);
    }

    public long hoardOf(UUID p) { return hoard.getOrDefault(p, 0L); }
    public int tierOf(UUID p) { return tierApplied.getOrDefault(p, 0); }
}
