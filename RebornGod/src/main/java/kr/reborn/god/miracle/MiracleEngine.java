package kr.reborn.god.miracle;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * 기적 시전 엔진 — 신성 차감·쿨다운 검증 후 실제 효과 적용.
 *
 * 플레이어 신: /god miracle <type> [target] 명령으로 시전.
 * NPC 신: 추후 WorldAI가 자율 시전 트리거.
 */
public final class MiracleEngine {

    private final RebornGod plugin;

    public MiracleEngine(RebornGod plugin) { this.plugin = plugin; }

    /** 시전 시도. 실패 시 사유 메시지. 성공 시 true. */
    public boolean cast(Player caster, Miracle m, String targetName) {
        God g = plugin.gods().of(caster.getUniqueId());
        if (g == null) { Msg.error(caster, "신만이 기적을 행할 수 있다."); return false; }
        // 등급 검증
        String tier = g.tier(plugin.getConfig().getMapList("tiers"));
        if (Miracle.tierOf(tier) < m.tierIndex()) {
            Msg.error(caster, "이 기적은 " + m.requiredTier + " 이상에서만 가능. 현재: " + tier);
            return false;
        }
        // 신성 비용
        if (g.divinity < m.cost) {
            Msg.error(caster, "신성 부족 — 필요 " + m.cost + ", 보유 " + (int) g.divinity);
            return false;
        }
        // 쿨다운
        long now = System.currentTimeMillis();
        Long next = g.miracleCooldowns.get(m.name());
        if (next != null && now < next) {
            Msg.error(caster, "쿨다운 " + (next - now) / 1000 + "초 남음");
            return false;
        }
        if (m == Miracle.REWRITE_REALITY && next != null) {
            Msg.error(caster, "현실 재기록은 영구 1회 한정. 이미 사용했다."); return false;
        }
        // 차감·쿨다운
        g.divinity -= m.cost;
        if (m.cooldownMs > 0) g.miracleCooldowns.put(m.name(), now + m.cooldownMs);
        else g.miracleCooldowns.put(m.name(), Long.MAX_VALUE);  // 영구
        // 효과 적용
        apply(caster, g, m, targetName);
        Bukkit.broadcastMessage("§6§l[기적] §f" + caster.getName()
                + " §7→ §e" + m.name() + " §7(" + m.requiredTier + ")");
        return true;
    }

    private void apply(Player caster, God g, Miracle m, String targetName) {
        switch (m) {
            case BLESS_FOLLOWERS:    blessFollowers(g); break;
            case DIVINE_SHIELD:      divineShield(caster, targetName); break;
            case GUIDING_LIGHT:      guidingLight(caster, targetName); break;
            case MASS_HEAL:          massHeal(caster); break;
            case SMITE_TARGET:       smite(caster, targetName); break;
            case INSPIRE_NATION:     inspireNation(g, caster); break;
            case WORLD_RAIN_BLESSING: rainBlessing(caster); break;
            case JUDGMENT:           judgment(caster); break;
            case PILLAR_OF_FIRE:     pillarOfFire(caster); break;
            case DIVINE_INTERVENTION: divineIntervention(caster); break;
            case AVATAR_DESCENT:     avatarDescent(caster); break;
            case RESURRECT_FOLLOWER: resurrect(caster, targetName); break;
            case CONCEPT_DOMINION:   conceptDominion(caster, g); break;
            case REWRITE_REALITY:    rewriteReality(caster); break;
        }
    }

    private void blessFollowers(God g) {
        int hit = 0;
        for (UUID uuid : g.followers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            for (PotionEffectType t : new PotionEffectType[]{
                    PotionEffectType.INCREASE_DAMAGE, PotionEffectType.SPEED,
                    PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.LUCK}) {
                p.addPotionEffect(new PotionEffect(t, 6000, 0));
            }
            p.getWorld().spawnParticle(Particle.TOTEM, p.getLocation().add(0, 1.8, 0), 30);
            hit++;
        }
        Bukkit.broadcastMessage("§a[축복] §f신도 " + hit + "명이 신성한 가호를 받았다.");
    }

