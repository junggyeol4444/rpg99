package kr.reborn.pet.command;

import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MountCommand implements CommandExecutor {
    private final RebornPet plugin;
    public MountCommand(RebornPet p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length < 1) {
            Msg.send(p, "&7/mount <id>      - 탈것 소환");
            Msg.send(p, "&7/mount dismiss   - 탈것 회수");
            var sec = plugin.getConfig().getConfigurationSection("mounts");
            if (sec != null) Msg.send(p, "&7종류: &f" + String.join(", ", sec.getKeys(false)));
            return true;
        }
        if (a[0].equalsIgnoreCase("dismiss")) {
            plugin.mounts().dismiss(p);
            Msg.send(p, "&7탈것 회수.");
            return true;
        }
        plugin.mounts().summon(p, a[0]);
        return true;
    }
}
