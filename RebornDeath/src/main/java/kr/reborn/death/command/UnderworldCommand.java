package kr.reborn.death.command;

import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UnderworldCommand implements CommandExecutor {
    private final RebornDeath plugin;
    public UnderworldCommand(RebornDeath p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/underworld revive            - 환혼 (살아 돌아간다)");
            Msg.send(p, "&7/underworld reincarnate       - 윤회 (새로 환생)");
            Msg.send(p, "&7/underworld stay              - 명계 잔류 (토착민)");
            Msg.send(p, "&7/underworld quest             - 명계 의뢰 목록");
            Msg.send(p, "&7/underworld quest <id>        - 의뢰 진척 확인");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "revive" -> plugin.underworld().revive(p);
            case "reincarnate" -> plugin.underworld().reincarnate(p);
            case "stay" -> plugin.underworld().stay(p);
            case "quest" -> {
                if (a.length >= 2) {
                    String pid = plugin.underworldQuests().progressOf(p.getUniqueId(), a[1]);
                    Msg.send(p, "&6의뢰 진척: " + pid);
                } else {
                    Msg.send(p, "&6=== 명계 의뢰 목록 ===");
                    for (String qid : plugin.underworldQuests().allQuestIds()) {
                        p.sendMessage("§7• §e" + qid + " §7- "
                                + plugin.underworldQuests().progressOf(p.getUniqueId(), qid));
                    }
                    if (plugin.underworldQuests().alreadyReincarnationEligible(p.getUniqueId())) {
                        Msg.send(p, "&6&l✦ 환생 자격 회복! &r/underworld reincarnate");
                    }
                }
            }
            default -> Msg.warn(p, "/underworld revive|reincarnate|stay|quest");
        }
        return true;
    }
}
