package kr.reborn.quest.command;

import kr.reborn.core.util.Msg;
import kr.reborn.quest.RebornQuest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class QuestCommand implements CommandExecutor {
    private final RebornQuest plugin;
    public QuestCommand(RebornQuest p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/quest list | accept <id> | abandon <id> | active");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list":
                Msg.send(p, "&6등록된 퀘스트:");
                plugin.registry().all().forEach(q ->
                        p.sendMessage("§e" + q.id + " §7- " + q.name + " (" + q.type + ")"));
                break;
            case "accept":
                if (a.length < 2) return true;
                if (!plugin.engine().accept(p, a[1])) Msg.error(p, "퀘스트 없음 또는 이미 진행 중.");
                break;
            case "abandon":
                if (a.length < 2) return true;
                if (!plugin.engine().abandon(p, a[1])) Msg.error(p, "진행 중이 아닌 퀘스트.");
                break;
            case "active":
                var ids = plugin.engine().activeFor(p.getUniqueId()).keySet();
                if (ids.isEmpty()) { Msg.send(p, "&7진행 중인 퀘스트가 없다."); break; }
                Msg.send(p, "&6진행 중 (" + ids.size() + "):");
                for (String id : ids) {
                    String line = plugin.engine().describe(p.getUniqueId(), id);
                    if (line != null) p.sendMessage("§e" + id + " §7- " + line);
                }
                break;
            case "create":
                Msg.warn(p, "&7자기 생성 퀘스트는 아직 구현되지 않았다. /quest list에서 기존 퀘스트를 선택하라.");
                break;
            case "contrib": {
                if (a.length < 2) { Msg.warn(p, "/quest contrib <questId>"); return true; }
                var contribs = plugin.contrib().of(a[1]);
                if (contribs.isEmpty()) { Msg.send(p, "&7기여 기록 없음: " + a[1]); break; }
                Msg.send(p, "&6=== " + a[1] + " 기여도 (상위 10) ===");
                contribs.entrySet().stream()
                        .sorted(java.util.Map.Entry.<java.util.UUID, Double>comparingByValue().reversed())
                        .limit(10)
                        .forEach(e -> {
                            var off = org.bukkit.Bukkit.getOfflinePlayer(e.getKey());
                            String name = off.getName() != null ? off.getName()
                                    : e.getKey().toString().substring(0, 8);
                            p.sendMessage("§e" + name + " §7- §a" + String.format("%.0f", e.getValue()));
                        });
                break;
            }
        }
        return true;
    }
}
