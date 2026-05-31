package kr.reborn.mob.command;

import kr.reborn.core.util.Msg;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.dungeon.Dungeon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DungeonCommand implements CommandExecutor {
    private final RebornMob plugin;
    public DungeonCommand(RebornMob plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/dungeon list             - 전체 던전 목록");
            Msg.send(p, "&7/dungeon enter <id>       - 입장");
            Msg.send(p, "&7/dungeon exit             - 퇴장");
            Msg.send(p, "&7/dungeon info <id>        - 던전 정보");
            Msg.send(p, "&7/dungeon progress         - 내 진척도");
            Msg.send(p, "&7/dungeon current          - 현재 진행 중인 던전");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                Msg.send(p, "&6=== 던전 (" + plugin.dungeons().all().size() + ") ===");
                for (Dungeon d : plugin.dungeons().all()) {
                    int maxFloor = plugin.dungeons().maxFloorOf(p.getUniqueId(), d.id);
                    String prog = maxFloor > 0 ? " §a[" + maxFloor + "/" + d.totalFloors + "층]" : "";
                    p.sendMessage("§7• §e" + d.id + " §f" + d.name
                            + " §8(" + d.world + ", " + d.totalFloors + "층) §7min " + d.minTotalStats + prog);
                }
            }
            case "enter" -> {
                if (a.length < 2) { Msg.warn(p, "/dungeon enter <id>"); return true; }
                plugin.dungeons().enter(p, a[1]);
            }
            case "exit" -> plugin.dungeons().exit(p);
            case "info" -> {
                if (a.length < 2) { Msg.warn(p, "/dungeon info <id>"); return true; }
                Dungeon d = plugin.dungeons().get(a[1]);
                if (d == null) { Msg.error(p, "던전 없음"); return true; }
                Msg.send(p, "&6=== " + d.name + " ===");
                p.sendMessage("§7세계: §e" + d.world + " §7층수: §f" + d.totalFloors);
                p.sendMessage("§7최소 스탯: §c" + d.minTotalStats);
                p.sendMessage("§7완주 보상: §a" + d.completionRewards);
                int maxFloor = plugin.dungeons().maxFloorOf(p.getUniqueId(), d.id);
                p.sendMessage("§7내 진척: §6" + maxFloor + "/" + d.totalFloors);
            }
            case "progress" -> {
                Msg.send(p, "&6=== 내 던전 진척도 ===");
                for (Dungeon d : plugin.dungeons().all()) {
                    int maxFloor = plugin.dungeons().maxFloorOf(p.getUniqueId(), d.id);
                    if (maxFloor == 0) continue;
                    p.sendMessage("§7• §e" + d.name + " §6" + maxFloor + "/" + d.totalFloors
                            + " " + (maxFloor >= d.totalFloors ? "§a✓ 클리어" : ""));
                }
            }
            case "current" -> {
                var sess = plugin.dungeons().activeOf(p.getUniqueId());
                if (sess == null) { Msg.send(p, "&7진행 중 던전 없음."); return true; }
                Dungeon d = plugin.dungeons().get(sess.dungeonId);
                Msg.send(p, "&6진행 중: §f" + (d != null ? d.name : sess.dungeonId)
                        + " §7현재 §6" + sess.currentFloor + "/" + (d != null ? d.totalFloors : "?") + "층");
            }
            default -> Msg.warn(p, "/dungeon list|enter|exit|info|progress|current");
        }
        return true;
    }
}
