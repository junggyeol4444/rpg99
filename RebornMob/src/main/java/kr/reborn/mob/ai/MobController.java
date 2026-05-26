package kr.reborn.mob.ai;

import kr.reborn.core.util.Rand;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.def.MobDef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 모든 활성 커스텀 몹의 AI를 매 주기 구동한다.
 *
 * 스폰 시 register(), 사망 시 unregister(). tick()이 각 몹을 MobDef.ai 종류별로
 * 분기해 진짜 행동(협공·카이팅·페이즈·소환·은신…)을 실행한다.
 * 바닐라 AI(BASIC/FLYING/AQUATIC/PASSIVE)는 건드리지 않는다.
 */
public final class MobController {

    private final RebornMob plugin;
    private final Map<UUID, MobRuntime> mobs = new ConcurrentHashMap<>();
    private final double aggro;
    private final NamespacedKey tagKey;

    public MobController(RebornMob plugin) {
        this.plugin = plugin;
        this.aggro = plugin.getConfig().getDouble("aggro-range", 24);
        this.tagKey = new NamespacedKey(plugin, "rmob");
    }

    public void register(LivingEntity e, MobDef def) {
        MobRuntime rt = new MobRuntime(def.id);
        rt.home = e.getLocation().clone();
        mobs.put(e.getUniqueId(), rt);
    }

    public void unregister(UUID id) { mobs.remove(id); }
    public int active() { return mobs.size(); }

    public void tick() {
        long now = now();
        for (var it = mobs.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, MobRuntime> en = it.next();
            MobRuntime rt = en.getValue();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (ent == null) {
                // 청크 언로드 또는 디스폰 — 5분 넘게 안 보이면 누수 방지로 정리
                if (now - rt.lastSeen > 300_000L) it.remove();
                continue;
            }
            if (!(ent instanceof LivingEntity le) || le.isDead()) { it.remove(); continue; }
            MobDef def = plugin.registry().get(rt.defId);
            if (def == null) { it.remove(); continue; }
            rt.lastSeen = now;
            try { act(le, def, rt); } catch (Throwable ignored) {}
        }
    }

    private void act(LivingEntity le, MobDef def, MobRuntime rt) {
        Player target = nearestPlayer(le, aggro);
        switch (def.ai) {
            case PACK:        pack(le, def, target); break;
            case RANGED:      ranged(le, def, rt, target); break;
            case BOSS:        boss(le, def, rt, target); break;
            case SWARM:       swarm(le, target); break;
            case FLEE:        flee(le, target); break;
            case TERRITORIAL: territorial(le, rt, target); break;
            case CASTER:      caster(le, def, rt, target); break;
            case TANK:        tank(le, rt, target); break;
            case SUPPORT:     support(le, rt); break;
            case BERSERKER:   berserker(le, def, target); break;
            case SUMMONER:    summoner(le, def, rt, target); break;
            case AMBUSH:      ambush(le, def, rt, target); break;
            default: break;  // BASIC/FLYING/AQUATIC/PASSIVE → 바닐라
        }
    }

    // ───────────────────────── 아키타입 ─────────────────────────

    private void pack(LivingEntity le, MobDef def, Player target) {
        if (target == null) return;
        setTarget(le, target);
        // 근처 같은 종 동료도 같은 표적으로 — 협공
        for (Entity e : le.getNearbyEntities(12, 6, 12)) {
            if (e instanceof Mob m && isSameDef(m, def.id)) setTarget(m, target);
        }
    }

    private void ranged(LivingEntity le, MobDef def, MobRuntime rt, Player target) {
        if (target == null) return;
        setTarget(le, target);
        double dist = le.getLocation().distance(target.getLocation());
        if (dist < 6) {                 // 너무 가까움 → 후퇴
            moveTo(le, fleePoint(le, target, 6), 1.3);
        } else if (dist > 16) {         // 너무 멀음 → 접근
            moveTo(le, target.getLocation(), 1.0);
        } else if (now() >= rt.nextAction) {
            shootArrow(le, target, def.damage);
            rt.nextAction = now() + 1500;
        }
    }

    private void boss(LivingEntity le, MobDef def, MobRuntime rt, Player target) {
        double pct = le.getHealth() / maxHp(le);
        int wantPhase = pct > 0.75 ? 1 : pct > 0.5 ? 2 : pct > 0.25 ? 3 : 4;
        if (wantPhase > rt.phase) {
            rt.phase = wantPhase;
            enterPhase(le, def, rt, target);
        }
        if (target != null) {
            setTarget(le, target);
            if (now() >= rt.nextAction) {
                if (rt.phase >= 2) shockwave(le, def, 6);
                rt.nextAction = now() + 4000;
            }
        }
    }

