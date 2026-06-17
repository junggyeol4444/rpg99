package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class KingdomCommand implements CommandExecutor {
    private final RebornClan plugin;
    public KingdomCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/kingdom create <id> <name> | join <id> | leave | list | info [id] | members | relations | ally <id> | war <id> | treaty <id> | marry <id> <npc>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "create":
                if (a.length < 3) return true;
                plugin.kingdoms().create(p, a[1], a[2]);
                break;
            case "join":
                if (a.length < 2) { Msg.warn(p, "/kingdom join <id>"); return true; }
                plugin.kingdoms().join(p, a[1]);
                break;
            case "leave":
                plugin.kingdoms().leave(p);
                break;
            case "list": {
                var all = plugin.kingdoms().all();
                Msg.send(p, "&6=== 왕국 목록 (" + all.size() + ") ===");
                for (var kk : all) {
                    p.sendMessage("§e" + kk.id + " §7- " + kk.name
                            + " §8(가문 " + kk.clans.size() + ")");
                }
                break;
            }
            case "info": {
                String targetId = a.length >= 2 ? a[1] : null;
                var kk = targetId != null ? plugin.kingdoms().get(targetId)
                        : plugin.kingdoms().ofPlayer(p.getUniqueId());
                if (kk == null) { Msg.warn(p, "왕국 없음."); break; }
                Msg.send(p, "&6=== " + kk.name + " ===");
                p.sendMessage("§7ID: §f" + kk.id);
                p.sendMessage("§7왕: §f" + kk.king);
                p.sendMessage("§7산하 가문: §f" + kk.clans.size());
                p.sendMessage("§7총 영토 chunk: §f" + plugin.kingdoms().totalTerritory(kk));
                p.sendMessage("§7총 인구(가문 멤버 합): §f" + plugin.kingdoms().totalPopulation(kk));
                p.sendMessage("§6주기 세금 수입: §e" + plugin.kingdoms().taxRevenue(kk) + " GOLD");
                int allies = 0, wars = 0;
                for (var other : plugin.kingdoms().all()) {
                    if (other.id.equals(kk.id)) continue;
                    var rel = plugin.kingdoms().getRelation(kk.id, other.id);
                    if (rel == kr.reborn.clan.manager.KingdomManager.Relation.ALLY) allies++;
                    else if (rel == kr.reborn.clan.manager.KingdomManager.Relation.AT_WAR) wars++;
                }
                p.sendMessage("§a동맹 " + allies + "  §c전쟁 " + wars);
                break;
            }
            case "members": {
                var kk = plugin.kingdoms().ofPlayer(p.getUniqueId());
                if (kk == null) { Msg.warn(p, "왕국 소속 없음."); break; }
                Msg.send(p, "&6=== " + kk.name + " 산하 가문 ===");
                for (String cid : kk.clans) p.sendMessage("§e• §f" + cid);
                break;
            }
            case "relations": {
                var kk = plugin.kingdoms().ofPlayer(p.getUniqueId());
                if (kk == null) { Msg.warn(p, "왕국 소속 없음."); break; }
                Msg.send(p, "&6=== " + kk.name + " 외교 관계 ===");
                for (var other : plugin.kingdoms().all()) {
                    if (other.id.equals(kk.id)) continue;
                    var rel = plugin.kingdoms().getRelation(kk.id, other.id);
                    String col = switch (rel) {
                        case ALLY -> "§a";
                        case AT_WAR -> "§4";
                        case ENEMY -> "§c";
                        default -> "§7";
                    };
                    p.sendMessage(col + "• " + other.name + " §8(" + rel.name() + ")");
                }
                break;
            }
            case "ally":
                if (a.length < 2) return true;
                plugin.kingdoms().ally(p, a[1]);
                break;
            case "war":
                if (a.length < 2) return true;
                plugin.kingdoms().declareWar(p, a[1]);
                break;
            case "treaty":
                if (a.length < 2) return true;
                plugin.kingdoms().treaty(p, a[1]);
                break;
            case "marry":
                if (a.length < 3) return true;
                plugin.kingdoms().politicalMarriage(p, a[1], a[2]);
                break;
        }
        return true;
    }
}
