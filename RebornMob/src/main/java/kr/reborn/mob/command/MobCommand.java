package kr.reborn.mob.command;

import kr.reborn.core.util.Msg;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.def.MobDef;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MobCommand implements CommandExecutor {

    private final RebornMob plugin;

    public MobCommand(RebornMob p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.send(s, "&7/rmob spawn <id>  |  boss <id>  |  list  |  reload");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "spawn":
                if (!(s instanceof Player p) || a.length < 2) return true;
                MobDef def = plugin.registry().get(a[1]);
                if (def == null) { Msg.error(s, "정의 없음"); return true; }
                new kr.reborn.mob.spawn.SpawnTicker(plugin).spawn(def, p.getLocation().getChunk());
                Msg.send(s, "&a스폰 완료.");
                break;
            case "boss":
                if (!(s instanceof Player pp) || a.length < 2) return true;
                plugin.bosses().summon(a[1], pp.getLocation());
                break;
            case "list":
                Msg.send(s, "&6몬스터 정의:");
                plugin.registry().all().forEach(d -> s.sendMessage("§e" + d.id + " §7- " + d.name + " (" + d.world + (d.boss ? " BOSS" : "") + ")"));
                break;
            case "reload":
                plugin.reloadConfig();
                plugin.registry().load();
                Msg.send(s, "&a다시 로드 완료.");
                break;
        }
        return true;
    }
}
