package kr.reborn.time.command;

import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ChamberCommand implements CommandExecutor {
    private final RebornTime plugin;
    public ChamberCommand(RebornTime p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/chamber enter <id> | exit | progress");
            Msg.send(p, "&7  드래곤: dragon_chamber_aurelius/ignifer/nocterna/cerylis/silvarex");
            Msg.send(p, "&7  선계: caveheaven_1 ~ caveheaven_36 (36동천), bless_1 ~ bless_72 (72복지)");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "enter":
                if (a.length < 2) { Msg.warn(p, "/chamber enter <id>"); return true; }
                plugin.chamber().enter(p, a[1]);
                break;
            case "exit":
                plugin.chamber().exit(p);
                break;
            case "progress": {
                int caves = plugin.chamber().discoveredCaveheavens(p.getUniqueId());
                int blesses = plugin.chamber().discoveredBlessedLands(p.getUniqueId());
                Msg.send(p, "&6&l[선계 진척]");
                Msg.send(p, "&536동천 발견: §f" + caves + " / 36");
                Msg.send(p, "&572복지 발견: §f" + blesses + " / 72");
                if (caves >= 36) Msg.send(p, "&6&l✦ 36동천 모두 발견! 동천 마스터");
                if (blesses >= 72) Msg.send(p, "&6&l✦ 72복지 모두 발견! 복지 순례자");
                break;
            }
            default -> Msg.warn(p, "/chamber enter|exit|progress");
        }
        return true;
    }
}
