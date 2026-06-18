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

    private static final String NS = "RebornPet.pets";

    private final RebornPet plugin;
    private final Map<UUID, List<Pet>> byOwner = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public PetManager(RebornPet p) { this.plugin = p; }

    private void ensureLoaded(UUID owner) {
        if (loaded.add(owner)) {
            var all = RebornCore.get().kv().loadAll(NS, owner);
            List<Pet> list = new ArrayList<>();
            for (var e : all.entrySet()) {
                try {
                    String[] parts = e.getValue().split("\\|", -1);
                    if (parts.length < 6) continue;
                    Pet pp = new Pet(owner, parts[0], parts[1]);
                    pp.level = Integer.parseInt(parts[2]);
                    pp.xp = Long.parseLong(parts[3]);
                    pp.bond = Integer.parseInt(parts[4]);
                    try { pp.mode = Pet.Mode.valueOf(parts[5]); }
                    catch (Throwable ignored) { pp.mode = Pet.Mode.FOLLOW; }
                    list.add(pp);
                } catch (Throwable ignored) {}
            }
            if (!list.isEmpty()) byOwner.put(owner, list);
        }
    }

    private void persist(Pet pp) {
        String encoded = pp.name + "|" + pp.mobId + "|" + pp.level + "|"
                + pp.xp + "|" + pp.bond + "|" + pp.mode.name();
        RebornCore.get().kv().put(NS, pp.owner, pp.id.toString(), encoded);
    }

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
            ensureLoaded(p.getUniqueId());
            Pet pet = new Pet(p.getUniqueId(), target.getType().name() + "_pet", target.getType().name());
            byOwner.computeIfAbsent(p.getUniqueId(), x -> new ArrayList<>()).add(pet);
            persist(pet);
            // 몬스터 종류별 고유 길들이기 메시지
            String successMsg = tameMessage(target.getType().name());
            target.remove();
            Msg.send(p, successMsg);
            try {
                p.getWorld().spawnParticle(org.bukkit.Particle.HEART,
                        p.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } catch (Throwable ignored) {}
            return true;
        }
        // 광폭화 — 종별로 메시지 다르게
        target.setHealth(Math.min(target.getMaxHealth(), target.getHealth() * 2));
        Msg.error(p, ragingMessage(target.getType().name()));
        return false;
    }

    /** 몬스터 종별 길들이기 성공 메시지. */
    private String tameMessage(String mobType) {
        switch (mobType) {
            case "WOLF": return "&a늑대가 너의 명에 복종한다 — 충실한 동반자.";
            case "CAT": case "OCELOT": return "&a고양이가 다가와 너의 다리에 몸을 부빈다.";
            case "PARROT": return "&a앵무새가 너의 어깨에 내려앉았다.";
            case "FOX": return "&a여우가 너의 손을 핥는다 — 영물이 너를 받아들였다.";
            case "DOLPHIN": return "&a돌고래가 너에게 노래한다 — 바다의 친구.";
            case "AXOLOTL": return "&a&l새끼 드래곤이 너를 어미로 인식한다.";
            case "POLAR_BEAR": return "&a&l쇠가죽 곰이 너의 강함을 인정했다.";
            case "BLAZE": return "&c화염 정령이 너의 단호함에 무릎을 꿇었다.";
            case "GUARDIAN": return "&3해양 수호자가 너를 인정한다 — 바다의 일원.";
            case "IRON_GOLEM": return "&7대지 정령 — 골렘이 너를 주인으로 받아들였다.";
            case "WITHER_SKELETON": return "&8&l죽음의 영혼이 너를 따른다.";
            case "PIGLIN": case "PIGLIN_BRUTE": return "&c피글린이 무릎을 꿇었다 — 강함을 인정.";
            case "ZOMBIE": return "&2언데드가 너의 명을 받들겠다.";
            case "SLIME": case "MAGMA_CUBE": return "&a슬라임이 흔들거리며 따라온다.";
            case "PHANTOM": return "&8&l환영이 너의 그림자가 되었다.";
            case "ALLAY": return "&b&l정령이 노래하며 너에게 다가왔다.";
            case "VEX": return "&5요괴가 너에게 굴복했다.";
            case "GLOW_SQUID": return "&b심해의 발광 오징어 — 빛으로 너를 따른다.";
            case "WARDEN": return "&0&l심연의 수호자 — 그 깊은 어둠을 너에게 맡겼다.";
            case "ENDER_DRAGON": return "&5&l용이 너를 주인으로 인정했다 — 전설적인 순간.";
            default: return "&a길들이기 성공 — " + mobType + " 이(가) 너를 따른다.";
        }
    }

    /** 몬스터 종별 길들이기 실패 (광폭화) 메시지. */
    private String ragingMessage(String mobType) {
        switch (mobType) {
            case "WOLF": return "늑대가 분노한다 — 이빨을 드러낸다!";
            case "CAT": case "OCELOT": return "고양이가 발톱을 세웠다.";
            case "FOX": return "여우가 너의 손을 물었다.";
            case "ENDER_DRAGON": return "용이 분노했다 — 도망쳐라!";
            case "WARDEN": return "심연의 수호자가 너의 거짓을 보았다.";
            case "POLAR_BEAR": return "곰이 너에게 돌진한다.";
            case "BLAZE": return "화염 정령이 분노 — 불이 사방으로 튄다.";
            default: return mobType + " 가 광폭화했다 — 도망쳐라!";
        }
    }

    public List<Pet> petsOf(UUID owner) {
        ensureLoaded(owner);
        return byOwner.getOrDefault(owner, List.of());
    }

    public Pet byName(UUID owner, String name) {
        for (Pet pp : petsOf(owner)) if (pp.name.equalsIgnoreCase(name)) return pp;
        return null;
    }

    public boolean summon(Player owner, String name) {
        Pet pp = byName(owner.getUniqueId(), name);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        // 이미 소환된 펫 — 중복 스폰으로 orphan 몹이 생기던 버그 방지
        if (pp.activeEntityId != null && Bukkit.getEntity(pp.activeEntityId) != null) {
            Msg.warn(owner, "이미 소환된 펫이다.");
            return false;
        }
        // 동시 활성 수 제한 — 기본 1마리, 절대자(총합 5000+)는 3마리
        double total = RebornCore.get().api().getTotalStats(owner.getUniqueId());
        int maxActive = total >= 5000
                ? plugin.getConfig().getInt("pet.max-active-absolute", 3)
                : plugin.getConfig().getInt("pet.max-active", 1);
        int active = 0;
        for (Pet other : petsOf(owner.getUniqueId())) {
            if (other.activeEntityId != null && Bukkit.getEntity(other.activeEntityId) != null) active++;
        }
        if (active >= maxActive) {
            Msg.error(owner, "동시 소환 한도 " + maxActive + "마리 — 먼저 /pet dismiss");
            return false;
        }
        try {
            var t = org.bukkit.entity.EntityType.valueOf(pp.mobId);
            var ent = owner.getWorld().spawnEntity(owner.getLocation(), t);
            ent.setCustomName(pp.name);
            ent.setCustomNameVisible(true);
            // 길들임 — 주인을 공격하지 않도록
            if (ent instanceof org.bukkit.entity.Tameable tame) {
                tame.setTamed(true);
                tame.setOwner(owner);
            }
            ent.setPersistent(true);
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
        persist(pet);
    }

    /** 펫 진화 — 특정 레벨 도달 시 mobId 변경. 다단계 체인(예: AXOLOTL 25→PUFFERFISH 60→GUARDIAN) 지원. */
    public void tryEvolve(Pet pet) {
        var evos = plugin.getConfig().getConfigurationSection("pet.evolutions");
        if (evos == null) return;
        // 현재 mobId가 속한 진화 체인의 base 키 탐색 — 직접 키이거나, 어떤 체인의 to 값
        // (1차 진화 후 mobId가 바뀌면 base 키와 안 맞아 2차 진화가 영원히 안 되던 버그 수정)
        String base = null;
        outer:
        for (String fromMob : evos.getKeys(false)) {
            if (pet.mobId.equalsIgnoreCase(fromMob)) { base = fromMob; break; }
            for (var e : evos.getMapList(fromMob)) {
                if (pet.mobId.equalsIgnoreCase(String.valueOf(e.get("to")))) { base = fromMob; break outer; }
            }
        }
        if (base == null) return;
        // 자격 되는 가장 높은 단계 선택
        String targetMob = null;
        int bestLevel = -1;
        for (var e : evos.getMapList(base)) {
            int reqLevel = ((Number) e.getOrDefault("level", 999)).intValue();
            if (pet.level >= reqLevel && reqLevel > bestLevel) {
                bestLevel = reqLevel;
                targetMob = String.valueOf(e.get("to"));
            }
        }
        if (targetMob == null || targetMob.equalsIgnoreCase(pet.mobId)) return;
        pet.mobId = targetMob;
        pet.bond = Math.min(100, pet.bond + 20);
        Bukkit.broadcastMessage("§5§l[펫 진화] §f" + pet.name + " §7→ " + targetMob);
        if (pet.activeEntityId != null) {
            var ent = Bukkit.getEntity(pet.activeEntityId);
            if (ent != null) {
                var loc = ent.getLocation();
                ent.remove();
                try {
                    var t = org.bukkit.entity.EntityType.valueOf(targetMob);
                    var newEnt = ent.getWorld().spawnEntity(loc, t);
                    newEnt.setCustomName(pet.name);
                    newEnt.setCustomNameVisible(true);
                    pet.activeEntityId = newEnt.getUniqueId();
                } catch (Throwable ignored) {}
            }
        }
    }

    public boolean feed(Player owner, String petName, org.bukkit.Material food) {
        Pet pp = byName(owner.getUniqueId(), petName);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        // contains+removeItem race 제거 — removeItem이 원자적으로 보유량 보고 처리.
        var leftover = owner.getInventory().removeItem(new org.bukkit.inventory.ItemStack(food, 1));
        if (!leftover.isEmpty()) {
            Msg.error(owner, "먹이 부족: " + food);
            return false;
        }
        int bondGain = 5;
        long xpGain = 50;
        var pref = plugin.getConfig().getString("pet.preferred-food." + pp.mobId);
        if (pref != null && pref.equalsIgnoreCase(food.name())) {
            bondGain = 15;
            xpGain = 150;
            Msg.send(owner, "&a선호 먹이! 효과 ×3");
        }
        pp.bond = Math.min(100, pp.bond + bondGain);
        addXp(pp, xpGain);  // addXp 안에서 persist
        Msg.send(owner, "&6먹이 주기 — bond +" + bondGain + " (총 " + pp.bond + ")");
        return true;
    }

    public boolean rename(Player owner, String oldName, String newName) {
        Pet pp = byName(owner.getUniqueId(), oldName);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        pp.name = newName;
        var ent = pp.activeEntityId != null ? Bukkit.getEntity(pp.activeEntityId) : null;
        if (ent != null) ent.setCustomName(newName);
        persist(pp);
        Msg.send(owner, "&a이름 변경: " + newName);
        return true;
    }

    public boolean setMode(Player owner, String petName, Pet.Mode mode) {
        Pet pp = byName(owner.getUniqueId(), petName);
        if (pp == null) { Msg.error(owner, "해당 펫 없음"); return false; }
        pp.mode = mode;
        persist(pp);
        Msg.send(owner, "&a모드: " + mode);
        return true;
    }

    public Map<UUID, List<Pet>> byOwnerSnapshot() {
        return new java.util.HashMap<>(byOwner);
    }
}
