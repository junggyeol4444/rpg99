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
        }
        return true;
    }
}
