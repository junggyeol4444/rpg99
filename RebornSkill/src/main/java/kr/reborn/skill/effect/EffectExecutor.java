package kr.reborn.skill.effect;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.def.SkillDef;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Tameable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 스킬 종류(SkillType)별로 실제 효과를 실행한다.
 *
 * 모든 효과는 시전자(Player)의 명령 스레드에서 호출되므로 엔티티/월드 접근이 안전.
 * 지연 효과(DOT·소환 소멸·버프 해제)만 스케줄러 사용.
 * 데미지는 Element 상성 배수·상태이상을 통과한다.
 */
public final class EffectExecutor {

    private final RebornSkill plugin;

    public EffectExecutor(RebornSkill plugin) { this.plugin = plugin; }

    public void execute(Player caster, SkillDef def, double power) {
        switch (def.type) {
            case MELEE:      melee(caster, def, power); break;
            case PROJECTILE: projectile(caster, def, power); break;
            case AOE:        aoe(caster, def, power); break;
            case HEAL:       heal(caster, def, power); break;
            case BUFF:
            case UTILITY:    buff(caster, def); break;
            case DASH:       dash(caster, def); break;
            case BLINK:      blink(caster, def); break;
            case DOT:        dot(caster, def, power); break;
            case CHAIN:      chain(caster, def, power); break;
            case SUMMON:     summon(caster, def); break;
            default:         melee(caster, def, power); break;
        }
    }

    // ───────────────────────── 근접 ─────────────────────────

    private void melee(Player p, SkillDef def, double power) {
        double range = def.range > 0 ? def.range : 8;
        LivingEntity t = coneTarget(p, range);
        if (t == null) { Msg.warn(p, "정면에 대상이 없다."); return; }
        damageTarget(p, t, power, def.element);
        t.getWorld().spawnParticle(Particle.SWEEP_ATTACK, t.getLocation().add(0, 1, 0), 3);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    // ───────────────────────── 투사체 ─────────────────────────

    private void projectile(Player p, SkillDef def, double power) {
        Vector dir = p.getEyeLocation().getDirection();
        double speed = def.projectileSpeed > 0 ? def.projectileSpeed : 1.6;
        String el = def.element == null ? "" : def.element.toUpperCase();
        Projectile proj;
        if (el.equals("FIRE")) {
            SmallFireball fb = p.launchProjectile(SmallFireball.class);
            fb.setIsIncendiary(false);  // 블록 화재 방지
            fb.setYield(0f);            // 폭발 블록 피해 없음
            proj = fb;                  // 가속도 기반으로 자동 비행
        } else {
            Snowball sb = p.launchProjectile(Snowball.class);
            sb.setVelocity(dir.multiply(speed));
            proj = sb;
        }
        proj.setMetadata("reborn_skill", new FixedMetadataValue(plugin, def.id));
        proj.setMetadata("reborn_dmg", new FixedMetadataValue(plugin, power));
        proj.setMetadata("reborn_elem", new FixedMetadataValue(plugin, el));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.2f);
    }

    // ───────────────────────── 광역 ─────────────────────────

    private void aoe(Player p, SkillDef def, double power) {
        double r = def.radius > 0 ? def.radius : 5;
        Location center = aimLocation(p, def.range > 0 ? def.range : r + 6);
        if (center.getWorld() == null) return;
        int hits = 0;
        for (Entity e : center.getWorld().getNearbyEntities(center, r, r, r)) {
            if (e instanceof LivingEntity le && e != p && !(e instanceof Player)) {
                damageTarget(p, le, power, def.element);
                hits++;
            }
        }
        particleRing(center, r, def.element);
        p.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        Msg.send(p, "&b" + def.name + " &7→ " + hits + "명 적중");
    }

    // ───────────────────────── 회복 ─────────────────────────

