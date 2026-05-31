package kr.reborn.skill.command;

import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.manual.SecretManual;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ManualCommand implements CommandExecutor {
    private final RebornSkill plugin;
    public ManualCommand(RebornSkill plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/manual list                    - 전체 비급 목록");
            Msg.send(p, "&7/manual owned                   - 내 비급");
            Msg.send(p, "&7/manual info <id>               - 비급 정보");
            Msg.send(p, "&7/manual discover <id>           - 발견 시뮬레이션 (테스트)");
            Msg.send(p, "&7/manual research <id>           - 연구 시작");
            Msg.send(p, "&7/manual progress                - 연구 진척도");
            Msg.send(p, "&7/manual steal <npcId>           - 도난 시도");
            Msg.send(p, "&7/manual transfer <player> <id>  - 양도");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                Msg.send(p, "&6=== 전체 비급 (" + plugin.manuals().available().size() + ") ===");
                int n = 0;
                for (SecretManual m : plugin.manuals().available()) {
                    if (n++ >= 25) { p.sendMessage("§7… 추가 다수"); break; }
                    String rarityColor = switch (m.rarity) {
                        case COMMON -> "§7";
                        case RARE -> "§a";
                        case EPIC -> "§5";
                        case LEGENDARY -> "§6";
                        case MYTHIC -> "§c§l";
                    };
                    p.sendMessage("§7• " + rarityColor + m.name
                            + " §8(" + m.id + ") §7" + m.rarity.koreanName);
                }
            }
            case "owned" -> {
                var set = plugin.manuals().ownedOf(p.getUniqueId());
                Msg.send(p, "&6=== 내 비급 (" + set.size() + ") ===");
                for (String id : set) {
                    SecretManual m = plugin.manuals().get(id);
                    if (m != null) p.sendMessage("§7• §f" + m.name + " §8(" + id + ")");
                }
            }
            case "info" -> {
                if (a.length < 2) { Msg.warn(p, "/manual info <id>"); return true; }
                SecretManual m = plugin.manuals().get(a[1]);
                if (m == null) { Msg.error(p, "비급 없음."); return true; }
                Msg.send(p, "&6=== " + m.name + " ===");
                p.sendMessage("§7등급: §f" + m.rarity.koreanName + " §7(시장가 " + m.rarity.marketValue + ")");
                p.sendMessage("§7세계: §e" + m.world);
                p.sendMessage("§7스킬: §b" + m.skillId);
                p.sendMessage("§7연구 시간: §6" + m.researchMinutes + "분");
                p.sendMessage("§7입수처: §c" + m.foundAt);
                p.sendMessage("§7설명: §7" + m.description);
            }
            case "discover" -> {
                if (a.length < 2) { Msg.warn(p, "/manual discover <id>"); return true; }
                plugin.manuals().discover(p, a[1]);
            }
            case "research" -> {
                if (a.length < 2) { Msg.warn(p, "/manual research <id>"); return true; }
                plugin.manuals().startResearch(p, a[1]);
            }
            case "progress" -> {
                var map = plugin.manuals().researchOf(p.getUniqueId());
                if (map.isEmpty()) { Msg.send(p, "&7진행 중인 연구 없음."); return true; }
                Msg.send(p, "&6=== 연구 진척 ===");
                for (var e : map.entrySet()) {
                    double prog = plugin.manuals().researchProgress(p.getUniqueId(), e.getKey());
                    SecretManual m = plugin.manuals().get(e.getKey());
                    if (m != null) {
                        p.sendMessage("§7• " + m.name + " §6" + (int)(prog * 100) + "%");
                    }
                }
            }
            case "steal" -> {
                if (a.length < 2) { Msg.warn(p, "/manual steal <npcId>"); return true; }
                plugin.manuals().stealFromNpc(p, a[1]);
            }
            case "transfer" -> {
                if (a.length < 3) { Msg.warn(p, "/manual transfer <player> <manualId>"); return true; }
                Player target = Bukkit.getPlayerExact(a[1]);
                if (target == null) { Msg.error(p, "오프라인."); return true; }
                plugin.manuals().transferTo(p, target, a[2]);
            }
            default -> Msg.warn(p, "/manual list|owned|info|discover|research|progress|steal|transfer");
        }
        return true;
    }
}
