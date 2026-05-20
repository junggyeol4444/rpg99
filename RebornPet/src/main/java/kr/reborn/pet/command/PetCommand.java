package kr.reborn.pet.command;

import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PetCommand implements CommandExecutor {
    private final RebornPet plugin;
    public PetCommand(RebornPet p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/pet tame | summon <name> | dismiss <name> | list | mode <m> | contract <id>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "tame":
                Entity tg = nearestLE(p);
                if (tg instanceof LivingEntity le) plugin.pets().tryTame(p, le);
                else Msg.error(p, "근처에 대상 없음");
                break;
            case "summon":
                if (a.length < 2) return true;
                plugin.pets().summon(p, a[1]);
                break;
            case "dismiss":
                if (a.length < 2) return true;
                plugin.pets().dismiss(p.getUniqueId(), a[1]);
                Msg.send(p, "&7펫 귀환");
                break;
            case "list":
                Msg.send(p, "&6펫 목록:");
                plugin.pets().petsOf(p.getUniqueId()).forEach(pp ->
                        p.sendMessage("§e" + pp.name + " §7Lv " + pp.level + " 유대 " + pp.bond));
                break;
            case "contract":
                if (a.length < 2) return true;
                plugin.contracts().propose(p, a[1], kr.reborn.pet.manager.ContractManager.Grade.HALF);
                break;
        }
        return true;
    }

    private Entity nearestLE(Player p) {
        Entity best = null;
        double dist = 25;
        for (Entity e : p.getNearbyEntities(5, 5, 5)) {
            if (!(e instanceof LivingEntity)) continue;
            double d = e.getLocation().distance(p.getLocation());
            if (d < dist) { dist = d; best = e; }
        }
        return best;
    }
}
