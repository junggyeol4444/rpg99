package kr.reborn.pet.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.pet.RebornPet;
import kr.reborn.pet.data.Pet;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PetManager {

    private final RebornPet plugin;
    private final Map<UUID, List<Pet>> byOwner = new ConcurrentHashMap<>();

    public PetManager(RebornPet p) { this.plugin = p; }

    public boolean tryTame(Player p, LivingEntity target) {
        double percent = plugin.getConfig().getDouble("tame.hp-threshold-percent", 10) / 100.0;
        if (target.getHealth() / target.getMaxHealth() > percent) {
            Msg.warn(p, "체력이 너무 높다 (10% 이하 필요).");
            return false;
        }
        double base = plugin.getConfig().getDouble("tame.base-success", 0.30);
        double charisma = RebornCore.get().api().getStat(p.getUniqueId(), StatType.CHARISMA);
        double chance = base + charisma * plugin.getConfig().getDouble("tame.charisma-bonus", 0.005);
        if (Rand.chance(chance)) {
            Pet pet = new Pet(p.getUniqueId(), target.getType().name() + "_pet", target.getType().name());
            byOwner.computeIfAbsent(p.getUniqueId(), x -> new ArrayList<>()).add(pet);
            target.remove();
            Msg.send(p, "&a길들이기 성공!");
            return true;
        }
        // 광폭화
        target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() * 2));
        Msg.error(p, "길들이기 실패 — 광폭화!");
        return false;
    }

    public List<Pet> petsOf(UUID owner) {
        return byOwner.getOrDefault(owner, List.of());
    }

    public Pet byName(UUID owner, String name) {
        for (Pet pp : petsOf(owner)) if (pp.name.equalsIgnoreCase(name)) return pp;
        return null;
    }

    public boolean summon(Player owner, String name) {
        Pet pp = byName(owner.getUniqueId(), name);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        try {
            var t = org.bukkit.entity.EntityType.valueOf(pp.mobId);
            var ent = owner.getWorld().spawnEntity(owner.getLocation(), t);
            ent.setCustomName(pp.name);
            ent.setCustomNameVisible(true);
            pp.activeEntityId = ent.getUniqueId();
            Msg.send(owner, "&a펫 소환: " + pp.name);
            return true;
        } catch (Exception e) {
            Msg.error(owner, "소환 실패");
            return false;
        }
    }

    public void dismiss(UUID owner, String name) {
        Pet pp = byName(owner, name);
        if (pp == null || pp.activeEntityId == null) return;
        var e = Bukkit.getEntity(pp.activeEntityId);
        if (e != null) e.remove();
        pp.activeEntityId = null;
    }

    public void addXp(Pet pet, long xp) {
        pet.xp += xp;
        long need = pet.level * 100L;
        while (pet.xp >= need && pet.level < plugin.getConfig().getInt("pet.level-cap", 100)) {
            pet.xp -= need;
            pet.level++;
            need = pet.level * 100L;
            tryEvolve(pet);
        }
    }

    /** 펫 진화 — 특정 레벨 도달 시 mobId 변경. */
    public void tryEvolve(Pet pet) {
        var evos = plugin.getConfig().getConfigurationSection("pet.evolutions");
        if (evos == null) return;
        for (String fromMob : evos.getKeys(false)) {
            if (!pet.mobId.equalsIgnoreCase(fromMob)) continue;
            var entries = evos.getMapList(fromMob);
            for (var e : entries) {
                int reqLevel = ((Number) e.getOrDefault("level", 999)).intValue();
                if (pet.level >= reqLevel) {
                    String toMob = String.valueOf(e.get("to"));
                    pet.mobId = toMob;
                    pet.bond += 20;
                    Bukkit.broadcastMessage("§5§l[펫 진화] §f" + pet.name + " §7→ " + toMob);
                    if (pet.activeEntityId != null) {
                        var ent = Bukkit.getEntity(pet.activeEntityId);
                        if (ent != null) {
                            var loc = ent.getLocation();
                            ent.remove();
                            try {
                                var t = org.bukkit.entity.EntityType.valueOf(toMob);
                                var newEnt = ent.getWorld().spawnEntity(loc, t);
                                newEnt.setCustomName(pet.name);
                                newEnt.setCustomNameVisible(true);
                                pet.activeEntityId = newEnt.getUniqueId();
                            } catch (Throwable ignored) {}
                        }
                    }
                    break;
                }
            }
        }
    }

    public boolean feed(Player owner, String petName, org.bukkit.Material food) {
        Pet pp = byName(owner.getUniqueId(), petName);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        if (!owner.getInventory().contains(food)) {
            Msg.error(owner, "먹이 부족: " + food);
            return false;
        }
        owner.getInventory().removeItem(new org.bukkit.inventory.ItemStack(food, 1));
        int bondGain = 5;
        long xpGain = 50;
        var pref = plugin.getConfig().getString("pet.preferred-food." + pp.mobId);
        if (pref != null && pref.equalsIgnoreCase(food.name())) {
            bondGain = 15;
            xpGain = 150;
            Msg.send(owner, "&a선호 먹이! 효과 ×3");
        }
        pp.bond = Math.min(100, pp.bond + bondGain);
        addXp(pp, xpGain);
        Msg.send(owner, "&6먹이 주기 — bond +" + bondGain + " (총 " + pp.bond + ")");
        return true;
    }

    public boolean rename(Player owner, String oldName, String newName) {
        Pet pp = byName(owner.getUniqueId(), oldName);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        pp.name = newName;
        var ent = pp.activeEntityId != null ? Bukkit.getEntity(pp.activeEntityId) : null;
        if (ent != null) ent.setCustomName(newName);
        Msg.send(owner, "&a이름 변경: " + newName);
        return true;
    }

    public boolean setMode(Player owner, String petName, Pet.Mode mode) {
        Pet pp = byName(owner.getUniqueId(), petName);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        pp.mode = mode;
        Msg.send(owner, "&a모드: " + mode);
        return true;
    }

    public Map<UUID, List<Pet>> byOwnerSnapshot() {
        return new java.util.HashMap<>(byOwner);
    }
}
