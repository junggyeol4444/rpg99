package kr.reborn.god.command;

import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import kr.reborn.god.miracle.Miracle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GodCommand implements CommandExecutor {
    private final RebornGod plugin;
    public GodCommand(RebornGod p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/god trial start | status | riddle | answer <단어> | conviction");
            Msg.send(p, "&7     ascend | info | tier | tier list");
            Msg.send(p, "&7     domain create | enter | invite <p> | kick <p> | rule <k> <v>");
            Msg.send(p, "&7     religion create <id> <name> | list | pray <id>");
            Msg.send(p, "&7     miracle list | cast <type> [target]");
            Msg.send(p, "&7     war declare <godId> | list");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "trial": {
                if (a.length < 2) { Msg.send(p, "&7/god trial start|status|conviction"); break; }
                String sub = a[1].toLowerCase();
                if ("start".equals(sub))           plugin.trials().startTrial(p);
                else if ("status".equals(sub))     plugin.trials().status(p);
                else if ("conviction".equals(sub)) plugin.trials().startConviction(p);
                break;
            }
            case "riddle":  plugin.trials().askRiddle(p); break;
            case "answer":
                if (a.length < 2) return true;
                plugin.trials().answerRiddle(p, a[1]);
                break;

            case "ascend": plugin.gods().ascend(p); break;
            case "info": {
                God g = plugin.gods().of(p.getUniqueId());
                if (g == null) { Msg.warn(p, "필멸자"); return true; }
                String tier = plugin.gods().tierOf(g);
                Msg.send(p, "&6=== " + g.name + " ===");
                Msg.send(p, "&7신성: §f" + (int) g.divinity + " §7등급: §6" + tier);
                Msg.send(p, "&7신도: §f" + g.followers.size() + " §7동맹: §f" + g.allies.size());
                Msg.send(p, "&7신역: §f" + (g.domainWorld.isEmpty() ? "없음" : g.domainWorld));
                Msg.send(p, "&7전쟁: §f" + (g.atWar() ? "vs " + g.warOpponent : "평화"));
                break;
            }
            case "tier": {
                if (a.length >= 2 && "list".equalsIgnoreCase(a[1])) {
                    Msg.send(p, "&6신성 등급 표:");
                    for (var e : plugin.getConfig().getMapList("tiers")) {
                        p.sendMessage("§7• §f" + e.get("name") + " §8(min " + e.get("min") + ")");
                    }
                } else {
                    God g = plugin.gods().of(p.getUniqueId());
                    Msg.send(p, g == null ? "&7필멸자" : "&6현재 등급: " + plugin.gods().tierOf(g));
                }
                break;
            }

            case "domain": {
                if (a.length < 2) { plugin.domains().enter(p); break; }
                String sub = a[1].toLowerCase();
                if ("create".equals(sub)) plugin.domains().create(p);
                else if ("enter".equals(sub)) plugin.domains().enter(p);
                else if ("invite".equals(sub) && a.length >= 3) {
                    Player g = p.getServer().getPlayerExact(a[2]);
                    if (g != null) plugin.domains().invite(p, g);
                } else if ("kick".equals(sub) && a.length >= 3) {
                    Player g = p.getServer().getPlayerExact(a[2]);
                    if (g != null) plugin.domains().kick(p, g);
                } else if ("rule".equals(sub) && a.length >= 4) {
                    plugin.domains().setRule(p, a[2], a[3]);
                }
                break;
            }

            case "religion": {
                if (a.length < 2) { Msg.send(p, "&7/god religion create <id> <name> | list"); break; }
                String sub = a[1].toLowerCase();
                if ("create".equals(sub) && a.length >= 4) {
                    plugin.religions().create(p, a[2], a[3]);
                } else if ("list".equals(sub)) {
                    Msg.send(p, "&6=== 교단 목록 (" + plugin.religions().all().size() + ") ===");
                    int shown = 0;
                    for (var r : plugin.religions().all()) {
                        if (shown++ >= 20) { p.sendMessage("§7…"); break; }
                        p.sendMessage("§e" + r.id + " §7- " + r.name + " §8신앙:" + (int) r.faith
                                + " 신도:" + r.totalFollowers());
                    }
                }
                break;
            }
            case "pray":
                if (a.length < 2) return true;
                if (plugin.religions().pray(p, a[1])) Msg.send(p, "&a기도 — 신앙 +5");
                else Msg.error(p, "교단 없음: " + a[1]);
                break;

            case "miracle": {
                if (a.length < 2) { Msg.send(p, "&7/god miracle list | cast <type> [target]"); break; }
                String sub = a[1].toLowerCase();
                if ("list".equals(sub)) {
                    God g = plugin.gods().of(p.getUniqueId());
                    String myTier = g == null ? "" : plugin.gods().tierOf(g);
                    Msg.send(p, "&6=== 기적 (현 등급: " + myTier + ") ===");
                    for (Miracle m : Miracle.values()) {
                        String mark = g != null && Miracle.tierOf(myTier) >= m.tierIndex() ? "§a✔" : "§7";
                        p.sendMessage(mark + " §f" + m.name() + " §7(" + m.requiredTier
                                + ", 신성 " + (int) m.cost + ") " + m.description);
                    }
                } else if ("cast".equals(sub) && a.length >= 3) {
                    try {
                        Miracle m = Miracle.valueOf(a[2].toUpperCase());
                        String target = a.length >= 4 ? a[3] : "";
                        plugin.miracles().cast(p, m, target);
                    } catch (IllegalArgumentException e) {
                        Msg.error(p, "그런 기적 없음: " + a[2]);
                    }
                }
                break;
            }

            case "war": {
                if (a.length < 2) { Msg.send(p, "&7/god war declare <godId> | list"); break; }
                String sub = a[1].toLowerCase();
                if ("declare".equals(sub) && a.length >= 3) {
                    plugin.wars().declareWar(p, a[2]);
                } else if ("list".equals(sub)) {
                    var active = plugin.wars().activeWars();
                    Msg.send(p, "&6=== 활성 신 전쟁 (" + active.size() + ") ===");
                    for (var w : active) {
                        p.sendMessage("§c⚔ §f" + w.challengerGodId + " §c vs §f" + w.defenderGodId
                                + " §7점수 " + (int) w.challengerScore + " : " + (int) w.defenderScore);
                    }
                }
                break;
            }
        }
        return true;
    }
}
