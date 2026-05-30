package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 선계 성장: 양생·천겁·단도(丹道)·도반(道伴).
 *
 * - 운기 quality 0.9+ → 천기 누적 ×2, 영석 발견 5% 확률
 * - 영석 발견 시 IMMORTAL_KI += 100, 천기 누적 ↑
 * - 천기 누적 1000마다 → 천겁 자동 진입 (성공 시 +500 IMMORTAL_KI, 실패 시 도법기 손실)
 * - 단약: 자하단(+1000 KI 1회), 옥액환(+MENTAL 50), 환령단(부활 1회)
 * - 도반: 다인 합동 명상 시 +30%, 도반과 함께 천겁 진입 시 성공률 +20%
 */
public final class ImmortalGrowth implements GrowthStrategy {

    /** uuid → 누적 천기 (다음 천겁까지) */
    private final Map<UUID, Double> celestialQi = new ConcurrentHashMap<>();
    /** uuid → 천겁 통과 횟수 (선인 단계) */
    private final Map<UUID, Integer> tribulationCount = new ConcurrentHashMap<>();
    /** 단약 사용 카운트 */
    private final Map<UUID, Map<String, Integer>> pillUsage = new ConcurrentHashMap<>();
    /** uuid → 환령단 사용 여부 (1회 한정) */
    private final Map<UUID, Boolean> revivalUsed = new ConcurrentHashMap<>();

    private static final double TRIBULATION_THRESHOLD = 1000.0;

    @Override public WorldKey world() { return WorldKey.IMMORTAL; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 0.5, "kill");
        // 마교 NPC 처치는 추가 천기
        accumulateCelestialQi(p, 0.5);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 5 * weight, "quest");
        accumulateCelestialQi(p, 5 * weight);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 6 * quality, "meditate");
        double celestial = quality >= 0.9 ? 12.0 : 6.0;
        accumulateCelestialQi(p, celestial * quality);
        // 영석 발견
        if (quality >= 0.9 && Rand.chance(0.05)) {
            discoverSpiritStone(p);
        }
        // 천기 1000 누적 시 천겁 진입
        if (celestialQi.getOrDefault(p.getUniqueId(), 0.0) >= TRIBULATION_THRESHOLD) {
            enterTribulation(p);
        }
    }

    /** 도반 합동 양생. */
    public void onDaoCompanionMeditate(Player p, int companionCount) {
        double mult = 1.0 + companionCount * 0.3;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.IMMORTAL_KI, 10 * mult, "dao-meditate");
        accumulateCelestialQi(p, 8 * mult);
        Msg.send(p, "&b도반 합동 양생 — 천기 ×" + String.format("%.1f", mult));
    }

    public void consumePill(Player p, String pillId) {
        Map<String, Integer> usage = pillUsage.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        int count = usage.merge(pillId, 1, Integer::sum);
        double penalty = Math.max(0.3, 1.0 - (count - 1) * 0.1);

        switch (pillId) {
            case "purple_summit_pill" -> { // 자하단
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.IMMORTAL_KI, 1000 * penalty, "pill:" + pillId);
                Msg.send(p, "&5자하단 — 선기 +" + (int)(1000*penalty));
            }
            case "jade_liquid_pill" -> { // 옥액환
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.MENTAL, 50 * penalty, "pill:" + pillId);
                Msg.send(p, "&b옥액환 — 정신 +" + (int)(50*penalty));
            }
            case "soul_return_pill" -> { // 환령단 — 1회 한정 부활
                if (Boolean.TRUE.equals(revivalUsed.get(p.getUniqueId()))) {
                    Msg.warn(p, "환령단은 1회 한정 — 이미 사용했다.");
                    return;
                }
                revivalUsed.put(p.getUniqueId(), true);
                Msg.send(p, "&e환령단 — 다음 사망 시 자동 부활.");
            }
            case "celestial_essence_pill" -> { // 천령단 — 천기 누적
                accumulateCelestialQi(p, 200 * penalty);
                Msg.send(p, "&6천령단 — 천기 +" + (int)(200*penalty));
            }
            case "longevity_pill" -> { // 양생단 — 행운 + 회복
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.LUCK, 5, "pill:longevity");
                try { p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 8)); }
                catch (Throwable ignored) {}
            }
            default -> {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.IMMORTAL_KI, 100 * penalty, "pill:unknown");
            }
        }
    }

    /** 사망 시 부활 시도. */
    public boolean tryRevival(Player p) {
        if (!Boolean.TRUE.equals(revivalUsed.get(p.getUniqueId()))) return false;
        revivalUsed.put(p.getUniqueId(), false); // 소비
        try {
            p.setHealth(p.getMaxHealth());
            p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 200, 1, 2, 1);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            Bukkit.broadcastMessage("§b§l[환령] §f" + p.getName() + " §7가 환령단으로 부활!");
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private void accumulateCelestialQi(Player p, double v) {
        celestialQi.merge(p.getUniqueId(), v, Double::sum);
    }

    private void discoverSpiritStone(Player p) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.IMMORTAL_KI, 100, "spirit-stone");
        accumulateCelestialQi(p, 50);
        Msg.send(p, "&b&l영석 발견! &7선기 +100, 천기 +50");
        try { p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation(), 50, 1, 1, 1); }
        catch (Throwable ignored) {}
    }

    private void enterTribulation(Player p) {
        celestialQi.put(p.getUniqueId(), 0.0);
        int curLvl = tribulationCount.getOrDefault(p.getUniqueId(), 0);
        // 천겁 성공률 = max(20%, 90% - 5%×curLvl)
        double successRate = Math.max(0.2, 0.9 - curLvl * 0.05);
        Bukkit.broadcastMessage("§e§l[천겁] §f" + p.getName()
                + " §7가 제 " + (curLvl + 1) + "차 천겁에 진입했다!");
        try {
            p.getWorld().strikeLightningEffect(p.getLocation());
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
            p.damage(p.getMaxHealth() * 0.7); // 큰 데미지
        } catch (Throwable ignored) {}
        if (Rand.chance(successRate)) {
            int newLvl = tribulationCount.merge(p.getUniqueId(), 1, Integer::sum);
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.IMMORTAL_KI, 500, "tribulation-success");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.MENTAL, 20, "tribulation-success");
            Bukkit.broadcastMessage("§e§l[천겁 성공] §f" + p.getName()
                    + " §7가 제 " + newLvl + "차 천겁을 넘었다! §6선기 +500 정신 +20");
        } else {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.IMMORTAL_KI, -200, "tribulation-fail");
            // 선계 천벌 저주 부여
            try {
                var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                if (cp != null) {
                    Object effects = cp.getClass().getMethod("effects").invoke(cp);
                    effects.getClass().getMethod("apply", Player.class, String.class)
                            .invoke(effects, p, "immortal_heavenly_punishment");
                }
            } catch (Throwable ignored) {}
            Bukkit.broadcastMessage("§c§l[천겁 실패] §f" + p.getName()
                    + " §7가 천겁에 떨어졌다 — 선기 -200, 천벌 저주.");
        }
    }

    public int tribulationLevelOf(UUID p) { return tribulationCount.getOrDefault(p, 0); }
    public double tribulationProgress(UUID p) { return celestialQi.getOrDefault(p, 0.0); }
}