    private void enterPhase(LivingEntity le, MobDef def, MobRuntime rt, Player target) {
        Bukkit.broadcastMessage("§5§l[보스] §f" + def.name + " §d페이즈 " + rt.phase + "!");
        le.getWorld().playSound(le.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
        switch (rt.phase) {
            case 2: shockwave(le, def, 7); break;
            case 3: summonMinions(le, def, target, 3); break;
            case 4:
                if (!rt.enraged) { enrage(le, def); rt.enraged = true; }
                shockwave(le, def, 8);
                break;
            default: break;
        }
    }

    private void swarm(LivingEntity le, Player target) {
        if (target == null) return;
        setTarget(le, target);
        moveTo(le, target.getLocation(), 1.35);  // 떼로 빠르게 돌격
    }

    private void flee(LivingEntity le, Player target) {
        if (target == null) return;
        moveTo(le, fleePoint(le, target, 8), 1.4);
    }

    private void territorial(LivingEntity le, MobRuntime rt, Player target) {
        if (rt.home != null && le.getWorld() == rt.home.getWorld()
                && le.getLocation().distance(rt.home) > 20) {
            clearTarget(le);
            moveTo(le, rt.home, 1.0);   // 둥지 복귀
            return;
        }
        if (target != null && rt.home != null
                && target.getLocation().getWorld() == rt.home.getWorld()
                && target.getLocation().distance(rt.home) < 16) {
            setTarget(le, target);
        } else {
            clearTarget(le);
        }
    }

    private void caster(LivingEntity le, MobDef def, MobRuntime rt, Player target) {
        if (target == null) return;
        setTarget(le, target);
        double dist = le.getLocation().distance(target.getLocation());
        if (dist < 5) moveTo(le, fleePoint(le, target, 5), 1.2);
        if (now() >= rt.nextAction) {
            castSpell(le, def, target);
            rt.nextAction = now() + 3000;
        }
    }

    private void tank(LivingEntity le, MobRuntime rt, Player target) {
        if (target == null) return;
        setTarget(le, target);
        moveTo(le, target.getLocation(), 0.9);
        if (now() >= rt.nextAction) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 120, 1));
            rt.nextAction = now() + 5000;
        }
    }

    private void support(LivingEntity le, MobRuntime rt) {
        LivingEntity ally = lowestHpAlly(le, 16);
        if (ally != null && now() >= rt.nextAction) {
            double max = maxHp(ally);
            ally.setHealth(Math.min(max, ally.getHealth() + max * 0.2));
            ally.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 120, 0));
            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1.5, 0), 5);
            rt.nextAction = now() + 4000;
        }
        Player p = nearestPlayer(le, 10);   // 플레이어 회피 (후방 지원)
        if (p != null) moveTo(le, fleePoint(le, p, 8), 1.2);
    }

    private void berserker(LivingEntity le, MobDef def, Player target) {
        double pct = le.getHealth() / maxHp(le);
        double rage = 1 + (1 - pct);  // HP 낮을수록 최대 2배
        var dmg = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(def.damage * rage);
        var spd = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(def.speed * (1 + (1 - pct) * 0.6));
        if (pct < 0.3) le.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, le.getLocation().add(0, 1.8, 0), 3);
        if (target != null) setTarget(le, target);
    }

    private void summoner(LivingEntity le, MobDef def, MobRuntime rt, Player target) {
        if (target == null) return;
        setTarget(le, target);
        if (rt.summonCount < 6 && now() >= rt.nextAction) {
            summonMinions(le, def, target, 2);
            rt.summonCount += 2;
            rt.nextAction = now() + 8000;
        }
    }

    private void ambush(LivingEntity le, MobDef def, MobRuntime rt, Player target) {
        if (target == null) {
            if (!rt.hidden) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                rt.hidden = true;
            }
            return;
        }
        double dist = le.getLocation().distance(target.getLocation());
        if (rt.hidden && dist <= 5) {
            le.removePotionEffect(PotionEffectType.INVISIBILITY);
            rt.hidden = false;
            Vector lunge = target.getLocation().toVector().subtract(le.getLocation().toVector())
                    .normalize().multiply(1.2).setY(0.4);
            le.setVelocity(lunge);
            target.damage(def.damage * 1.5, le);  // 기습 보너스
            le.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 12);
            le.getWorld().playSound(le.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.7f);
            setTarget(le, target);
        } else if (!rt.hidden) {
            setTarget(le, target);  // 정체 노출 — 추격
        }
    }

    // ───────────────────────── 능력 ─────────────────────────

    private void shootArrow(LivingEntity le, Player target, double damage) {
        Arrow arrow = le.launchProjectile(Arrow.class);
        Vector v = target.getEyeLocation().toVector()
                .subtract(le.getEyeLocation().toVector()).normalize().multiply(2.2);
        arrow.setVelocity(v);
        arrow.setDamage(Math.max(1, damage / 2.0));
        le.getWorld().playSound(le.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1f, 1f);
    }

    private void castSpell(LivingEntity le, MobDef def, Player target) {
        Location c = target.getLocation();
        if (c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.SPELL_WITCH, c.clone().add(0, 1, 0), 30, 1, 1, 1, 0.1);
        c.getWorld().playSound(c, Sound.ENTITY_EVOKER_CAST_SPELL, 1f, 1f);
        for (Entity e : c.getWorld().getNearbyEntities(c, 3, 3, 3)) {
            if (e instanceof Player pl && survival(pl)) {
                pl.damage(def.damage, le);
                pl.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0));
            }
        }
    }

    private void shockwave(LivingEntity le, MobDef def, double radius) {
        Location c = le.getLocation();
        if (c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, c, 5);
        c.getWorld().playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        for (Entity e : le.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player pl && survival(pl)) {
                pl.damage(def.damage * 1.5, le);
                Vector kb = pl.getLocation().toVector().subtract(c.toVector())
                        .normalize().multiply(1.5).setY(0.6);
                pl.setVelocity(kb);
            }
        }
    }

    private void summonMinions(LivingEntity le, MobDef def, Player target, int count) {
        if (le.getWorld() == null) return;
        for (int i = 0; i < count; i++) {
            Location at = le.getLocation().add(Rand.rangeD(-2, 2), 0, Rand.rangeD(-2, 2));
            Entity m = le.getWorld().spawnEntity(at, EntityType.ZOMBIE);
            if (m instanceof LivingEntity ml) {
                var hp = ml.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (hp != null) { hp.setBaseValue(10); ml.setHealth(10); }
                ml.setCustomName("§8" + stripColor(def.name) + "의 수하");
                if (ml instanceof Mob mob && target != null) mob.setTarget(target);
            }
        }
        le.getWorld().playSound(le.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
    }

    private void enrage(LivingEntity le, MobDef def) {
        var dmg = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(def.damage * 1.6);
        var spd = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(def.speed * 1.4);
        le.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false));
        le.getWorld().spawnParticle(Particle.SMOKE_LARGE, le.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
    }

    // ───────────────────────── 보조 ─────────────────────────

    private Player nearestPlayer(LivingEntity le, double range) {
        Player best = null;
        double bestD = range * range;
        for (Entity e : le.getNearbyEntities(range, range, range)) {
            if (!(e instanceof Player p) || !survival(p) || p.isDead()) continue;
            double d = p.getLocation().distanceSquared(le.getLocation());
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private LivingEntity lowestHpAlly(LivingEntity self, double range) {
        LivingEntity best = null;
        double bestRatio = 1.0;
        for (Entity e : self.getNearbyEntities(range, range, range)) {
            if (e == self || !(e instanceof LivingEntity le)) continue;
            if (!le.getPersistentDataContainer().has(tagKey, PersistentDataType.STRING)) continue;
            double ratio = le.getHealth() / maxHp(le);
            if (ratio < bestRatio && ratio < 0.95) { bestRatio = ratio; best = le; }
        }
        return best;
    }

    private boolean isSameDef(LivingEntity le, String defId) {
        return defId.equals(le.getPersistentDataContainer().get(tagKey, PersistentDataType.STRING));
    }

    private Location fleePoint(LivingEntity le, Player from, double dist) {
        Vector away = le.getLocation().toVector().subtract(from.getLocation().toVector());
        if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
        return le.getLocation().add(away.normalize().multiply(dist));
    }

    private void setTarget(LivingEntity le, Player target) {
        if (le instanceof Mob mob) mob.setTarget(target);
    }

    private void setTarget(Mob mob, Player target) { mob.setTarget(target); }

    private void clearTarget(LivingEntity le) {
        if (le instanceof Mob mob) mob.setTarget(null);
    }

    private void moveTo(LivingEntity le, Location to, double speed) {
        if (le instanceof Mob mob) {
            try { mob.getPathfinder().moveTo(to, speed); } catch (Throwable ignored) {}
        }
    }

    private double maxHp(LivingEntity le) {
        var attr = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20;
    }

    private boolean survival(Player p) {
        return p.getGameMode() == org.bukkit.GameMode.SURVIVAL
                || p.getGameMode() == org.bukkit.GameMode.ADVENTURE;
    }

    private String stripColor(String s) { return s == null ? "" : s.replaceAll("&.|§.", ""); }

    private long now() { return System.currentTimeMillis(); }
}
