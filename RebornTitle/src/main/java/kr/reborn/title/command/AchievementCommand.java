package kr.reborn.title.command;

import kr.reborn.core.util.Msg;
import kr.reborn.title.RebornTitle;
import kr.reborn.title.achievement.Achievement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AchievementCommand implements CommandExecutor {
    private final RebornTitle plugin;
    public AchievementCommand(RebornTitle plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/achievement list                - 전체 업적 (50)");
            Msg.send(p, "&7/achievement earned              - 내 달성 업적");
            Msg.send(p, "&7/achievement progress            - 진척 중인 업적");
            Msg.send(p, "&7/achievement info <id>           - 업적 정보");
            Msg.send(p, "&7/achievement points              - 총 점수");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                Msg.send(p, "&6=== 업적 (" + plugin.achievements().all().size() + ") ===");
                int n = 0;
                for (Achievement def : plugin.achievements().all().values()) {
                    if (n++ >= 30) { p.sendMessage("§7… 추가 다수"); break; }
                    boolean earned = plugin.achievements().earnedOf(p.getUniqueId()).contains(def.id);
                    String mark = earned ? "§a✓" : "§7◌";
                    p.sendMessage(mark + " " + def.rarity.color + def.name
                            + " §7(" + def.rarity + ") " + def.description);
                }
            }
            case "earned" -> {
                var set = plugin.achievements().earnedOf(p.getUniqueId());
                Msg.send(p, "&6=== 달성 업적 (" + set.size() + ") ===");
                for (String id : set) {
                    Achievement def = plugin.achievements().all().get(id);
                    if (def != null) {
                        p.sendMessage("§a✓ " + def.rarity.color + def.name
                                + " §7+§e" + def.rarity.points);
                    }
                }
                Msg.send(p, "&6총 점수: §e" + plugin.achievements().pointsOf(p.getUniqueId()));
            }
            case "progress" -> {
                Msg.send(p, "&6=== 진척 중 ===");
                var pm = plugin.achievements().progressOf(p.getUniqueId());
                var earned = plugin.achievements().earnedOf(p.getUniqueId());
                for (var e : pm.entrySet()) {
                    if (earned.contains(e.getKey())) continue;
                    Achievement def = plugin.achievements().all().get(e.getKey());
                    if (def != null) {
                        p.sendMessage("§7• " + def.rarity.color + def.name
                                + " §f" + e.getValue() + "/" + def.requiredProgress);
                    }
                }
            }
            case "info" -> {
                if (a.length < 2) { Msg.warn(p, "/achievement info <id>"); return true; }
                Achievement def = plugin.achievements().all().get(a[1]);
                if (def == null) { Msg.error(p, "업적 없음."); return true; }
                Msg.send(p, "&6=== " + def.rarity.color + def.name + " &6===");
                p.sendMessage("§7카테고리: §f" + def.category);
                p.sendMessage("§7등급: " + def.rarity.color + def.rarity + " §7+§e" + def.rarity.points + "점");
                p.sendMessage("§7설명: §f" + def.description);
                p.sendMessage("§7필요 조건: §f" + def.requiredProgress);
                boolean earned = plugin.achievements().earnedOf(p.getUniqueId()).contains(def.id);
                p.sendMessage("§7상태: " + (earned ? "§a달성" : "§c미달성"));
            }
            case "points" -> {
                int total = plugin.achievements().pointsOf(p.getUniqueId());
                int n = plugin.achievements().earnedOf(p.getUniqueId()).size();
                Msg.send(p, "&6내 업적 점수: §e" + total + " §7(" + n + " 종 달성)");
            }
            default -> Msg.warn(p, "/achievement list|earned|progress|info|points");
        }
        return true;
    }
}
