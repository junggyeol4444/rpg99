package kr.reborn.curse.command;

import kr.reborn.core.util.Items;
import kr.reborn.core.util.Msg;
import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.ActiveEffect;
import kr.reborn.curse.data.EffectDef;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BuffCommand implements CommandExecutor {
    private final RebornCurse plugin;
    public BuffCommand(RebornCurse plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        var b = plugin.gui().builder("&6축복·저주", 6);
        int slot = 0;
        for (var e : plugin.effects().of(p.getUniqueId()).entrySet()) {
            if (slot >= 54) break;
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null) continue;
            String time = a.isPermanent() ? "&7영구" : "&f" + a.remainingTicks + "초";
            Material mat = def.kind == EffectDef.Kind.BLESSING
                    ? Material.GOLDEN_APPLE : Material.WITHER_SKELETON_SKULL;
            var icon = Items.of(mat, def.name,
                    "&7" + def.description,
                    "&7남은 시간: " + time,
                    "&7중첩: &f" + a.stacks,
                    a.berserkActive ? "&c광폭화 중!" : "");
            b.set(slot++, icon, evt -> {});
        }
        b.open(p);
        return true;
    }
}
