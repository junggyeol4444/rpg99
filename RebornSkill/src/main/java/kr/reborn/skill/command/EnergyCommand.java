package kr.reborn.skill.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EnergyCommand implements CommandExecutor {
    private final RebornSkill plugin;
    public EnergyCommand(RebornSkill p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        Msg.send(p, "&6에너지 (현재 세계: " + d.worldKey() + ")");
        for (StatType t : new StatType[]{
                StatType.MANA, StatType.AURA, StatType.DEMON_KI, StatType.HEAVEN_KI,
                StatType.IMMORTAL_KI, StatType.YOKAI_KI, StatType.SPIRIT_POWER,
                StatType.DRAGON_POWER, StatType.OCEAN_POWER, StatType.MAGITECH_ENERGY,
                StatType.CYBER_ADAPTATION, StatType.LEVEL, StatType.INNER_KI, StatType.TAO_POWER}) {
            if (d.getStat(t) > 0) p.sendMessage("§e" + t.name() + ": §f" + d.getStat(t));
        }
        return true;
    }
}
