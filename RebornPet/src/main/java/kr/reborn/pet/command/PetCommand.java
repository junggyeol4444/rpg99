package kr.reborn.pet.command;

import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import kr.reborn.pet.data.Pet;
import org.bukkit.Material;
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
            Msg.send(p, "&7/pet tame                      - 근처 약체 대상 길들이기");
            Msg.send(p, "&7/pet summon <name>             - 펫 소환");
            Msg.send(p, "&7/pet dismiss <name>            - 펫 귀환");
            Msg.send(p, "&7/pet list                      - 펫 목록 + 정보");
            Msg.send(p, "&7/pet mode <name> <ATK|DEF|FOL|STAY> - 모드 설정");
            Msg.send(p, "&7/pet feed <name> <material>    - 먹이 주기");
            Msg.send(p, "&7/pet rename <old> <new>        - 이름 변경");
            Msg.send(p, "&7/pet contract <id>             - 인간 NPC와 계약");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "tame" -> {
                Entity tg = nearestLE(p);
                if (tg instanceof LivingEntity le) plugin.pets().tryTame(p, le);
                else Msg.error(p, "근처에 대상 없음");
            }
            case "summon" -> {
                if (a.length < 2) return true;
                plugin.pets().summon(p, a[1]);
            }
            case "dismiss" -> {
                if (a.length < 2) return true;
                plugin.pets().dismiss(p.getUniqueId(), a[1]);
                Msg.send(p, "&7펫 귀환");
            }
            case "list" -> {
                Msg.send(p, "&6=== 내 펫 (" + plugin.pets().petsOf(p.getUniqueId()).size() + ") ===");
                for (Pet pp : plugin.pets().petsOf(p.getUniqueId())) {
                    String active = pp.activeEntityId != null ? " §a[활성]" : "";
                    p.sendMessage("§e" + pp.name + " §7(" + pp.mobId + ") Lv " + pp.level
                            + " xp " + pp.xp + " 유대 " + pp.bond + " 모드 " + pp.mode + active);
                }
            }
            case "mode" -> {
                if (a.length < 3) { Msg.warn(p, "/pet mode <name> <ATK|DEF|FOL|STAY>"); return true; }
                Pet.Mode mode = switch (a[2].toUpperCase()) {
                    case "ATK", "ATTACK" -> Pet.Mode.ATTACK;
                    case "DEF", "DEFEND" -> Pet.Mode.DEFEND;
                    case "FOL", "FOLLOW" -> Pet.Mode.FOLLOW;
                    case "STAY" -> Pet.Mode.STAY;
                    default -> Pet.Mode.FOLLOW;
                };
                plugin.pets().setMode(p, a[1], mode);
            }
            case "feed" -> {
                if (a.length < 3) { Msg.warn(p, "/pet feed <name> <material>"); return true; }
                Material m = Material.matchMaterial(a[2]);
                if (m == null) { Msg.error(p, "잘못된 material."); return true; }
                plugin.pets().feed(p, a[1], m);
            }
            case "rename" -> {
                if (a.length < 3) { Msg.warn(p, "/pet rename <old> <new>"); return true; }
                plugin.pets().rename(p, a[1], a[2]);
            }
            case "contract" -> {
                if (a.length < 2) return true;
                plugin.contracts().propose(p, a[1], kr.reborn.pet.manager.ContractManager.Grade.HALF);
            }
            default -> Msg.warn(p, "알 수 없는 하위 명령.");
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
