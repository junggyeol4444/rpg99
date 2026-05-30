package kr.reborn.ship.combat;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.ship.RebornShip;
import kr.reborn.ship.data.Ship;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 해전 시스템.
 *
 * - 함포 발사: 헬름에서 정면으로 TNT 발사 (cooldown 4초).
 * - 충돌 데미지: 다른 배에 5블록 이내 진입 시 충각 데미지.
 * - 해적 깃발: PvP 면책 (전투 모드 진입 시).
 * - 함선 침몰: hp 0 → SUNK + 보물 드롭 + 위치에 잔해 파티클.
 */
public final class ShipCombat {

    private final RebornShip plugin;
    /** shipId → 마지막 함포 시각 */
    private final Map<UUID, Long> cannonCooldown = new ConcurrentHashMap<>();

    public ShipCombat(RebornShip plugin) {
        this.plugin = plugin;
        // 매 2초마다 충돌 체크
        RebornCore.get().scheduler().runTimer(this::tickCollision, 40L, 40L);
    }

    /** 함포 발사 — 헬름에서 정면 TNT. */
    public boolean fireCannon(Player p, Ship ship) {
        long now = System.currentTimeMillis();
        long ready = cannonCooldown.getOrDefault(ship.id, 0L);
        if (now < ready) {
            Msg.warn(p, "함포 쿨다운 " + ((ready - now) / 1000) + "초 남음.");
            return false;
        }
        cannonCooldown.put(ship.id, now + 4000L);
        Location loc = ship.helm;
        if (loc == null) return false;
        Vector dir = p.getLocation().getDirection().setY(0).normalize().multiply(2);
        Location fireFrom = loc.clone().add(dir);
        try {
            TNTPrimed tnt = loc.getWorld().spawn(fireFrom, TNTPrimed.class);
            tnt.setVelocity(dir.multiply(2));
            tnt.setFuseTicks(40);
            tnt.setSource(p);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, fireFrom, 30, 0.5, 0.5, 0.5);
        } catch (Throwable t) {
            Msg.error(p, "함포 발사 실패.");
            return false;
        }
        ship.state = Ship.State.COMBAT;
        return true;
    }

    /** 다른 배에게 데미지. */
    public void onShipHit(Ship target, double damage, Player attacker) {
        target.hp -= damage;
        if (target.hp <= 0) {
            sinkShip(target, attacker);
            return;
        }
        target.state = Ship.State.COMBAT;
        Player owner = Bukkit.getPlayer(target.owner);
        if (owner != null) {
            Msg.warn(owner, "&c함선 피격! HP " + (int) target.hp + " / " + (int) target.maxHp);
        }
    }

    private void sinkShip(Ship s, Player attacker) {
        s.state = Ship.State.SUNK;
        Bukkit.broadcastMessage("§3§l[해전] §f"
                + (attacker != null ? attacker.getName() + " §c가 " : "")
                + s.name + " 함선을 침몰시켰다!");
        try {
            if (s.helm != null) {
                s.helm.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, s.helm, 10, 5, 2, 5);
                // 보물 드롭
                s.helm.getWorld().dropItem(s.helm,
                        new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_BLOCK,
                                Math.max(1, s.grade * 2)));
            }
        } catch (Throwable ignored) {}
        // 해적왕 후예에게 보고 (RebornStat OceanGrowth hook)
        if (attacker != null) {
            try {
                var sp = Bukkit.getPluginManager().getPlugin("RebornStat");
                if (sp != null) {
                    Object growth = sp.getClass().getMethod("growth").invoke(sp);
                    Object ocean = growth.getClass().getMethod("of",
                                    kr.reborn.core.data.WorldKey.class)
                            .invoke(growth, kr.reborn.core.data.WorldKey.OCEAN);
                    if (ocean != null) {
                        ocean.getClass().getMethod("onShipCapture", Player.class)
                                .invoke(ocean, attacker);
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private void tickCollision() {
        var ships = plugin.ships().all();
        for (int i = 0; i < ships.size(); i++) {
            Ship a = ships.get(i);
            if (a.state == Ship.State.SUNK || a.helm == null) continue;
            for (int j = i + 1; j < ships.size(); j++) {
                Ship b = ships.get(j);
                if (b.state == Ship.State.SUNK || b.helm == null) continue;
                if (a.helm.getWorld() != b.helm.getWorld()) continue;
                if (a.helm.distance(b.helm) < 8) {
                    // 충각
                    double dmg = Math.min(a.grade, b.grade) * 5;
                    onShipHit(a, dmg, null);
                    onShipHit(b, dmg, null);
                }
            }
        }
    }
}
