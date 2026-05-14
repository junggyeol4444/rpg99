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
        }
    }
}
