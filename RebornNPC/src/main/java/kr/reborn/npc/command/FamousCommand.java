package kr.reborn.npc.command;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.famous.FamousNpc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class FamousCommand implements CommandExecutor {
    private final RebornNPC plugin;
    public FamousCommand(RebornNPC plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.send(s, "&7/famous list                - 전체 유명 NPC (" + plugin.famous().all().size() + ")");
            Msg.send(s, "&7/famous world <WORLD>       - 세계별");
            Msg.send(s, "&7/famous info <id>           - NPC 정보");
            Msg.send(s, "&7/famous top [N]             - 권력 순위");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                Msg.send(s, "&6=== 유명 NPC (" + plugin.famous().all().size() + ") ===");
                int n = 0;
                for (FamousNpc fn : plugin.famous().all()) {
                    if (n++ >= 30) { s.sendMessage("§7… 추가 다수 (/famous world <W>)"); break; }
                    s.sendMessage("§7• " + fn.displayName + " §8(" + fn.world + ") §7권력 §c" + fn.powerRank);
                }
            }
            case "world" -> {
                if (a.length < 2) { Msg.warn(s, "/famous world <WORLD>"); return true; }
                try {
                    WorldKey w = WorldKey.valueOf(a[1].toUpperCase());
                    Msg.send(s, "&6=== " + w + " 유명 NPC ===");
                    for (FamousNpc fn : plugin.famous().ofWorld(w)) {
                        s.sendMessage("§7• " + fn.displayName + " §7- §e" + fn.title);
                    }
                } catch (Exception e) { Msg.error(s, "잘못된 세계."); }
            }
            case "info" -> {
                if (a.length < 2) { Msg.warn(s, "/famous info <id>"); return true; }
                FamousNpc fn = plugin.famous().get(a[1]);
                if (fn == null) { Msg.error(s, "NPC 없음."); return true; }
                Msg.send(s, "&6=== " + fn.displayName + " ===");
                s.sendMessage("§7세계: §e" + fn.world + " §7직업: §f" + fn.job);
                s.sendMessage("§7세력: §f" + fn.faction + " §7호칭: §6" + fn.title);
                s.sendMessage("§7권력: §c" + fn.powerRank + "/10");
                if (fn.rewardManualId != null) s.sendMessage("§7보상 비급: §a" + fn.rewardManualId);
                s.sendMessage("§7설명: §f" + fn.description);
            }
            case "top" -> {
                int n = 20;
                if (a.length >= 2) {
                    try { n = Math.max(1, Math.min(100, Integer.parseInt(a[1]))); }
                    catch (NumberFormatException e) { Msg.error(s, "숫자 필요."); return true; }
                }
                Msg.send(s, "&6=== 권력 순위 ===");
                int i = 1;
                for (FamousNpc fn : plugin.famous().topRank(n)) {
                    s.sendMessage("§e" + (i++) + ". " + fn.displayName
                            + " §7권력 §c" + fn.powerRank);
                }
            }
            default -> Msg.warn(s, "/famous list|world|info|top");
        }
        return true;
    }
}
