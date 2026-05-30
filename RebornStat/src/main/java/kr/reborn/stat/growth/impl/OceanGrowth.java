package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 해양제국 성장: 바다에서만, 다이빙·해전·진주 채집.
 *
 * 외부 호출:
 *   onDive(p, depth): 깊이별 해양력 누적 (최대 100m)
 *   onPearl(p, quality): 진주 채집 — quality 1~5
 *   onSeaBattle(p, victimLevel): 해상 전투 승리
 *   onShipCapture(p): 적선 나포 — 명성 + 해양력
 *
 * 마일스톤:
 *   해양력 1000: 항해사 단계 (+ WATER_BREATHING 영구)
 *   해양력 5000: 선장 단계
 *   해양력 20000: 해왕 후보 단계
 */
public final class OceanGrowth implements GrowthStrategy {

    /** uuid → 다이빙 누적 미터 */
    private final Map<UUID, Double> diveMeters = new ConcurrentHashMap<>();
    /** uuid → 진주 수 */
    private final Map<UUID, Integer> pearls = new ConcurrentHashMap<>();
    /** uuid → 적선 나포 횟수 */
    private final Map<UUID, Integer> captures = new ConcurrentHashMap<>();
    /** uuid → 적용된 단계 */
    private final Map<UUID, Integer> stage = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.OCEAN; }

    private boolean atSea(Player p) {
        Material m = p.getLocation().getBlock().getType();
        return m == Material.WATER || p.isInWater();
    }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        if (!atSea(p)) return;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 1.0, "sea-kill");
        checkStage(p);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 5 * weight, "voyage");
        checkStage(p);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        if (!atSea(p)) {
            Msg.warn(p, "해양력 수련은 바다 위에서만 가능합니다.");
            return;
        }
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 4 * quality, "tide-meditate");
        // 다이빙 보너스
        double y = p.getLocation().getY();
        if (y < 30) {
            // 깊은 곳일수록 보너스
            double depthBonus = (30 - y) * 0.5 * quality;
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.OCEAN_POWER, depthBonus, "deep-meditate");
        }
        checkStage(p);
    }

    public void onDive(Player p, double meters) {
        double cur = diveMeters.merge(p.getUniqueId(), meters, Double::sum);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, meters * 0.3, "dive");
        if (cur >= 1000 && cur - meters < 1000) {
            Msg.send(p, "&b1000m 다이빙 누적 — 해양력 +500");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.OCEAN_POWER, 500, "dive-1000m");
        }
    }

    public void onPearl(Player p, int quality) {
        int n = pearls.merge(p.getUniqueId(), 1, Integer::sum);
        double bonus = 10 * quality;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, bonus, "pearl");
        Msg.send(p, "&b진주 채집 (Q" + quality + ") — 해양력 +" + bonus + " §7(총 " + n + "개)");
        if (n % 10 == 0) {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.LUCK, 5, "pearl-10");
            Msg.send(p, "&6진주 10개 — 행운 +5");
        }
    }

    public void onSeaBattle(Player p, double victimLevel) {
        if (!atSea(p)) return;
        double bonus = 5 + victimLevel * 0.1;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, bonus, "sea-battle");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.STRENGTH, 0.5, "sea-battle");
    }

    public void onShipCapture(Player p) {
        int n = captures.merge(p.getUniqueId(), 1, Integer::sum);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 100, "ship-capture");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.CHARISMA, 3, "ship-capture");
        Bukkit.broadcastMessage("§3§l[해전] §f" + p.getName()
                + " §7가 적선을 나포 §6(총 " + n + "척) §7- 해양력 +100");
    }

    private void checkStage(Player p) {
        double ki = RebornCore.get().api().getStat(p.getUniqueId(), StatType.OCEAN_POWER);
        int newStage = ki >= 20000 ? 3 : ki >= 5000 ? 2 : ki >= 1000 ? 1 : 0;
        int prev = stage.getOrDefault(p.getUniqueId(), -1);
        if (newStage > prev) {
            stage.put(p.getUniqueId(), newStage);
            String label = switch (newStage) {
                case 1 -> "항해사";
                case 2 -> "선장";
                case 3 -> "해왕 후보";
                default -> "범인";
            };
            Bukkit.broadcastMessage("§3§l[해양] §f" + p.getName() + " §7가 §6" + label + " §7단계!");
            if (newStage == 1) {
                try {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.WATER_BREATHING,
                            Integer.MAX_VALUE, 0, true, false));
                } catch (Throwable ignored) {}
            }
            if (newStage == 3) {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.OCEAN_POWER, 5000, "sea-king-candidate");
            }
        }
    }

    public int stageOf(UUID p) { return stage.getOrDefault(p, 0); }
    public int pearlsOf(UUID p) { return pearls.getOrDefault(p, 0); }
}
