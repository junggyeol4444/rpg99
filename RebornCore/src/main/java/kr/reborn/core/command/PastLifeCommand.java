package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.reincarnation.PastLife;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PastLifeCommand implements CommandExecutor {
    private final RebornCore plugin;
    public PastLifeCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        var lives = plugin.reincarnationMemory().livesOf(p.getUniqueId());
        if (lives.isEmpty()) {
            Msg.send(p, "&7전생 기록 없음 — 첫 생.");
            return true;
        }
        Msg.send(p, "&5&l╔════════════════════════════╗");
        Msg.send(p, "&5&l║ §f전생 회상 (" + lives.size() + "생)&5&l ║");
        Msg.send(p, "&5&l╚════════════════════════════╝");
        for (PastLife life : lives) {
            p.sendMessage("§e[" + life.reincarnationNumber + "생] §f" + life.world
                    + " §7- 경지 §e" + life.tier
                    + " §7- 스탯 최대 §6" + life.totalStatPeak);
            p.sendMessage("  §7사인: §c" + life.causeOfDeath);
            if (!life.achievements.isEmpty()) {
                p.sendMessage("  §7업적: §a" + life.achievements);
            }
            if (!life.knownSkills.isEmpty() && life.knownSkills.size() <= 5) {
                p.sendMessage("  §7스킬: §b" + life.knownSkills);
            } else if (!life.knownSkills.isEmpty()) {
                p.sendMessage("  §7스킬 " + life.knownSkills.size() + " 종");
            }
            long days = life.durationMs / 86_400_000L;
            p.sendMessage("  §7지속: §f" + days + "일");
        }
        return true;
    }
}
