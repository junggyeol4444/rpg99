package kr.reborn.stat.serendipity;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /fortune list | obtained | grant <id> (관리·확인용). */
public final class FortuneCommand implements CommandExecutor {

    private final RebornStat plugin;

    public FortuneCommand(RebornStat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var reg = plugin.fortunes();
        var mgr = plugin.fortuneManager();
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (a.length == 0) {
            Msg.send(p, "&7/fortune list | obtained | grant <id>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list":
                Msg.send(p, "&6=== 기연 목록 (" + reg.all().size() + ") ===");
                for (Fortune f : reg.all()) {
                    boolean got = d != null && mgr.has(d, f.id);
                    String w = f.world == null ? "공통" : f.world.name();
                    p.sendMessage((got ? "§a✔ " : "§7• ") + "§f" + f.id + " §7[" + w + "/" + f.trigger + "] " + f.name);
                }
                break;
            case "obtained":
                if (d == null) return true;
                Msg.send(p, "&6=== 획득한 기연 ===");
                int n = 0;
                for (Fortune f : reg.all()) {
                    if (mgr.has(d, f.id)) { p.sendMessage("§a✔ §f" + f.name); n++; }
                }
                if (n == 0) p.sendMessage("§7(아직 없음)");
                break;
            case "grant":  // 관리/테스트 — 즉시 지급
                if (a.length < 2 || d == null) return true;
                Fortune f = reg.get(a[1]);
                if (f == null) { Msg.error(p, "기연 없음: " + a[1]); return true; }
                if (!mgr.grant(p, d, f)) Msg.warn(p, "이미 얻은 기연.");
                break;
            default:
                Msg.send(p, "&7/fortune list | obtained | grant <id>");
        }
        return true;
    }
}