    private void divineShield(Player caster, String targetName) {
        Player target = pickTarget(caster, targetName);
        target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 4));
        target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 4));
        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 40);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
    }

    private void guidingLight(Player caster, String targetName) {
        Player target = pickTarget(caster, targetName);
        target.getWorld().strikeLightningEffect(target.getLocation());
        caster.sendMessage("§e§l[인도의 빛] §f" + target.getName() + " 위치: "
                + target.getWorld().getName() + " " + target.getLocation().getBlockX()
                + "," + target.getLocation().getBlockY() + "," + target.getLocation().getBlockZ());
    }

    private void massHeal(Player caster) {
        int hit = 0;
        for (Entity e : caster.getNearbyEntities(50, 30, 50)) {
            if (e instanceof Player pl && !pl.isDead()) {
                double max = 20;
                var a = pl.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (a != null) max = a.getValue();
                pl.setHealth(max);
                pl.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                pl.getWorld().spawnParticle(Particle.HEART, pl.getLocation().add(0, 1.8, 0), 20);
                hit++;
            }
        }
        caster.sendMessage("§a대규모 회복 — " + hit + "명");
    }

    private void smite(Player caster, String targetName) {
        Player target = pickTarget(caster, targetName);
        if (target == caster) {
            caster.sendMessage("§c자신을 대상으로 지정할 수 없다."); return;
        }
        target.getWorld().strikeLightning(target.getLocation());
        target.damage(50, caster);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 2));
    }

    private void inspireNation(God g, Player caster) {
        int hit = 0;
        for (UUID uuid : g.followers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 72000, 1));
            p.sendMessage("§6§l[국가 사기] §f신께서 너에게 힘을 주셨다.");
            hit++;
        }
        Bukkit.broadcastMessage("§6[" + caster.getName() + "의 신민] §f" + hit + "명이 1시간 동안 강해진다.");
    }

    private void rainBlessing(Player caster) {
        caster.getWorld().setStorm(true);
        caster.getWorld().setWeatherDuration(12000);
        Bukkit.broadcastMessage("§3[강우] §f신의 가호로 비가 내린다.");
    }

    private void judgment(Player caster) {
        int hit = 0;
        for (Entity e : caster.getNearbyEntities(30, 30, 30)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                le.damage(200, caster);
                le.getWorld().strikeLightningEffect(le.getLocation());
                hit++;
            }
        }
        caster.sendMessage("§e§l심판 — " + hit + "체 처단");
    }

    private void pillarOfFire(Player caster) {
        Location target = caster.getTargetBlockExact(64) != null
                ? caster.getTargetBlockExact(64).getLocation() : caster.getLocation();
        target.getWorld().spawnParticle(Particle.LAVA, target.add(0, 5, 0), 200, 5, 10, 5);
        target.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
        for (Entity e : target.getWorld().getNearbyEntities(target, 20, 20, 20)) {
            if (e instanceof LivingEntity le && !(e instanceof Player p && p == caster)) {
                le.damage(150, caster);
                le.setFireTicks(200);
            }
        }
    }

    private void divineIntervention(Player caster) {
        Bukkit.broadcastMessage("§6§l[신탁] §f" + caster.getName()
                + "이(가) 진행중 사건에 신적 개입을 행한다.");
        // RebornWorldAI / RebornQuest와의 실제 연동은 이벤트로
        try {
            org.bukkit.event.Event ev = new kr.reborn.core.event.RebornStatChangeEvent(
                    caster, kr.reborn.core.data.StatType.DIVINITY, 0, 0);
            Bukkit.getPluginManager().callEvent(ev);
        } catch (Throwable ignored) {}
    }

    private void avatarDescent(Player caster) {
        caster.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 36000, 4));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 36000, 3));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 36000, 2));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 36000, 2));
        var a = caster.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (a != null) { a.setBaseValue(a.getValue() * 3); caster.setHealth(a.getValue()); }
        Bukkit.broadcastMessage("§5§l[화신 강림] §f" + caster.getName() + "이(가) 본체로 강림한다!");
    }

    private void resurrect(Player caster, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isDead()) {
            caster.sendMessage("§c부활할 대상이 사망 상태가 아니다."); return;
        }
        target.spigot().respawn();
        target.setHealth(20);
        target.getWorld().spawnParticle(Particle.TOTEM, target.getLocation().add(0, 1, 0), 50);
    }

    private void conceptDominion(Player caster, God g) {
        if (g.domainWorld.isEmpty()) {
            caster.sendMessage("§c먼저 신역(/god domain create)을 만들어야 한다."); return;
        }
        // 신역 안에서 절대 법칙 — 시간 정지·자기 강제 회복
        var w = Bukkit.getWorld(g.domainWorld);
        if (w != null) {
            w.setTime(6000);
            w.setStorm(false);
        }
        caster.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 12000, 9));
        caster.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 12000, 9));
        Bukkit.broadcastMessage("§0§l[개념 권능] §f" + caster.getName() + "의 신역이 절대 법칙에 잠겼다.");
    }

    private void rewriteReality(Player caster) {
        Bukkit.broadcastMessage("§6§l§n[현실 재기록] §r§f" + caster.getName()
                + "이(가) 한 사건의 결과를 바꿨다. 세계가 진동한다.");
        // 1회 한정 — 실제 효과는 관리자 조작 또는 사건별 분기 처리
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6현실이 다시 쓰인다", "§f" + caster.getName() + "의 의지", 20, 100, 40);
        }
    }

    private Player pickTarget(Player caster, String name) {
        if (name == null || name.isEmpty()) return caster;
        Player t = Bukkit.getPlayerExact(name);
        return t != null ? t : caster;
    }
}
