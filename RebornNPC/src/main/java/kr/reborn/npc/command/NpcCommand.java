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
            Msg.send(s, "&7/rnpc spawn <id> <name> [job] [faction] | remove <id> | list | setstat <id> <stat> <v>");
            Msg.send(s, "&7       | hermit <id> | sethome <id> | setwork <id> | inspect <id>");
            Msg.send(s, "&7       | goals <id> | setgoal <id> <kind> [target] | abandon <id> <kind>");
            Msg.send(s, "&7       | social <id> | rumors <id> | reputation <id> [target]");
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
                    String brain = nn.brain != null && nn.brain.current() != null
                            ? nn.brain.current().id() : "-";
                    s.sendMessage("§e" + nn.id + " §f" + nn.displayName
                            + " §7(" + nn.world + " " + nn.state + " " + brain + ")");
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
            case "sethome":
                if (!(s instanceof Player ph) || a.length < 2) return true;
                RebornNpc hn = plugin.registry().get(a[1]);
                if (hn == null) { Msg.error(s, "NPC 없음"); return true; }
                hn.home = ph.getLocation();
                Msg.send(s, hn.id + " 집 좌표 설정");
                break;
            case "setwork":
                if (!(s instanceof Player pw) || a.length < 2) return true;
                RebornNpc wn = plugin.registry().get(a[1]);
                if (wn == null) { Msg.error(s, "NPC 없음"); return true; }
                wn.workplace = pw.getLocation();
                Msg.send(s, wn.id + " 직장 좌표 설정");
                break;
            case "inspect":
                if (a.length < 2) return true;
                RebornNpc ins = plugin.registry().get(a[1]);
                if (ins == null) { Msg.error(s, "NPC 없음"); return true; }
                Msg.send(s, "&6=== " + ins.displayName + " (" + ins.id + ") ===");
                s.sendMessage("§7state: §f" + ins.state + " §7brain: §f"
                        + (ins.brain != null && ins.brain.current() != null ? ins.brain.current().id() : "-"));
                s.sendMessage("§7faction: §f" + ins.faction + " §7job: §f" + ins.job);
                s.sendMessage("§7spouse: §f" + (ins.spouseNpcId.isEmpty() ? "-" : ins.spouseNpcId)
                        + " §7children: §f" + ins.children.size());
                s.sendMessage("§7emotion: §c분노 " + (int)ins.emotion.get(kr.reborn.npc.emotion.Emotion.Kind.ANGER)
                        + " §e공포 " + (int)ins.emotion.get(kr.reborn.npc.emotion.Emotion.Kind.FEAR)
                        + " §a기쁨 " + (int)ins.emotion.get(kr.reborn.npc.emotion.Emotion.Kind.HAPPINESS)
                        + " §b신뢰 " + (int)ins.emotion.get(kr.reborn.npc.emotion.Emotion.Kind.TRUST));
                s.sendMessage("§7stats total: §f" + (int)ins.totalStats() + " (hermit eff: " + (int)ins.effectiveTotal() + ")");
                if (ins.soul != null) {
                    s.sendMessage("§6--- 영혼 ---");
                    s.sendMessage("§7성격: " + ins.soul.personality.summary());
                    s.sendMessage(String.format("§7나이: §f%.1f년 §7행복: §f%.0f",
                            ins.soul.ageYears, ins.soul.happiness()));
                    var nn = ins.soul.needs;
                    s.sendMessage(String.format("§7욕구: §c음식%.0f §6휴식%.0f §e안전%.0f §a사교%.0f §d사랑%.0f §b지위%.0f",
                            nn.get(kr.reborn.npc.soul.Needs.Kind.FOOD),
                            nn.get(kr.reborn.npc.soul.Needs.Kind.REST),
                            nn.get(kr.reborn.npc.soul.Needs.Kind.SAFETY),
                            nn.get(kr.reborn.npc.soul.Needs.Kind.COMPANIONSHIP),
                            nn.get(kr.reborn.npc.soul.Needs.Kind.LOVE),
                            nn.get(kr.reborn.npc.soul.Needs.Kind.STATUS)));
                    s.sendMessage("§7관계: §a가족 " + ins.soul.family.size()
                            + " §b친구 " + ins.soul.friends.size()
                            + " §6라이벌 " + ins.soul.rivals.size()
                            + " §4원수 " + ins.soul.nemeses.size());
                    s.sendMessage("§7기억 수: §f" + ins.soul.memory.all().size() + "개");
                }
                // 활성 목표
                if (!ins.goals.isEmpty()) {
                    s.sendMessage("§6--- 활성 목표 ---");
                    for (var g : ins.goals) {
                        s.sendMessage("§e[" + g.priority + "] §f" + g.description
                                + " §7(" + String.format("%.0f%%", g.progress) + ")");
                    }
                }
                // Utility 의사결정 스냅샷
                if (ins.brain != null && !ins.brain.lastScores.isEmpty()) {
                    s.sendMessage("§6--- Utility 점수 (마지막 결정) ---");
                    ins.brain.lastScores.entrySet().stream()
                            .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
                            .forEach(e -> {
                                String color = e.getValue() > 0.7 ? "§c"
                                        : e.getValue() > 0.4 ? "§e"
                                        : e.getValue() > 0.2 ? "§7" : "§8";
                                s.sendMessage("  " + color + e.getKey() + ": " + String.format("%.2f", e.getValue()));
                            });
                    s.sendMessage(String.format("§7현재 선택: §f%s §7(util %.2f)",
                            ins.brain.current() != null ? ins.brain.current().id() : "-",
                            ins.brain.currentUtility()));
                }
                if (!ins.goalsArchive.isEmpty()) {
                    s.sendMessage("§8과거 목표 " + ins.goalsArchive.size() + "개 (archive)");
                }
                break;
            case "goals":
                if (a.length < 2) return true;
                RebornNpc gn = plugin.registry().get(a[1]);
                if (gn == null) { Msg.error(s, "NPC 없음"); return true; }
                Msg.send(s, "&6=== " + gn.displayName + " 활성 목표 ===");
                if (gn.goals.isEmpty()) s.sendMessage("§7(없음 — 곧 새 목표 생성될 수 있음)");
                for (var g : gn.goals) {
                    s.sendMessage("§e[" + g.priority + "] §f" + g.kind + " §7→ " + g.target);
                    s.sendMessage("    §f" + g.description + " §7(" + String.format("%.1f%%", g.progress) + ")");
                }
                Msg.send(s, "&7과거 목표 " + gn.goalsArchive.size() + "개:");
                for (int i = Math.max(0, gn.goalsArchive.size() - 5); i < gn.goalsArchive.size(); i++) {
                    var g = gn.goalsArchive.get(i);
                    String mark = g.isFulfilled() ? "§a✓" : "§4✗";
                    s.sendMessage("  " + mark + " §7" + g.description);
                }
                break;
            case "setgoal":
                if (a.length < 3) { Msg.error(s, "/rnpc setgoal <id> <KIND> [target]"); return true; }
                RebornNpc sg = plugin.registry().get(a[1]);
                if (sg == null) { Msg.error(s, "NPC 없음"); return true; }
                try {
                    var kind = kr.reborn.npc.soul.GoalKind.valueOf(a[2].toUpperCase());
                    String target = a.length > 3 ? a[3] : "";
                    var goal = new kr.reborn.npc.soul.Goal(kind, target,
                            "[관리자 부여] " + kind.description);
                    goal.priority = 80;
                    sg.goals.add(goal);
                    Msg.send(s, "&a목표 부여: " + kind.description);
                } catch (Exception e) {
                    Msg.error(s, "유효한 KIND: GAIN_POWER, SERVE_LORD, FOUND_TOWN, FOUND_RELIGION, DEFEAT_RIVAL, GAIN_WEALTH, START_BUSINESS, FIND_LOVE, PROTECT_FAMILY, AVENGE, MASTER_ART, EXPLORE, ACCUMULATE_KNOWLEDGE, ASCEND, DESTROY_RIVAL_FACTION, BETRAY, HIDE");
                }
                break;
            case "abandon":
                if (a.length < 3) return true;
                RebornNpc ab = plugin.registry().get(a[1]);
                if (ab == null) { Msg.error(s, "NPC 없음"); return true; }
                try {
                    var kind = kr.reborn.npc.soul.GoalKind.valueOf(a[2].toUpperCase());
                    int removed = 0;
                    for (var g : new java.util.ArrayList<>(ab.goals)) {
                        if (g.kind == kind) { g.abandoned = true; removed++; }
                    }
                    Msg.send(s, "&7" + removed + "개 목표 포기.");
                } catch (Exception ignored) {}
                break;
            case "social": {
                if (a.length < 2) return true;
                RebornNpc so = plugin.registry().get(a[1]);
                if (so == null) { Msg.error(s, "NPC 없음"); return true; }
                Msg.send(s, "&6=== " + so.displayName + " 관계망 ===");
                var rels = plugin.registry().socialNetwork().neighbors(so.id);
                if (rels.isEmpty()) s.sendMessage("§7(관계 없음)");
                for (var e : rels.entrySet()) {
                    var partner = plugin.registry().get(e.getKey());
                    String pname = partner != null ? partner.displayName : e.getKey();
                    s.sendMessage("§e" + e.getValue().label + " §7→ §f" + pname);
                }
                s.sendMessage("§7연결 수(영향력): §f" + plugin.registry().socialNetwork().connectionCount(so.id));
                break;
            }
            case "rumors": {
                if (a.length < 2) return true;
                RebornNpc ru = plugin.registry().get(a[1]);
                if (ru == null || ru.soul == null) { Msg.error(s, "NPC 없음"); return true; }
                Msg.send(s, "&6=== " + ru.displayName + "이(가) 들은 소문 (" + ru.soul.rumorsHeard.size() + ") ===");
                int shown = 0;
                for (int i = ru.soul.rumorsHeard.size() - 1; i >= 0 && shown < 10; i--, shown++) {
                    var r = ru.soul.rumorsHeard.get(i);
                    var subj = plugin.registry().get(r.subject);
                    String sname = subj != null ? subj.displayName : shorten(r.subject);
                    String color = r.content.isPositive() ? "§a" : "§c";
                    s.sendMessage(color + "• " + r.story(sname)
                            + " §8(신뢰 " + String.format("%.0f%%", r.believability * 100)
                            + ", " + r.hopCount + "다리)");
                }
                break;
            }
            case "reputation": {
                if (a.length < 2) return true;
                RebornNpc rp = plugin.registry().get(a[1]);
                if (rp == null || rp.soul == null) { Msg.error(s, "NPC 없음"); return true; }
                if (a.length >= 3) {
                    String target = a[2];
                    var tn = plugin.registry().get(target);
                    if (tn != null) target = tn.id;
                    double score = rp.soul.reputation.scoreOf(target);
                    Msg.send(s, "&6" + rp.displayName + "의 " + a[2] + " 평판: §f" + String.format("%.1f", score));
                } else {
                    Msg.send(s, "&6=== " + rp.displayName + " 평판 (상위/하위) ===");
                    rp.soul.reputation.all().entrySet().stream()
                            .sorted((x, y) -> Double.compare(Math.abs(y.getValue()), Math.abs(x.getValue())))
                            .limit(10)
                            .forEach(e -> {
                                var t = plugin.registry().get(e.getKey());
                                String tname = t != null ? t.displayName : shorten(e.getKey());
                                String c = e.getValue() > 0 ? "§a+" : "§c";
                                s.sendMessage("  " + c + String.format("%.0f", e.getValue()) + " §7" + tname);
                            });
                }
                break;
            }
        }
        return true;
    }

    private String shorten(String id) {
        return id.length() <= 12 ? id : id.substring(0, 8) + "...";
    }

    private WorldKey detect(Player p) {
        try { return WorldKey.valueOf(p.getWorld().getName().toUpperCase()); }
        catch (Exception e) { return WorldKey.LOBBY; }
    }
}
