package kr.reborn.npc.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class NpcCommand implements CommandExecutor {

    private final RebornNPC plugin;

    public NpcCommand(RebornNPC p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.send(s, "&7/rnpc spawn <id> <name> [job] [faction]  |  remove <id>  |  list  |  setstat <id> <stat> <v>  |  hermit <id>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "spawn":
                if (!(s instanceof Player p) || a.length < 3) return true;
                var n = plugin.registry().spawn(a[1], a[2], detect(p),
                        p.getLocation(),
                        a.length > 4 ? a[4] : "",
                        a.length > 3 ? a[3] : "VILLAGER");
                Msg.send(s, "&aNPC " + n.id + " 스폰됨.");
                break;
            case "remove":
                if (a.length < 2) return true;
                plugin.registry().remove(a[1]);
                Msg.send(s, "&aNPC 제거됨.");
                break;
            case "list":
                Msg.send(s, "&6총 " + plugin.registry().all().size() + "개 NPC");
                for (RebornNpc nn : plugin.registry().all()) {
                    s.sendMessage("§e" + nn.id + " §f" + nn.displayName + " §7(" + nn.world + " " + nn.state + ")");
                }
                break;
            case "setstat":
                if (a.length < 4) return true;
                RebornNpc t = plugin.registry().get(a[1]);
                if (t == null) { Msg.error(s, "NPC 없음"); return true; }
                t.stats.put(a[2], Double.parseDouble(a[3]));
                break;
            case "hermit":
                if (a.length < 2) return true;
                RebornNpc h = plugin.registry().get(a[1]);
                if (h == null) { Msg.error(s, "NPC 없음"); return true; }
                h.hermit = true;
                Msg.send(s, h.id + " 은둔고수로 표시.");
                break;
        }
        return true;
    }

    private WorldKey detect(Player p) {
        try { return WorldKey.valueOf(p.getWorld().getName().toUpperCase()); }
        catch (Exception e) { return WorldKey.LOBBY; }
    }
}
