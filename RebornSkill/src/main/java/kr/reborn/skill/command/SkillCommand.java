package kr.reborn.skill.command;

import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SkillCommand implements CommandExecutor {
    private final RebornSkill plugin;
    public SkillCommand(RebornSkill p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/skill list  |  cast <id>  |  learn <id>  |  equip <slot> <id>");
            Msg.send(p, "&7         |  techniques <id> &8(비급의 초식 보기)");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "techniques": {
                if (a.length < 2) return true;
                String skillId = a[1];
                var def = plugin.registry().get(skillId);
                if (def == null) { Msg.error(p, "스킬 없음: " + skillId); return true; }
                var all = plugin.techniques().of(skillId);
                if (all.isEmpty()) { Msg.send(p, "&7" + def.name + " — 초식 데이터 없음"); return true; }
                int prof = plugin.store().prof(p.getUniqueId(), skillId);
                var unlocked = plugin.techniques().unlocked(skillId, prof);
                Msg.send(p, "&6=== " + def.name + " — 초식 " + all.size() + " (해금 " + unlocked.size() + ", 숙련 " + prof + ") ===");
                for (int i = 0; i < all.size(); i++) {
                    var t = all.get(i);
                    boolean got = i < unlocked.size();
                    String head = got ? "§a" + (i + 1) + ". " : "§8" + (i + 1) + ". §7";
                    String mult = "§7×" + String.format("%.2f", t.mult);
                    String elem = t.elementOverride == null ? "" : " §d[" + t.elementOverride + "]";
                    p.sendMessage(head + "§f" + t.name + " " + mult + elem);
                    if (!t.description.isEmpty() && got) p.sendMessage("    §8" + t.description);
                }
                break;
            }
            case "list":
                Msg.send(p, "&6보유 스킬:");
                for (String id : plugin.store().owned(p.getUniqueId())) {
                    var d = plugin.registry().get(id);
                    p.sendMessage("§e" + id + " §7- " + (d == null ? "?" : d.name) + " §7숙련도 " + plugin.store().prof(p.getUniqueId(), id));
                }
                break;
            case "cast":
                if (a.length < 2) return true;
                plugin.caster().cast(p, a[1]);
                break;
            case "learn":
                if (a.length < 2) return true;
                if (plugin.registry().get(a[1]) == null) { Msg.error(p, "스킬 없음"); return true; }
                plugin.store().learn(p.getUniqueId(), a[1]);
                Msg.send(p, "&a학습 완료.");
                break;
            case "equip":
                if (a.length < 3) return true;
                plugin.store().equip(p.getUniqueId(), Integer.parseInt(a[1]), a[2]);
                Msg.send(p, "&a장착 완료.");
                break;
            case "info":
                if (a.length < 2) return true;
                var d = plugin.registry().get(a[1]);
                if (d == null) { Msg.error(p, "스킬 없음"); return true; }
                Msg.send(p, "&6=== " + d.name + " ===");
                p.sendMessage("§7세계: §e" + d.world + " §7카테고리: §e" + d.category);
                p.sendMessage("§7비용: §c" + (int) d.costAmount + " " + d.costType);
                p.sendMessage("§7쿨다운: §f" + d.cooldownSeconds + "s §7시전: §f" + d.castSeconds + "s");
                p.sendMessage("§7데미지: §c" + d.damageFormula + " §7원소: §5" + d.element);
                p.sendMessage("§7타입: §e" + d.type + " §7범위: §f" + d.radius + " §7사거리: §f" + d.range);
                if (d.durationTicks > 0) p.sendMessage("§7지속: §f" + (d.durationTicks / 20) + "초");
                p.sendMessage("§7학습 방법: §a" + d.learnMethod);
                break;
            case "combo":
                var dq = plugin.combo().recentOf(p.getUniqueId());
                Msg.send(p, "&6=== 최근 콤보 (5초 이내) ===");
                int i = 0;
                for (var rec : dq) {
                    p.sendMessage("§7" + (++i) + ". §f" + rec.skillId
                            + " §8(" + rec.category + "/" + rec.element + ")");
                }
                if (dq.isEmpty()) p.sendMessage("§7기록 없음.");
                break;
            case "catalog":
                Msg.send(p, "&6=== 전체 스킬 카탈로그 (" + plugin.registry().all().size() + ") ===");
                String filter = a.length >= 2 ? a[1].toUpperCase() : null;
                int shown = 0;
                for (var sk : plugin.registry().all()) {
                    if (filter != null && (sk.world == null || !sk.world.name().equals(filter))) continue;
                    if (shown++ >= 40) { p.sendMessage("§7… 추가 다수"); break; }
                    p.sendMessage("§7• §e" + sk.id + " §f" + sk.name
                            + " §8[" + sk.world + "/" + sk.category + "]");
                }
                break;
        }
        return true;
    }
}
