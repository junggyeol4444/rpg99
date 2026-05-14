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
            return true;
        }
        switch (a[0].toLowerCase()) {
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
        }
        return true;
    }
}
