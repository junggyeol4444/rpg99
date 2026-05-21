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
                break;
        }
        return true;
    }

    private WorldKey detect(Player p) {
        try { return WorldKey.valueOf(p.getWorld().getName().toUpperCase()); }
        catch (Exception e) { return WorldKey.LOBBY; }
    }
}
