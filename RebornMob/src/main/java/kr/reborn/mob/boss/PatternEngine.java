package kr.reborn.mob.boss;

import kr.reborn.core.RebornCore;
import kr.reborn.mob.def.MobDef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * 보스 패턴 실행 엔진. 각 BossPattern enum 값에 대응되는 실제 효과 구현.
 *
 * 호출: BossManager가 tick 또는 phase 진입 시 trigger().
 */
public final class PatternEngine {

    public void trigger(LivingEntity boss, MobDef def, BossPattern pattern) {
        Location l = boss.getLocation();
        switch (pattern) {
            case AOE_SLOW -> applyToNearby(boss, 15, p ->
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2)));
            case AOE_WITHER -> applyToNearby(boss, 12, p -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0));
            });
            case AOE_BURST -> applyToNearby(boss, 10, p -> p.damage(def.damage, boss));
            case AOE_EXPLOSION -> {
                try {
                    boss.getWorld().createExplosion(l, 4.0f, false, false);
                } catch (Throwable ignored) {}
            }
            case METEOR_RAIN -> {
                for (int i = 0; i < 8; i++) {
                    double dx = (Math.random() - 0.5) * 30;
                    double dz = (Math.random() - 0.5) * 30;
                    final Location dest = l.clone().add(dx, 0, dz);
                    RebornCore.get().scheduler().runTaskLater(() -> {
                        try { boss.getWorld().createExplosion(dest, 3.0f, true, false); }
                        catch (Throwable ignored) {}
                    }, 20L * i / 2);
                }
            }
            case BEAM_LINE -> {
                Player target = nearestPlayer(boss, 30);
                if (target == null) return;
                Location origin = l.clone().add(0, 1.5, 0);
                org.bukkit.util.Vector dir = target.getLocation().toVector().subtract(origin.toVector())
                        .normalize();
                for (int i = 0; i < 30; i++) {
                    Location p = origin.clone().add(dir.clone().multiply(i));
                    try { boss.getWorld().spawnParticle(Particle.FLAME, p, 10, 0.2, 0.2, 0.2); }
                    catch (Throwable ignored) {}
                    for (Entity e : boss.getWorld().getNearbyEntities(p, 1.5, 1.5, 1.5)) {
                        if (e instanceof LivingEntity le && e != boss) {
                            try { le.damage(def.damage * 0.5, boss); } catch (Throwable ignored) {}
                        }
                    }
                }
            }
            case SELF_HEAL -> {
                try {
                    boss.setHealth(Math.min(boss.getMaxHealth(), boss.getHealth() + boss.getMaxHealth() * 0.15));
                    boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation(), 30, 1, 1, 1);
                } catch (Throwable ignored) {}
            }
            case SELF_BUFF -> {
                try {
                    boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1));
                    boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                } catch (Throwable ignored) {}
            }
            case INVULNERABLE_BRIEF -> {
                try {
                    boss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 5));
                } catch (Throwable ignored) {}
            }
            case SUMMON_MINIONS -> {
                for (int i = 0; i < 3; i++) {
                    try {
                        Location pos = l.clone().add(Math.cos(i * 2.094) * 3, 0, Math.sin(i * 2.094) * 3);
                        Entity minion = boss.getWorld().spawnEntity(pos, boss.getType());
                        if (minion instanceof LivingEntity le) {
                            le.setCustomName("§7부하 (" + def.name + ")");
                            var attr = le.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                            if (attr != null) { attr.setBaseValue(20); le.setHealth(20); }
                        }
                    } catch (Throwable ignored) {}
                }
            }
            case TELEPORT_TO_TARGET -> {
                Player target = nearestPlayer(boss, 40);
                if (target == null) return;
                Location tp = target.getLocation().clone().add(
                        target.getLocation().getDirection().multiply(-2));
                try { boss.teleport(tp); } catch (Throwable ignored) {}
            }
            case PULL_PLAYERS -> applyToNearby(boss, 20, p -> {
                org.bukkit.util.Vector dir = l.toVector().subtract(p.getLocation().toVector())
                        .normalize().multiply(2);
                p.setVelocity(dir);
            });
            case KNOCKBACK -> applyToNearby(boss, 10, p -> {
                org.bukkit.util.Vector dir = p.getLocation().toVector().subtract(l.toVector())
                        .normalize().multiply(3).setY(0.8);
                p.setVelocity(dir);
            });
            case ENRAGE -> {
                try {
                    boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
                            Integer.MAX_VALUE, 2, true, false));
                    boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                            Integer.MAX_VALUE, 1, true, false));
                    boss.getWorld().playSound(l, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
                } catch (Throwable ignored) {}
            }
            case FREEZE_AOE -> applyToNearby(boss, 8, p -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 7));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 2));
                try { p.getWorld().spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 30, 1, 1, 1); }
                catch (Throwable ignored) {}
            });
            case POISON_FOG -> applyToNearby(boss, 12, p -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
            });
            case BLIND_AOE -> applyToNearby(boss, 15, p ->
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)));
            case TELEPORT_RANDOM -> {
                double dx = (Math.random() - 0.5) * 20;
                double dz = (Math.random() - 0.5) * 20;
                try { boss.teleport(l.clone().add(dx, 0, dz)); } catch (Throwable ignored) {}
            }
            case OPRESSIVE_AURA -> applyToNearby(boss, 30, p -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 5));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 3));
            });
            case DIMENSIONAL_SLASH -> {
                Player t = nearestPlayer(boss, 30);
                if (t == null) return;
                org.bukkit.util.Vector dir = t.getLocation().toVector().subtract(l.toVector()).normalize();
                for (int i = 0; i < 30; i++) {
                    Location p = l.clone().add(dir.clone().multiply(i));
                    try { boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p, 3, 1, 1, 1); }
                    catch (Throwable ignored) {}
                    for (Entity e : boss.getWorld().getNearbyEntities(p, 2, 2, 2)) {
                        if (e instanceof LivingEntity le && e != boss) {
                            try { le.damage(def.damage * 0.6, boss); } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        }
        // 패턴별 고유 마무리 입자/사운드
        renderPatternSignature(boss, l, pattern);
    }

    /** 각 패턴마다 다른 마무리 시각·청각 효과 — 기존엔 모두 SMOKE_LARGE였음. */
    private void renderPatternSignature(LivingEntity boss, Location l, BossPattern pattern) {
        try {
            switch (pattern) {
                case AOE_SLOW -> {
                    boss.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 50, 4, 1, 4);
                    boss.getWorld().playSound(l, org.bukkit.Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                }
                case AOE_WITHER -> {
                    boss.getWorld().spawnParticle(Particle.SQUID_INK, l, 60, 3, 2, 3);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
                }
                case AOE_BURST -> {
                    boss.getWorld().spawnParticle(Particle.CRIT, l, 80, 4, 2, 4);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.8f);
                }
                case AOE_EXPLOSION -> {
                    boss.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, l, 5, 1, 1, 1);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                }
                case METEOR_RAIN -> {
                    boss.getWorld().spawnParticle(Particle.LAVA, l, 100, 8, 5, 8);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.7f);
                }
                case BEAM_LINE -> {
                    boss.getWorld().spawnParticle(Particle.FLAME, l, 30, 0.5, 0.5, 0.5);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ITEM_TRIDENT_THUNDER, 1.5f, 1.5f);
                }
                case SELF_HEAL -> {
                    boss.getWorld().spawnParticle(Particle.HEART, l, 30, 1.5, 2, 1.5);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
                case SELF_BUFF -> {
                    boss.getWorld().spawnParticle(Particle.END_ROD, l, 40, 1, 2, 1);
                    boss.getWorld().playSound(l, org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.2f);
                }
                case INVULNERABLE_BRIEF -> {
                    boss.getWorld().spawnParticle(Particle.SPELL_INSTANT, l, 60, 1.5, 2, 1.5);
                    boss.getWorld().playSound(l, org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.8f);
                }
                case SUMMON_MINIONS -> {
                    boss.getWorld().spawnParticle(Particle.PORTAL, l, 100, 3, 2, 3);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);
                }
                case TELEPORT_TO_TARGET -> {
                    boss.getWorld().spawnParticle(Particle.PORTAL, l, 80, 0.5, 2, 0.5);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                }
                case PULL_PLAYERS -> {
                    boss.getWorld().spawnParticle(Particle.PORTAL, l, 60, 4, 1, 4);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 2.0f, 0.5f);
                }
                case KNOCKBACK -> {
                    boss.getWorld().spawnParticle(Particle.CLOUD, l, 60, 3, 1, 3);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.7f);
                }
                case ENRAGE -> {
                    boss.getWorld().spawnParticle(Particle.LAVA, l, 80, 2, 2, 2);
                    boss.getWorld().spawnParticle(Particle.FLAME, l, 60, 2, 2, 2);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                }
                case FREEZE_AOE -> {
                    boss.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 60, 3, 1, 3);
                    boss.getWorld().playSound(l, org.bukkit.Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                }
                case POISON_FOG -> {
                    boss.getWorld().spawnParticle(Particle.SPELL_WITCH, l, 80, 4, 2, 4);
                    boss.getWorld().spawnParticle(Particle.SLIME, l, 30, 3, 1, 3);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_SPIDER_HURT, 1.5f, 0.7f);
                }
                case BLIND_AOE -> {
                    boss.getWorld().spawnParticle(Particle.SQUID_INK, l, 50, 4, 2, 4);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_PHANTOM_AMBIENT, 1.5f, 0.5f);
                }
                case TELEPORT_RANDOM -> {
                    boss.getWorld().spawnParticle(Particle.PORTAL, l, 60, 1, 2, 1);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                }
                case OPRESSIVE_AURA -> {
                    boss.getWorld().spawnParticle(Particle.SOUL, l, 80, 5, 2, 5);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
                }
                case DIMENSIONAL_SLASH -> {
                    boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, l, 30, 1, 1, 1);
                    boss.getWorld().spawnParticle(Particle.PORTAL, l, 80, 5, 1, 5);
                    boss.getWorld().playSound(l, org.bukkit.Sound.ITEM_TRIDENT_THUNDER, 2.0f, 0.5f);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void applyToNearby(LivingEntity boss, double r, java.util.function.Consumer<Player> fn) {
        for (Entity e : boss.getNearbyEntities(r, r, r)) {
            if (e instanceof Player p) {
                try { fn.accept(p); } catch (Throwable ignored) {}
            }
        }
    }

    private Player nearestPlayer(LivingEntity boss, double r) {
        Player nearest = null;
        double bestSq = r * r;
        for (Entity e : boss.getNearbyEntities(r, r, r)) {
            if (e instanceof Player p) {
                double d = p.getLocation().distanceSquared(boss.getLocation());
                if (d < bestSq) { bestSq = d; nearest = p; }
            }
        }
        return nearest;
    }

    public List<BossPhase> defaultPhases(MobDef def) {
        // 보스 페이즈 기본값 — 3페이즈. config에서 오버라이드 가능 (추후).
        List<BossPhase> list = new ArrayList<>();
        BossPhase p1 = new BossPhase(1, 100, 1.0, 1.0, "초기");
        p1.patterns.add(BossPattern.AOE_SLOW);
        p1.patterns.add(BossPattern.AOE_BURST);
        p1.patterns.add(BossPattern.SELF_BUFF);
        list.add(p1);

        BossPhase p2 = new BossPhase(2, 60, 1.2, 0.9, "본격");
        p2.patterns.add(BossPattern.AOE_WITHER);
        p2.patterns.add(BossPattern.METEOR_RAIN);
        p2.patterns.add(BossPattern.SUMMON_MINIONS);
        p2.patterns.add(BossPattern.PULL_PLAYERS);
        p2.patterns.add(BossPattern.BEAM_LINE);
        list.add(p2);

        BossPhase p3 = new BossPhase(3, 30, 1.5, 0.8, "분노");
        p3.patterns.add(BossPattern.ENRAGE);
        p3.patterns.add(BossPattern.AOE_EXPLOSION);
        p3.patterns.add(BossPattern.DIMENSIONAL_SLASH);
        p3.patterns.add(BossPattern.OPRESSIVE_AURA);
        p3.patterns.add(BossPattern.SELF_HEAL);
        list.add(p3);

        return list;
    }
}
