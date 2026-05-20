package kr.reborn.mob.listener;

import kr.reborn.core.util.Rand;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.def.MobDef;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class MobListener implements Listener {

    private final RebornMob plugin;

    public MobListener(RebornMob p) { this.plugin = p; }

    private MobDef getDef(LivingEntity e) {
        String id = e.getPersistentDataContainer().get(new NamespacedKey(plugin, "rmob"), PersistentDataType.STRING);
        return id == null ? null : plugin.registry().get(id);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity le)) return;
        MobDef def = getDef(le);
        if (def == null) return;
        if (def.boss && e.getDamager() instanceof Player p) {
            plugin.bosses().onDamage(le, p, e.getFinalDamage());
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        MobDef def = getDef(le);
        if (def == null) return;
        e.getDrops().clear();
        for (var d : def.drops) {
            if (Rand.chance(d.chance)) {
                Material m;
                try { m = Material.valueOf(d.item); } catch (Exception ex) { continue; }
                int amt = Rand.range(d.min, d.max);
                e.getDrops().add(new ItemStack(m, amt));
            }
        }
        if (def.boss) plugin.bosses().onDeath(le);
    }
}
