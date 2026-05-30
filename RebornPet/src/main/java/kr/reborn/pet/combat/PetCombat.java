package kr.reborn.pet.combat;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import kr.reborn.pet.data.Pet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * 펫 전투 동작.
 *
 * - 주인이 데미지를 받으면 ATTACK·DEFEND 모드 펫이 공격자를 공격.
 * - 펫이 공격하는 대상이 죽으면 펫에게 XP +.
 * - 주인이 공격하는 대상은 ATTACK 모드 펫도 같이 공격 (협공).
 * - 펫 데미지: 펫 레벨 × bond 보너스.
 * - 5초마다 펫에게 짧은 강화 효과 부여 (passive).
 */
public final class PetCombat implements Listener {

    private final RebornPet plugin;

    public PetCombat(RebornPet plugin) {
        this.plugin = plugin;
        // 매 5초마다 활성 펫에 패시브 효과
        RebornCore.get().scheduler().runTimer(this::tickPassives, 100L, 100L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        // 주인 피격 시 펫 반격
        if (e.getEntity() instanceof Player owner) {
            for (Pet pp : plugin.pets().petsOf(owner.getUniqueId())) {
                if (pp.activeEntityId == null) continue;
                if (pp.mode != Pet.Mode.ATTACK && pp.mode != Pet.Mode.DEFEND) continue;
                Entity petEntity = Bukkit.getEntity(pp.activeEntityId);
                if (!(petEntity instanceof LivingEntity petLE) || petLE.isDead()) continue;
                if (e.getDamager() instanceof LivingEntity target) {
                    double dmg = petDamage(pp);
                    try { target.damage(dmg, petLE); } catch (Throwable ignored) {}
                }
            }
        }
        // 펫이 공격: XP 누적 (대상이 죽으면 onDeath 핸들러가 처리)
        if (e.getDamager() instanceof LivingEntity dle) {
            Pet pp = findPetByEntity(dle.getUniqueId());
            if (pp != null) {
                // 협공 보너스
                e.setDamage(e.getDamage() + petDamage(pp) * 0.3);
            }
        }
        // 주인이 공격하면 ATTACK 모드 펫이 협공
        if (e.getDamager() instanceof Player attacker) {
            if (!(e.getEntity() instanceof LivingEntity target)) return;
            for (Pet pp : plugin.pets().petsOf(attacker.getUniqueId())) {
                if (pp.activeEntityId == null) continue;
                if (pp.mode != Pet.Mode.ATTACK) continue;
                Entity petEntity = Bukkit.getEntity(pp.activeEntityId);
                if (!(petEntity instanceof LivingEntity petLE) || petLE.isDead()) continue;
                double dmg = petDamage(pp);
                try { target.damage(dmg, petLE); } catch (Throwable ignored) {}
            }
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        var killer = e.getEntity().getKiller();
        if (killer != null) return; // 플레이어가 직접 죽인 경우는 PlayerExperienceEvent가 처리
        // 펫이 죽인 대상 → XP
        if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent ev) {
            if (ev.getDamager() instanceof LivingEntity petEntity) {
                Pet pp = findPetByEntity(petEntity.getUniqueId());
                if (pp != null) {
                    long xp = (long)(e.getEntity().getMaxHealth() / 2);
                    plugin.pets().addXp(pp, xp);
                }
            }
        }
    }

    private double petDamage(Pet pp) {
        return pp.level * 0.5 + pp.bond * 0.1;
    }

    private Pet findPetByEntity(UUID entityId) {
        for (var owner : plugin.pets().byOwnerSnapshot().entrySet()) {
            for (Pet pp : owner.getValue()) {
                if (entityId.equals(pp.activeEntityId)) return pp;
            }
        }
        return null;
    }

    private void tickPassives() {
        for (var entry : plugin.pets().byOwnerSnapshot().entrySet()) {
            for (Pet pp : entry.getValue()) {
                if (pp.activeEntityId == null) continue;
                Entity e = Bukkit.getEntity(pp.activeEntityId);
                if (!(e instanceof LivingEntity le) || le.isDead()) continue;
                int amp = Math.min(3, pp.level / 25);
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 120, amp, true, false));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 120, amp, true, false));
                    if (pp.bond >= 80) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, true, false));
                    }
                } catch (Throwable ignored) {}
            }
        }
    }
}
