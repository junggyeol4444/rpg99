package kr.reborn.worldai.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.ai.WorldAI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class WorldAICommand implements CommandExecutor {
    private final RebornWorldAI plugin;
    public WorldAICommand(RebornWorldAI p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.send(s, "&7/worldai status <WORLD> | log [수] | force <WORLD> <event>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "status":
                if (a.length < 2) return true;
                try {
                    WorldKey w = WorldKey.valueOf(a[1].toUpperCase());
                    WorldAI ai = plugin.of(w);
                    if (ai == null) { Msg.error(s, "AI 미활성"); return true; }
                    var st = ai.state();
                    Msg.send(s, "&6[" + w + "] 인플레: " + (int) st.inflation + " 거래: " + String.format("%.2f", st.tradeActivity));
                    Msg.send(s, "&6긴장도: " + (int) st.tension + " 안정: " + (int) st.stability + " 몬스터: " + String.format("%.2f", st.mobBalance));
                } catch (Exception e) { Msg.error(s, "잘못된 세계"); }
                break;
            case "log":
                int n = a.length > 1 ? Integer.parseInt(a[1]) : 10;
                plugin.comm().recent(n).forEach(m ->
                        s.sendMessage("§7[" + m.from + "→" + m.to + "] " + m.type + ": " + m.payload));
                break;
            case "force":
                if (a.length < 3) return true;
                try {
                    WorldKey w = WorldKey.valueOf(a[1].toUpperCase());
                    WorldAI ai = plugin.of(w);
                    if (ai == null) { Msg.error(s, "AI 미활성"); return true; }
                    if ("WAR".equalsIgnoreCase(a[2])) ai.state().tension = 95;
                    if ("FESTIVAL".equalsIgnoreCase(a[2])) { ai.state().stability = 95; ai.state().tension = 5; }
                    Msg.send(s, "&a강제 트리거 설정.");
                } catch (Exception e) { Msg.error(s, "잘못된 인자"); }
                break;
        }
        return true;
    }
}
