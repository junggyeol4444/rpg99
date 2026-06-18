package kr.reborn.quest.command;

import kr.reborn.core.util.Msg;
import kr.reborn.quest.RebornQuest;
import kr.reborn.quest.engine.Quest;
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
            Msg.send(p, "&7/quest create <KILL|GATHER|EXPLORE|SURVIVE> <target> <n> [이름]");
            Msg.send(p, "&7/quest contrib <id>");
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
            case "create": {
                // 자기 생성 퀘스트 (기획서 7장 ④): 플레이어가 목표를 선언하면 시스템이 등록.
                // 사용법: /quest create <KILL|GATHER|EXPLORE|SURVIVE> <target> <amount> [name...]
                if (a.length < 4) {
                    Msg.send(p, "&7/quest create <KILL|GATHER|EXPLORE|SURVIVE> <target> <amount> [이름...]");
                    Msg.send(p, "&7예: /quest create KILL ZOMBIE 50 좀비 학살자");
                    return true;
                }
                String type = a[1].toUpperCase();
                if (!type.equals("KILL") && !type.equals("GATHER")
                        && !type.equals("EXPLORE") && !type.equals("SURVIVE")) {
                    Msg.error(p, "지원하지 않는 타입. KILL/GATHER/EXPLORE/SURVIVE 중 하나.");
                    return true;
                }
                String target = a[2];
                int amount;
                try { amount = Integer.parseInt(a[3]); }
                catch (NumberFormatException ex) { Msg.error(p, "amount는 숫자."); return true; }
                if (amount < 1 || amount > 100000) {
                    Msg.error(p, "amount는 1~100000.");
                    return true;
                }
                // AI 판단 — 너무 작은 목표는 거부 (기획서: "AI가 불가능하거나 모호한 목표는 등록 거부")
                if (type.equals("KILL") && amount < 5) {
                    Msg.warn(p, "&7너무 사소한 목표는 등록되지 않는다 (최소 5).");
                    return true;
                }
                StringBuilder nameB = new StringBuilder();
                for (int i = 4; i < a.length; i++) {
                    if (i > 4) nameB.append(' ');
                    nameB.append(a[i]);
                }
                String name = nameB.length() == 0
                        ? ("자기 목표: " + type + " " + target + " " + amount)
                        : nameB.toString();
                // 고유 ID 생성 — 플레이어 UUID 앞 8자 + 카운터
                String idPrefix = "self_" + p.getUniqueId().toString().substring(0, 8) + "_";
                int n = 1;
                String id;
                do { id = idPrefix + (n++); } while (plugin.registry().has(id));
                // 보상 — 자기 목표는 칭호만 (스탯/아이템 자동 등록 금지)
                java.util.Map<String, Object> rewards = java.util.Map.of(
                        "title", "자기 목표 달성자",
                        "stats", java.util.Map.of("MENTAL", 5)
                );
                Quest q = new Quest(id, name, type, target, amount, "", null, rewards);
                plugin.registry().register(q);
                Msg.send(p, "&6&l[자기 목표 등록] §f" + name + " §7(" + id + ")");
                Msg.send(p, "&7/quest accept " + id + " §8으로 시작하라.");
                break;
            }
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
