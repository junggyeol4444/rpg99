package kr.reborn.spawn.command;

import kr.reborn.spawn.RebornSpawn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RerollCommand implements CommandExecutor {
    private final RebornSpawn plugin;
    public RerollCommand(RebornSpawn p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        // 이중 가드 — /reroll은 admin/test 전용. 정상 환생은 여신 NPC 통해.
        if (!p.hasPermission("rebornspawn.admin") && !p.isOp()) {
            kr.reborn.core.util.Msg.error(p, "환생은 여신 NPC와의 만남으로 시작됩니다. /reroll은 관리/테스트 전용.");
            return true;
        }
        plugin.roulette().spin(p);
        return true;
    }
}
