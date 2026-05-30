package kr.reborn.spawn.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.spawn.RebornSpawn;
import kr.reborn.spawn.race.Race;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RaceCommand implements CommandExecutor {

    private final RebornSpawn plugin;
    public RaceCommand(RebornSpawn plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            if (!(s instanceof Player p)) return true;
            Race r = plugin.races().raceOf(p.getUniqueId());
            if (r == null) Msg.send(p, "&7종족 미배정.");
            else Msg.send(p, "&6내 종족: §e" + r.koreanName + " §7(" + r.name() + ")");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                if (a.length < 2) {
                    Msg.send(s, "&7/race list <WORLD>");
                    return true;
                }
                WorldKey w;
                try { w = WorldKey.valueOf(a[1].toUpperCase()); }
                catch (Exception e) { Msg.error(s, "잘못된 세계."); return true; }
                Msg.send(s, "&6=== " + w + " 가능 종족 ===");
                for (Race r : Race.availableFor(w)) {
                    s.sendMessage("§7• §e" + r.koreanName + " §8(" + r.name() + ") §7가중치 " + r.weight);
                }
            }
            case "info" -> {
                if (a.length < 2) { Msg.warn(s, "/race info <RACE>"); return true; }
                try {
                    Race r = Race.valueOf(a[1].toUpperCase());
                    Msg.send(s, "&6=== " + r.koreanName + " ===");
                    s.sendMessage("§7가중치: " + r.weight + (r.weight <= 0.1 ? " §c(희귀)" : ""));
                    s.sendMessage("§7보너스: §e" + r.bonus);
                } catch (Exception e) { Msg.error(s, "잘못된 종족."); }
            }
            case "set" -> {
                if (!s.hasPermission("rebornspawn.admin")) { Msg.error(s, "권한 없음."); return true; }
                if (a.length < 3) { Msg.warn(s, "/race set <player> <RACE>"); return true; }
                Player tgt = Bukkit.getPlayerExact(a[1]);
                if (tgt == null) { Msg.error(s, "오프라인."); return true; }
                try {
                    Race r = Race.valueOf(a[2].toUpperCase());
                    plugin.races().setRace(tgt, r);
                    Msg.send(s, "&a적용 완료.");
                } catch (Exception e) { Msg.error(s, "잘못된 종족."); }
            }
            case "all" -> {
                Msg.send(s, "&6=== 36 종족 전체 ===");
                for (Race r : Race.values()) {
                    s.sendMessage("§7• §e" + r.koreanName + " §8(" + r.name() + ")");
                }
            }
            default -> Msg.warn(s, "/race [list <WORLD> | info <RACE> | set <player> <RACE> | all]");
        }
        return true;
    }
}