    private void heal(Player p, SkillDef def, double power) {
        double amt = Math.abs(power);
        healEntity(p, amt);
        if (def.radius > 0) {
            for (Entity e : p.getNearbyEntities(def.radius, def.radius, def.radius)) {
                if (e instanceof Player ally) healEntity(ally, amt);
            }
        }
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.8, 0), 6);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
        Msg.send(p, "&a" + def.name + " &7→ §a+" + String.format("%.1f", amt) + " 회복");
    }

    // ───────────────────────── 버프 ─────────────────────────

    private void buff(Player p, SkillDef def) {
        int dur = def.durationTicks > 0 ? def.durationTicks : 200;
        String key = (def.id + " " + (def.name == null ? "" : def.name)).toLowerCase();
        if (containsAny(key, "방패", "실드", "shield", "보호", "철벽", "금강", "방어")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, dur, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, dur, 1));
        } else if (containsAny(key, "가속", "신속", "speed", "질주", "신경", "neural", "haste")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, dur, 1));
        } else if (containsAny(key, "비행", "flight", "fly")) {
            grantFlight(p, dur);
        } else if (containsAny(key, "수중", "호흡", "water", "navigation", "항해")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, dur, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, dur, 0));
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, dur, 1));
        }
        p.getWorld().spawnParticle(Particle.TOTEM, p.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
        Msg.send(p, "&e" + def.name + " &7시전 (" + dur / 20 + "초)");
    }

    private void grantFlight(Player p, int dur) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getAllowFlight()) return;
        p.setAllowFlight(true);
        p.setFlying(true);
        RebornCore.get().scheduler().runTaskLater(() -> {
            if (p.isOnline() && p.getGameMode() != GameMode.CREATIVE
                    && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }, dur);
    }

    // ───────────────────────── 이동기 ─────────────────────────

    private void dash(Player p, SkillDef def) {
        double power = def.projectileSpeed > 0 ? def.projectileSpeed : 1.6;
        Vector dir = p.getEyeLocation().getDirection().setY(0.25).normalize();
        p.setVelocity(dir.multiply(power));
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 12, 0.2, 0.1, 0.2, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
    }

    private void blink(Player p, SkillDef def) {
        double range = def.range > 0 ? def.range : 12;
        Location from = p.getLocation().clone();
        Block b = p.getTargetBlockExact((int) range);
        Location dest;
        if (b != null) dest = b.getLocation().add(0.5, 1, 0.5);
        else dest = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(range));
        dest.setYaw(from.getYaw());
        dest.setPitch(from.getPitch());
        p.getWorld().spawnParticle(Particle.PORTAL, from.add(0, 1, 0), 30);
        p.teleport(dest);
        p.getWorld().spawnParticle(Particle.PORTAL, dest.add(0, 1, 0), 30);
        p.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    // ───────────────────────── 지속 피해 ─────────────────────────

    private void dot(Player p, SkillDef def, double power) {
        double range = def.range > 0 ? def.range : 8;
        LivingEntity t = coneTarget(p, range);
        if (t == null) { Msg.warn(p, "정면에 대상이 없다."); return; }
        int total = def.durationTicks > 0 ? def.durationTicks : 100;
        int hits = 5;
        int interval = Math.max(10, total / hits);
        double per = power / hits;
        final UUID tid = t.getUniqueId();
        final String el = def.element;
        for (int i = 1; i <= hits; i++) {
            RebornCore.get().scheduler().runTaskLater(() -> {
                Entity e = org.bukkit.Bukkit.getEntity(tid);
                if (e instanceof LivingEntity le && !le.isDead()) {
                    le.damage(per * Element.multiplier(el, le), p);
                    Element.applyStatus(le, el, per);
                    le.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, le.getLocation().add(0, 1, 0), 3);
                }
            }, (long) i * interval);
        }
        Msg.send(p, "&5" + def.name + " &7→ 지속 피해 " + hits + "회");
    }

    // ───────────────────────── 연쇄 ─────────────────────────

    private void chain(Player p, SkillDef def, double power) {
        double range = def.range > 0 ? def.range : 8;
        LivingEntity cur = coneTarget(p, range);
        if (cur == null) { Msg.warn(p, "정면에 대상이 없다."); return; }
        double jumpRange = def.radius > 0 ? def.radius : 4;
        int maxJumps = 5;
        Set<UUID> hit = new HashSet<>();
        Location prev = p.getEyeLocation();
        double dmg = power;
        int jumps = 0;
        while (cur != null && jumps < maxJumps) {
            damageTarget(p, cur, dmg, def.element);
            hit.add(cur.getUniqueId());
            particleLine(prev, cur.getLocation().add(0, 1, 0));
            prev = cur.getLocation().add(0, 1, 0);
            cur = nearestUnhit(cur.getLocation(), jumpRange, hit, p);
            dmg *= 0.8;
            jumps++;
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.6f);
        Msg.send(p, "&b" + def.name + " &7→ " + jumps + "연쇄");
    }

    // ───────────────────────── 소환 ─────────────────────────

    private void summon(Player p, SkillDef def) {
        EntityType type = EntityType.WOLF;
        if (def.summonMob != null) {
            try { type = EntityType.valueOf(def.summonMob.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        Location at = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(2));
        Entity e = p.getWorld().spawnEntity(at, type);
        e.setCustomName("§b" + p.getName() + "의 " + def.name);
        e.setCustomNameVisible(true);
        if (e instanceof Tameable tame) { tame.setTamed(true); tame.setOwner(p); }
        int dur = def.durationTicks > 0 ? def.durationTicks : 600;
        final UUID eid = e.getUniqueId();
        RebornCore.get().scheduler().runTaskLater(() -> {
            Entity ent = org.bukkit.Bukkit.getEntity(eid);
            if (ent != null && !ent.isDead()) {
                ent.getWorld().spawnParticle(Particle.CLOUD, ent.getLocation(), 10);
                ent.remove();
            }
        }, dur);
        p.getWorld().playSound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);
        Msg.send(p, "&b" + def.name + " &7→ 소환 (" + dur / 20 + "초)");
    }

    // ───────────────────────── 공용 ─────────────────────────

    /** 상성 배수·상태이상 적용 후 피해. 시전자에게 상성 로그. */
    private void damageTarget(Player caster, LivingEntity t, double base, String element) {
        double mult = Element.multiplier(element, t);
        double dmg = base * mult;
        t.damage(dmg, caster);
        Element.applyStatus(t, element, base);
        String tag = t.getCustomName() != null ? t.getCustomName() : t.getType().name();
        if (mult > 1.0) Msg.send(caster, "&c" + tag + " &7→ " + fmt(dmg) + " §a상성 우위 ×" + fmt(mult));
        else if (mult < 1.0) Msg.send(caster, "&c" + tag + " &7→ " + fmt(dmg) + " §c상성 불리 ×" + fmt(mult));
        else Msg.send(caster, "&c" + tag + " &7→ " + fmt(dmg) + " 피해");
    }

    private void healEntity(LivingEntity le, double amt) {
        double max = 20;
        var attr = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) max = attr.getValue();
        le.setHealth(Math.min(max, le.getHealth() + amt));
    }

    /** 정면 콘(시야 0.6 이상) 범위 내 가장 가까운 살아있는 비-시전자 엔티티. */
    private LivingEntity coneTarget(Player p, double range) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        Vector dir = p.getEyeLocation().getDirection();
        for (Entity e : p.getNearbyEntities(range, range / 2 + 2, range)) {
            if (e == p || !(e instanceof LivingEntity le)) continue;
            Vector to = e.getLocation().toVector().subtract(p.getLocation().toVector());
            if (to.lengthSquared() < 0.01) continue;
            if (dir.dot(to.normalize()) < 0.6) continue;
            double d = e.getLocation().distanceSquared(p.getLocation());
            if (d < bestDist) { bestDist = d; best = le; }
        }
        return best;
    }

    private LivingEntity nearestUnhit(Location from, double range, Set<UUID> hit, Player caster) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : from.getWorld().getNearbyEntities(from, range, range, range)) {
            if (e == caster || !(e instanceof LivingEntity le)) continue;
            if (hit.contains(e.getUniqueId())) continue;
            double d = e.getLocation().distanceSquared(from);
            if (d < bestDist) { bestDist = d; best = le; }
        }
        return best;
    }

    /** 바라보는 지점(타깃 블록) 또는 시선 끝 위치. */
    private Location aimLocation(Player p, double range) {
        Block b = p.getTargetBlockExact((int) Math.max(4, range));
        if (b != null) return b.getLocation().add(0.5, 0.5, 0.5);
        return p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(range));
    }

    private void particleRing(Location center, double r, String element) {
        Particle particle = "FIRE".equalsIgnoreCase(element) ? Particle.FLAME
                : "ICE".equalsIgnoreCase(element) ? Particle.SNOWFLAKE
                : "DARK".equalsIgnoreCase(element) ? Particle.SMOKE_NORMAL
                : Particle.CRIT_MAGIC;
        if (center.getWorld() == null) return;
        for (int a = 0; a < 360; a += 15) {
            double rad = Math.toRadians(a);
            Location pt = center.clone().add(Math.cos(rad) * r, 0.2, Math.sin(rad) * r);
            center.getWorld().spawnParticle(particle, pt, 2, 0, 0, 0, 0);
        }
    }

    private void particleLine(Location a, Location b) {
        if (a.getWorld() == null || a.getWorld() != b.getWorld()) return;
        Vector diff = b.toVector().subtract(a.toVector());
        double len = diff.length();
        if (len < 0.01) return;
        Vector step = diff.normalize().multiply(0.6);
        Location cur = a.clone();
        for (double d = 0; d < len; d += 0.6) {
            a.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, cur, 1, 0, 0, 0, 0);
            cur.add(step);
        }
    }

    private boolean containsAny(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }

    private String fmt(double d) { return String.format("%.1f", d); }
}
