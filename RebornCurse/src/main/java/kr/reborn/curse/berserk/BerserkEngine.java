package kr.reborn.curse.berserk;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.ActiveEffect;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 광폭화 실행 엔진.
 *
 * 발동 시:
 *   1. 의식 흐려짐 (BLINDNESS + CONFUSION)
 *   2. 데미지 +50% (INCREASE_DAMAGE 2)
 *   3. 방어 -25% (WEAKNESS 0) — 명중률 떨어진 대신 한 방이 무거움
 *   4. 매 2초마다 가장 가까운 적(또는 같은 클랜 외 플레이어) 1체를 자동 공격
 *   5. 종료 시 후유증 1분 (HUNGER + 정신력 -10)
 *
 * 광폭화 중 PvP 데미지는 config의 berserk.pk-crime-reduction-percent% 만큼 PK 책임 감소
 * (RebornStat 평판 시스템 hook).
 */
public final class BerserkEngine {

    private final RebornCurse plugin;
    private final Map<UUID, Long> aggressionTickAt = new ConcurrentHashMap<>();
    /** uuid → 광폭화 종료 시각. 이 시간까지 PvP 면책. */
    private final Map<UUID, Long> berserkUntil = new ConcurrentHashMap<>();

    public BerserkEngine(RebornCurse plugin) {
        this.plugin = plugin;
        // 2초마다 광폭화 중인 플레이어가 가장 가까운 대상을 공격
        RebornCore.get().scheduler().runTimer(this::aggressionTick, 40L, 40L);
    }

    public void start(Player p, ActiveEffect a) {
        int duration = plugin.getConfig().getInt("berserk.duration-seconds", 10);
        a.berserkActive = true;
        a.berserkUntil = System.currentTimeMillis() + duration * 1000L;
        berserkUntil.put(p.getUniqueId(), a.berserkUntil);
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration * 20, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration * 20, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration * 20, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 20, 0));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
            p.getWorld().spawnParticle(Particle.SQUID_INK, p.getLocation(), 100, 1, 1, 1);
        } catch (Throwable ignored) {}
        Bukkit.broadcastMessage(Msg.PREFIX + Msg.c("&c&l[광폭화] &f" + p.getName()
                + " &7의 의식이 무너졌다 — 닿는 모든 것을 공격한다."));
    }

    /** 광폭화 종료 후유증. */
    public void endWithBacklash(Player p) {
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 1200, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1200, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 1200, 1));
        } catch (Throwable ignored) {}
        try {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    kr.reborn.core.data.StatType.MENTAL, -10, "BERSERK_BACKLASH");
        } catch (Throwable ignored) {}
        Msg.send(p, "&7광폭화 후유증 — 정신력 -10, 1분간 약화.");
    }

    private void aggressionTick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            Long until = berserkUntil.get(id);
            if (until == null) continue;
            if (now > until) {
                berserkUntil.remove(id);
                endWithBacklash(p);
                continue;
            }
            // 가장 가까운 적대 대상에게 자동 공격 시뮬레이션
            LivingEntity target = findNearestTarget(p);
            if (target == null) continue;
            RebornCore.get().scheduler().runEntityTask(target, () -> {
                try { target.damage(6.0 + Math.random() * 4.0, p); } catch (Throwable ignored) {}
            });
            try { p.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 0.5, 0.5); }
            catch (Throwable ignored) {}
        }
    }

    private LivingEntity findNearestTarget(Player p) {
        LivingEntity nearest = null;
        double best = 8 * 8;
        for (Entity e : p.getNearbyEntities(8, 8, 8)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e == p) continue;
            double d = e.getLocation().distanceSquared(p.getLocation());
            if (d < best) { best = d; nearest = le; }
        }
        return nearest;
    }

    /** 광폭화 중인 플레이어에 대한 PK 책임 감소 비율 (0~1). */
    public double pkReductionFor(UUID p) {
        Long until = berserkUntil.get(p);
        if (until == null || System.currentTimeMillis() > until) return 0;
        return plugin.getConfig().getInt("berserk.pk-crime-reduction-percent", 50) / 100.0;
    }

    public boolean isBerserk(UUID p) {
        Long until = berserkUntil.get(p);
        return until != null && System.currentTimeMillis() < until;
    }

    public long remainingMs(UUID p) {
        Long until = berserkUntil.get(p);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }
}
