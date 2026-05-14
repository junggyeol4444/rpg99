package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CoreCommand implements CommandExecutor {

    private final RebornCore plugin;

    public CoreCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            Msg.send(sender, "&7/rcore reload | save | tier | stat <get|set|add> <player> <stat> [value]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                plugin.tierManager().loadTables();
                Msg.send(sender, "&a설정 다시 로드 완료.");
                return true;
            case "save":
                plugin.dataManager().flushAll();
                Msg.send(sender, "&a데이터 저장 완료.");
                return true;
            case "tier":
                if (!(sender instanceof Player p)) { Msg.error(sender, "플레이어 전용."); return true; }
                var t = plugin.api().getTier(p.getUniqueId());
                Msg.send(sender, "&e현재 경지: " + (t == null ? "없음" : t.name)
                        + " (총합 " + plugin.api().getTotalStats(p.getUniqueId()) + ")");
                return true;
            case "stat":
                return statSub(sender, args);
        }
        return true;
    }

    private boolean statSub(CommandSender sender, String[] args) {
        if (args.length < 4) { Msg.error(sender, "/rcore stat <get|set|add> <player> <stat> [value]"); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { Msg.error(sender, "플레이어를 찾을 수 없습니다."); return true; }
        StatType type;
        try { type = StatType.valueOf(args[3].toUpperCase()); }
        catch (IllegalArgumentException e) { Msg.error(sender, "알 수 없는 스탯: " + args[3]); return true; }
        PlayerData d = plugin.dataManager().getOrLoad(target.getUniqueId());
        switch (args[1].toLowerCase()) {
            case "get":
                Msg.send(sender, target.getName() + "의 " + type + " = " + d.getStat(type));
                break;
            case "set":
                if (args.length < 5) { Msg.error(sender, "값이 필요합니다."); return true; }
                d.setStat(type, Double.parseDouble(args[4]));
                Msg.send(sender, "설정 완료.");
                break;
            case "add":
                if (args.length < 5) { Msg.error(sender, "값이 필요합니다."); return true; }
                plugin.api().addStat(target.getUniqueId(), type, Double.parseDouble(args[4]), "command");
                Msg.send(sender, "증가 완료.");
                break;
        }
        return true;
    }
}
