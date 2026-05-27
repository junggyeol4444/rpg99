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
                if (a.length < 2) return true;
                StringBuilder desc = new StringBuilder();
                for (int i = 1; i < a.length; i++) desc.append(a[i]).append(' ');
                Msg.send(p, "&7AI가 자기 생성 퀘스트로 등록 검토: " + desc.toString().trim());
                // TODO: AI 매칭 — 현재는 알림만
                break;
        }
        return true;
    }
}
