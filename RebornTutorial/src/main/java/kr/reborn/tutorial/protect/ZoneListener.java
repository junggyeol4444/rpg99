package kr.reborn.tutorial.protect;

import kr.reborn.core.util.Msg;
import kr.reborn.tutorial.RebornTutorial;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class ZoneListener implements Listener {

    private final RebornTutorial plugin;

    public ZoneListener(RebornTutorial p) { this.plugin = p; }

    public boolean isProtected(Location loc) {
        if (loc.getWorld() == null) return false;
        var name = loc.getWorld().getName();
        if (!name.startsWith("tutorial")) return false;
        var sec = plugin.getConfig().getConfigurationSection("protected-zones");
        if (sec == null) return false;
        for (String world : sec.getKeys(false)) {
            var zones = sec.getMapList(world);
            for (var z : zones) {
                if (!name.equalsIgnoreCase(String.valueOf(z.get("world")))) continue;
                var min = (java.util.Map<?, ?>) z.get("min");
                var max = (java.util.Map<?, ?>) z.get("max");
                if (loc.getX() >= ((Number) min.get("x")).doubleValue()
                        && loc.getX() <= ((Number) max.get("x")).doubleValue()
                        && loc.getY() >= ((Number) min.get("y")).doubleValue()
                        && loc.getY() <= ((Number) max.get("y")).doubleValue()
                        && loc.getZ() >= ((Number) min.get("z")).doubleValue()
                        && loc.getZ() <= ((Number) max.get("z")).doubleValue()) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof org.bukkit.entity.Player p)) return;
        if (!p.getWorld().getName().startsWith("tutorial")) return;
        if (isProtected(p.getLocation())) {
            e.setCancelled(true);
            Msg.warn(p, "보호 구역 안에서는 피해를 입지 않는다.");
        }
    }
}
