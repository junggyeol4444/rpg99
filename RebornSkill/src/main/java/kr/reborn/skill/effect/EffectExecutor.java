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
    private final kr.reborn.skill.signature.SignatureExecutor sigExec =
            new kr.reborn.skill.signature.SignatureExecutor();
    /** 현재 실행 중인 스킬의 시그니처 — damageTarget이 hit 렌더에 사용. */
    private kr.reborn.skill.signature.SkillSignature currentSig;

    public EffectExecutor(RebornSkill plugin) { this.plugin = plugin; }

    public void execute(Player caster, SkillDef def, double power) {
        // 시그니처 룩업 + 시전 시각·청각 효과
        kr.reborn.skill.signature.SkillSignature sig =
                kr.reborn.skill.signature.SignatureRegistry.lookup(def.id);
        this.currentSig = sig;
        if (sig != null) {
            try { sigExec.renderCast(caster, sig); }
            catch (Throwable ignored) {}
            if (sig.flavorText != null && !sig.flavorText.isEmpty()) {
                try { kr.reborn.core.util.Msg.send(caster, sig.flavorText); }
                catch (Throwable ignored) {}
            }
        }
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
            case SHIELD:     shield(caster, def, power); break;
            case DEBUFF:     debuff(caster, def, power); break;
            case CHANNELED:  channeled(caster, def, power); break;
            case TRANSFORM:  transform(caster, def); break;
            case GRAB:       grab(caster, def); break;
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
        // 1. 고유 BuffProfile 룩업 (40+ 고유 스킬별 차별화)
        kr.reborn.skill.buff.BuffProfile profile = kr.reborn.skill.buff.BuffRegistry.get(def.id);
        if (profile != null) {
            for (var st : profile.statuses) {
                int d = st.duration < 0 ? Integer.MAX_VALUE : st.duration;
                try { p.addPotionEffect(new PotionEffect(st.type, d, st.amplifier, st.ambient, st.particles)); }
                catch (Throwable ignored) {}
            }
            for (String side : profile.sideEffects) {
                applySideEffect(p, side, dur);
            }
            // 시그니처가 있으면 별도 입자 — 없으면 약식 입자
            if (kr.reborn.skill.signature.SignatureRegistry.lookup(def.id) == null) {
                try { p.getWorld().spawnParticle(Particle.TOTEM, p.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.1); }
                catch (Throwable ignored) {}
            }
            Msg.send(p, "&e" + def.name + " &7시전 (" + dur / 20 + "초)");
            return;
        }
        // 2. Fallback: 기존 키워드 5분류 (등록 안 된 스킬)
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

    /** 부수 효과 — sideEffects 문자열로 코드 분기. */
    private void applySideEffect(Player p, String side, int dur) {
        switch (side) {
            case "ALLOW_FLIGHT" -> grantFlight(p, dur);
            case "REMOVE_CONFUSION_POISON" -> {
                if (p.hasPotionEffect(PotionEffectType.CONFUSION)) p.removePotionEffect(PotionEffectType.CONFUSION);
                if (p.hasPotionEffect(PotionEffectType.POISON)) p.removePotionEffect(PotionEffectType.POISON);
            }
            case "DREAM_TRAINING" -> {
                if (p.getFoodLevel() < 10) {
                    try {
                        kr.reborn.core.RebornCore.get().api().addStat(p.getUniqueId(),
                                kr.reborn.core.data.StatType.MENTAL, 0.05, "dream-training");
                    } catch (Throwable ignored) {}
                }
            }
            default -> { /* unknown side effect */ }
        }
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
        // 시그니처 hit 렌더
        if (currentSig != null) {
            try { sigExec.renderHit(caster, t, currentSig); }
            catch (Throwable ignored) {}
        }
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

    /* ───────────────── 신규 스킬 타입 구현 ───────────────── */

    /**
     * 보호막 — BuffRegistry 우선, 없으면 element/skill 컨셉별 기본 흡수·저항.
     *   성스러운 방패: GLOW + END_ROD + BELL_USE
     *   마기 방패: SMOKE_LARGE + DRAGON_BREATH + WITHER_AMBIENT
     *   에너지 실드: ELECTRIC_SPARK + END_ROD + BEACON_AMBIENT
     *   금강불괴: BLOCK_DUST + IRON_GOLEM + ANVIL_LAND
     */
    private void shield(Player caster, SkillDef def, double power) {
        int dur = def.durationTicks > 0 ? def.durationTicks : 200;
        kr.reborn.skill.buff.BuffProfile profile = kr.reborn.skill.buff.BuffRegistry.get(def.id);
        if (profile != null) {
            for (var st : profile.statuses) {
                int d = st.duration < 0 ? Integer.MAX_VALUE : st.duration;
                try { caster.addPotionEffect(new PotionEffect(st.type, d, st.amplifier, st.ambient, st.particles)); }
                catch (Throwable ignored) {}
            }
            for (String side : profile.sideEffects) applySideEffect(caster, side, dur);
        } else {
            try {
                caster.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, dur, 3));
                caster.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, dur, 3));
            } catch (Throwable ignored) {}
        }
        // 시그니처가 있으면 거기서 처리; 없을 때만 폴백 입자
        if (kr.reborn.skill.signature.SignatureRegistry.lookup(def.id) == null) {
            Particle prt = shieldParticle(def);
            Sound snd = shieldSound(def);
            try { caster.getWorld().spawnParticle(prt, caster.getLocation().add(0, 1, 0), 50, 1, 1, 1); }
            catch (Throwable ignored) {}
            try { caster.getWorld().playSound(caster.getLocation(), snd, 0.8f, 1.5f); }
            catch (Throwable ignored) {}
        }
        Msg.send(caster, "&b" + def.name + " — " + (dur / 20) + "초 보호.");
    }

    /**
     * 광역 디버프 — 원소·스킬 컨셉별 차별화.
     *   FIRE: BLINDNESS + WEAKNESS (시야 + 약화)
     *   ICE: SLOW + SLOW_DIGGING (둔화 + 채굴)
     *   DARK: BLINDNESS + WITHER (실명 + 위더)
     *   POISON: POISON + WEAKNESS (중독)
     *   HOLY: GLOWING + WEAKNESS (성광 노출)
     *   LIGHTNING: SLOW + NAUSEA (감전 멍)
     *   기본: WEAKNESS + SLOW
     */
    private void debuff(Player caster, SkillDef def, double power) {
        double r = def.radius > 0 ? def.radius : 10;
        int dur = def.durationTicks > 0 ? def.durationTicks : 200;
        int amp = (int) Math.min(4, power / 30);
        String el = def.element == null ? "" : def.element.toUpperCase();
        PotionEffectType[] effects = debuffEffects(el);
        Particle prt = debuffParticle(el);
        int hits = 0;
        for (Entity e : caster.getNearbyEntities(r, r, r)) {
            if (e instanceof LivingEntity le && e != caster) {
                try {
                    for (var eff : effects) le.addPotionEffect(new PotionEffect(eff, dur, amp));
                    le.getWorld().spawnParticle(prt, le.getLocation().add(0, 1, 0), 20, 1, 1, 1);
                } catch (Throwable ignored) {}
                hits++;
            }
        }
        try { caster.getWorld().playSound(caster.getLocation(), debuffSound(el), 1f, 0.9f); }
        catch (Throwable ignored) {}
        Msg.send(caster, "&8" + def.name + " — 반경 " + r + " amp " + amp + " (" + hits + "명)");
    }

    /**
     * 지속 시전 — 원소별 빔 입자·피해 부가.
     *   FIRE: FLAME + 화상
     *   ICE: SNOWFLAKE + 둔화
     *   DARK: SQUID_INK + 위더
     *   HOLY: END_ROD + 발광
     *   LIGHTNING: ELECTRIC_SPARK + 낙뢰 (확률)
     *   POISON: SLIME + 중독
     */
    private void channeled(Player caster, SkillDef def, double power) {
        int ticks = def.durationTicks > 0 ? def.durationTicks : 100;
        String el = def.element == null ? "" : def.element.toUpperCase();
        Particle beamP = channeledParticle(el);
        final int[] elapsed = {0};
        final int every = 10;
        Runnable beam = new Runnable() {
            @Override
            public void run() {
                if (elapsed[0] >= ticks) return;
                if (!caster.isOnline() || caster.isDead()) return;
                Location origin = caster.getEyeLocation();
                Vector dir = origin.getDirection().normalize();
                for (int i = 0; i < 20; i++) {
                    Location pt = origin.clone().add(dir.clone().multiply(i));
                    try { pt.getWorld().spawnParticle(beamP, pt, 5, 0.2, 0.2, 0.2); }
                    catch (Throwable ignored) {}
                    for (Entity e : pt.getWorld().getNearbyEntities(pt, 1.2, 1.2, 1.2)) {
                        if (e instanceof LivingEntity le && e != caster) {
                            try {
                                le.damage(power * 0.15 * Element.multiplier(el, le), caster);
                                Element.applyStatus(le, el, power * 0.2);
                            } catch (Throwable ignored) {}
                        }
                    }
                }
                elapsed[0] += every;
                RebornCore.get().scheduler().runTaskLater(this, every);
            }
        };
        beam.run();
        Msg.send(caster, "&c" + def.name + " — " + (ticks / 20) + "초 지속 빔.");
    }

    /**
     * 변신 — BuffRegistry 우선, 없으면 skill 컨셉별 효과·사운드.
     *   yokai/fox: ENTITY_FOX_AGGRO
     *   dragon/breath: ENTITY_ENDER_DRAGON_GROWL
     *   demon/마/sura: ENTITY_WITHER_SPAWN
     *   ocean/sea: ENTITY_DOLPHIN_AMBIENT
     *   기본: ENDER_DRAGON_GROWL
     */
    private void transform(Player caster, SkillDef def) {
        int dur = def.durationTicks > 0 ? def.durationTicks : 600;
        kr.reborn.skill.buff.BuffProfile profile = kr.reborn.skill.buff.BuffRegistry.get(def.id);
        if (profile != null) {
            for (var st : profile.statuses) {
                int d = st.duration < 0 ? Integer.MAX_VALUE : st.duration;
                try { caster.addPotionEffect(new PotionEffect(st.type, d, st.amplifier, st.ambient, st.particles)); }
                catch (Throwable ignored) {}
            }
            for (String side : profile.sideEffects) applySideEffect(caster, side, dur);
        } else {
            try {
                caster.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, dur, 2));
                caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 1));
                caster.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, dur, 2));
                caster.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, dur, 1));
            } catch (Throwable ignored) {}
        }
        if (kr.reborn.skill.signature.SignatureRegistry.lookup(def.id) == null) {
            try { caster.getWorld().playSound(caster.getLocation(), transformSound(def), 1.5f, 1.0f); }
            catch (Throwable ignored) {}
            try { caster.getWorld().spawnParticle(transformParticle(def), caster.getLocation().add(0, 1, 0), 80, 1, 2, 1); }
            catch (Throwable ignored) {}
        }
        Msg.send(caster, "&5" + def.name + " — " + (dur / 20) + "초간 변신.");
    }

    /**
     * 끌어당기기 — skill 컨셉별 입자·사운드 차별.
     *   demon/마: SQUID_INK + WARDEN_AMBIENT
     *   tao/도: PORTAL + AMETHYST_CHIME
     *   spirit/정령: SPELL_MOB + PHANTOM_FLAP
     *   기본: PORTAL + ENDERMAN_TELEPORT
     */
    private void grab(Player caster, SkillDef def) {
        double r = def.radius > 0 ? def.radius : 10;
        int hits = 0;
        for (Entity e : caster.getNearbyEntities(r, r, r)) {
            if (e instanceof LivingEntity le && e != caster) {
                try {
                    Vector dir = caster.getLocation().toVector().subtract(e.getLocation().toVector())
                            .normalize().multiply(2).setY(0.5);
                    le.setVelocity(dir);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 3));
                } catch (Throwable ignored) {}
                hits++;
            }
        }
        if (kr.reborn.skill.signature.SignatureRegistry.lookup(def.id) == null) {
            try { caster.getWorld().spawnParticle(grabParticle(def), caster.getLocation().add(0, 1, 0), 60, 2, 2, 2); }
            catch (Throwable ignored) {}
            try { caster.getWorld().playSound(caster.getLocation(), grabSound(def), 1f, 1.1f); }
            catch (Throwable ignored) {}
        }
        Msg.send(caster, "&5" + def.name + " — 반경 " + r + " (" + hits + "명)");
    }

    // ───────── 폴백 입자·사운드 헬퍼 (시그니처가 없을 때만 사용) ─────────

    private Particle shieldParticle(SkillDef def) {
        String id = def.id == null ? "" : def.id;
        String el = def.element == null ? "" : def.element.toUpperCase();
        if (el.equals("HOLY") || id.contains("holy") || id.contains("divine") || id.contains("heaven")) return Particle.END_ROD;
        if (el.equals("DARK") || id.contains("demon") || id.contains("maggi") || id.contains("ma_")) return Particle.SMOKE_LARGE;
        if (id.contains("cyber") || id.contains("energy") || id.contains("neural")) return Particle.ELECTRIC_SPARK;
        if (id.contains("vajra") || id.contains("geum") || id.contains("iron")) return Particle.NAUTILUS;
        if (el.equals("FIRE")) return Particle.FLAME;
        if (el.equals("ICE")) return Particle.SNOWFLAKE;
        if (id.contains("spirit") || id.contains("정령")) return Particle.SPELL_MOB;
        return Particle.END_ROD;
    }

    private Sound shieldSound(SkillDef def) {
        String id = def.id == null ? "" : def.id;
        if (id.contains("holy") || id.contains("divine") || id.contains("heaven")) return Sound.BLOCK_BELL_USE;
        if (id.contains("demon") || id.contains("maggi")) return Sound.ENTITY_WITHER_AMBIENT;
        if (id.contains("cyber") || id.contains("energy")) return Sound.BLOCK_BEACON_AMBIENT;
        if (id.contains("vajra") || id.contains("geum") || id.contains("iron")) return Sound.BLOCK_ANVIL_LAND;
        return Sound.BLOCK_BEACON_ACTIVATE;
    }

    private PotionEffectType[] debuffEffects(String el) {
        switch (el) {
            case "FIRE":      return new PotionEffectType[]{PotionEffectType.BLINDNESS, PotionEffectType.WEAKNESS};
            case "ICE":       return new PotionEffectType[]{PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING};
            case "WATER":     return new PotionEffectType[]{PotionEffectType.SLOW, PotionEffectType.WEAKNESS};
            case "DARK":      return new PotionEffectType[]{PotionEffectType.BLINDNESS, PotionEffectType.WITHER};
            case "POISON":    return new PotionEffectType[]{PotionEffectType.POISON, PotionEffectType.WEAKNESS};
            case "HOLY":      return new PotionEffectType[]{PotionEffectType.GLOWING, PotionEffectType.WEAKNESS};
            case "LIGHTNING": return new PotionEffectType[]{PotionEffectType.SLOW, PotionEffectType.CONFUSION};
            case "WIND":      return new PotionEffectType[]{PotionEffectType.LEVITATION, PotionEffectType.WEAKNESS};
            case "EARTH":     return new PotionEffectType[]{PotionEffectType.SLOW, PotionEffectType.SLOW_DIGGING};
            case "NATURE":    return new PotionEffectType[]{PotionEffectType.POISON, PotionEffectType.HUNGER};
            case "ARCANE":    return new PotionEffectType[]{PotionEffectType.WEAKNESS, PotionEffectType.CONFUSION};
            default:          return new PotionEffectType[]{PotionEffectType.WEAKNESS, PotionEffectType.SLOW};
        }
    }

    private Particle debuffParticle(String el) {
        switch (el) {
            case "FIRE":      return Particle.FLAME;
            case "ICE":       return Particle.SNOWFLAKE;
            case "WATER":     return Particle.WATER_SPLASH;
            case "DARK":      return Particle.SQUID_INK;
            case "POISON":    return Particle.SLIME;
            case "HOLY":      return Particle.END_ROD;
            case "LIGHTNING": return Particle.ELECTRIC_SPARK;
            case "WIND":      return Particle.CLOUD;
            case "EARTH":     return Particle.LANDING_OBSIDIAN_TEAR;
            case "NATURE":    return Particle.VILLAGER_HAPPY;
            case "ARCANE":    return Particle.SPELL_WITCH;
            default:          return Particle.SQUID_INK;
        }
    }

    private Sound debuffSound(String el) {
        switch (el) {
            case "FIRE":      return Sound.ENTITY_BLAZE_HURT;
            case "ICE":       return Sound.BLOCK_GLASS_BREAK;
            case "DARK":      return Sound.ENTITY_WITHER_AMBIENT;
            case "POISON":    return Sound.ENTITY_SPIDER_HURT;
            case "HOLY":      return Sound.BLOCK_BELL_USE;
            case "LIGHTNING": return Sound.ENTITY_LIGHTNING_BOLT_IMPACT;
            case "WIND":      return Sound.ENTITY_PHANTOM_AMBIENT;
            case "EARTH":     return Sound.BLOCK_STONE_FALL;
            default:          return Sound.ENTITY_VEX_AMBIENT;
        }
    }

    private Particle channeledParticle(String el) {
        switch (el) {
            case "FIRE":      return Particle.FLAME;
            case "ICE":       return Particle.SNOWFLAKE;
            case "WATER":     return Particle.WATER_SPLASH;
            case "DARK":      return Particle.SQUID_INK;
            case "POISON":    return Particle.SLIME;
            case "HOLY":      return Particle.END_ROD;
            case "LIGHTNING": return Particle.ELECTRIC_SPARK;
            case "WIND":      return Particle.CLOUD;
            case "EARTH":     return Particle.LANDING_OBSIDIAN_TEAR;
            case "NATURE":    return Particle.VILLAGER_HAPPY;
            case "ARCANE":    return Particle.SPELL_WITCH;
            default:          return Particle.CRIT_MAGIC;
        }
    }

    private Sound transformSound(SkillDef def) {
        String id = def.id == null ? "" : def.id;
        if (id.contains("yokai") || id.contains("fox")) return Sound.ENTITY_FOX_AGGRO;
        if (id.contains("dragon") || id.contains("yong") || id.contains("ryong")) return Sound.ENTITY_ENDER_DRAGON_GROWL;
        if (id.contains("demon") || id.contains("ma_") || id.contains("sura")) return Sound.ENTITY_WITHER_SPAWN;
        if (id.contains("ocean") || id.contains("sea")) return Sound.ENTITY_DOLPHIN_AMBIENT;
        if (id.contains("spirit") || id.contains("정령")) return Sound.BLOCK_BEACON_ACTIVATE;
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    private Particle transformParticle(SkillDef def) {
        String id = def.id == null ? "" : def.id;
        if (id.contains("yokai") || id.contains("fox")) return Particle.SPELL_WITCH;
        if (id.contains("dragon") || id.contains("yong") || id.contains("ryong")) return Particle.DRAGON_BREATH;
        if (id.contains("demon") || id.contains("ma_") || id.contains("sura")) return Particle.SQUID_INK;
        if (id.contains("ocean") || id.contains("sea")) return Particle.WATER_BUBBLE;
        if (id.contains("spirit") || id.contains("정령")) return Particle.SPELL_MOB;
        return Particle.PORTAL;
    }

    private Particle grabParticle(SkillDef def) {
        String id = def.id == null ? "" : def.id;
        if (id.contains("demon") || id.contains("ma_") || id.contains("yeolma")) return Particle.SQUID_INK;
        if (id.contains("tao") || id.contains("도술")) return Particle.NAUTILUS;
        if (id.contains("spirit") || id.contains("정령")) return Particle.SPELL_MOB;
        if (id.contains("ocean") || id.contains("water")) return Particle.WATER_SPLASH;
        return Particle.PORTAL;
    }

    private Sound grabSound(SkillDef def) {
        String id = def.id == null ? "" : def.id;
        if (id.contains("demon") || id.contains("ma_")) return Sound.ENTITY_WARDEN_AMBIENT;
        if (id.contains("tao") || id.contains("도술")) return Sound.BLOCK_AMETHYST_BLOCK_CHIME;
        if (id.contains("spirit")) return Sound.ENTITY_PHANTOM_FLAP;
        return Sound.ENTITY_ENDERMAN_TELEPORT;
    }
}
