package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class HiddenWorldCommand implements CommandExecutor {
    private final RebornCore plugin;
    public HiddenWorldCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        WorldKey[] hidden = {WorldKey.ABYSS, WorldKey.UNDERWORLD, WorldKey.TIME_REALM,
                             WorldKey.DREAM, WorldKey.VOID, WorldKey.GOD};
        var set = plugin.hiddenWorld().unlockedOf(p.getUniqueId());
        Msg.send(p, "&5&l╔══════════════════════════════╗");
        Msg.send(p, "&5&l║   §f6 히든 월드 발견 상태   §5&l║");
        Msg.send(p, "&5&l╚══════════════════════════════╝");
        for (WorldKey w : hidden) {
            String mark = set.contains(w) ? "§a✓" : "§7◌";
            String label = labelOf(w);
            String cond = condOf(w);
            p.sendMessage(mark + " §f" + label + " §7- " + cond);
        }
        Msg.send(p, "&7발견: §f" + set.size() + "/6");
        return true;
    }

    private String labelOf(WorldKey w) {
        return switch (w) {
            case ABYSS -> "심연계";
            case UNDERWORLD -> "명계";
            case TIME_REALM -> "시간계";
            case DREAM -> "꿈계";
            case VOID -> "공허계";
            case GOD -> "신계";
            default -> w.name();
        };
    }

    private String condOf(WorldKey w) {
        return switch (w) {
            case ABYSS -> "마기 5000 + 사망 100회";
            case UNDERWORLD -> "사망 1회";
            case TIME_REALM -> "환생 50회";
            case DREAM -> "정신 500 + 지능 500";
            case VOID -> "13세계 방문 + 신성 1000";
            case GOD -> "신성 1000";
            default -> "?";
        };
    }
}
