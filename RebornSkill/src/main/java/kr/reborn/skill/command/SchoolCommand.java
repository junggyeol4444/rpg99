package kr.reborn.skill.command;

import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.school.MartialSchool;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SchoolCommand implements CommandExecutor {
    private final RebornSkill plugin;
    public SchoolCommand(RebornSkill plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            MartialSchool ms = plugin.schools().of(p.getUniqueId());
            Msg.send(p, "&6내 학파: " + (ms != null ? ms.colorCode + ms.koreanName : "&7무소속"));
            Msg.send(p, "&7/school join <ORTHODOX|UNORTHODOX|DEMON_CULT|IMPERIAL|HERMIT>");
            Msg.send(p, "&7/school list                - 5 학파 정보");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "join" -> {
                if (a.length < 2) { Msg.warn(p, "/school join <SCHOOL>"); return true; }
                try {
                    MartialSchool ms = MartialSchool.valueOf(a[1].toUpperCase());
                    plugin.schools().setSchool(p, ms);
                } catch (Exception e) { Msg.error(p, "잘못된 학파."); }
            }
            case "list" -> {
                Msg.send(p, "&6=== 5 무공 학파 ===");
                for (MartialSchool ms : MartialSchool.values()) {
                    p.sendMessage("§7• " + ms.colorCode + ms.koreanName
                            + " §7- §e" + ms.bonus);
                }
            }
            default -> Msg.warn(p, "/school join|list");
        }
        return true;
    }
}
