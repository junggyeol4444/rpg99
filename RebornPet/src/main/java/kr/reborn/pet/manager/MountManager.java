package kr.reborn.pet.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MountManager {

    private final RebornPet plugin;
    /** uuid → 현재 소환된 탈것 entity id. 1인 1탈것. */
    private final Map<UUID, UUID> activeMount = new ConcurrentHashMap<>();

    public MountManager(RebornPet p) { this.plugin = p; }

    public boolean summon(Player p, String mountId) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("mounts." + mountId);
        if (s == null) { Msg.error(p, "탈것 정의 없음: " + mountId); return false; }
        if (s.getBoolean("night-only", false)) {
            long t = p.getWorld().getTime();
            if (!(t >= 13000 && t <= 23000)) {
                Msg.warn(p, "밤에만 소환 가능"); return false;
            }
        }
        // 연료 — config의 fuel은 RebornEconomy 통화 ID (예: MAGITECH_CORE, CREDIT)
        String fuel = s.getString("fuel", null);
        if (fuel != null && !fuel.isEmpty()) {
            long cost = s.getLong("fuel-cost", 10);
            if (!withdrawFuel(p, fuel, cost)) {
                Msg.error(p, "연료 부족: " + fuel + " ×" + cost);
                return false;
            }
            Msg.send(p, "&7연료 소비: " + fuel + " ×" + cost);
        }
        // 기존 탈것 자동 회수 — 무한 소환으로 entity 양산되던 버그 방지
        dismiss(p);

        EntityType type;
        try { type = EntityType.valueOf(s.getString("entity", "HORSE")); }
        catch (Exception e) { Msg.error(p, "엔티티 잘못됨"); return false; }
        var ent = p.getWorld().spawnEntity(p.getLocation(), type);
        if (ent instanceof LivingEntity le) {
            var attr = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(s.getDouble("speed", 1.5) * 0.25);
            le.setCustomName("§6" + mountId);
            le.setCustomNameVisible(true);
            le.setPersistent(false);
            // 말 계열은 길들이기 + 안장 — 없으면 플레이어가 조종 불가
            if (le instanceof AbstractHorse horse) {
                horse.setTamed(true);
                horse.setOwner(p);
                try { horse.getInventory().setSaddle(new ItemStack(Material.SADDLE)); }
                catch (Throwable ignored) {}
            }
            if (le instanceof org.bukkit.entity.Tameable tame && !(le instanceof AbstractHorse)) {
                tame.setTamed(true);
                tame.setOwner(p);
            }
            ent.addPassenger(p);
            activeMount.put(p.getUniqueId(), ent.getUniqueId());
        }
        Msg.send(p, "&6탈것 소환: " + mountId);
        return true;
    }

    /** 현재 탈것 회수. */
    public void dismiss(Player p) {
        UUID prev = activeMount.remove(p.getUniqueId());
        if (prev == null) return;
        Entity e = Bukkit.getEntity(prev);
        if (e != null && !e.isDead()) e.remove();
    }

    private boolean withdrawFuel(Player p, String currency, long amount) {
        try {
            var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep == null) return true;  // 경제 플러그인 없으면 무료
            Object cm = ep.getClass().getMethod("currencies").invoke(ep);
            Object res = cm.getClass().getMethod("withdraw",
                            UUID.class, String.class, long.class)
                    .invoke(cm, p.getUniqueId(), currency, amount);
            return Boolean.TRUE.equals(res);
        } catch (Throwable t) { return true; }
    }
}
