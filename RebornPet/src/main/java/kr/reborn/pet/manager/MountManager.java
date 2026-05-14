package kr.reborn.pet.manager;

import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class MountManager {

    private final RebornPet plugin;

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
        EntityType type;
        try { type = EntityType.valueOf(s.getString("entity", "HORSE")); }
        catch (Exception e) { Msg.error(p, "엔티티 잘못됨"); return false; }
        var ent = p.getWorld().spawnEntity(p.getLocation(), type);
        if (ent instanceof LivingEntity le) {
            var attr = le.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(s.getDouble("speed", 1.5) * 0.25);
            le.setCustomName("§6" + mountId);
            le.setCustomNameVisible(true);
            ent.addPassenger(p);
        }
        Msg.send(p, "&6탈것 소환: " + mountId);
        return true;
    }
}
