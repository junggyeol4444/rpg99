package kr.reborn.craft.command;

import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.specialty.SpecialtyRecipe;
import kr.reborn.craft.specialty.SpecialtyType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SpecialtyCommand implements CommandExecutor {

    private final RebornCraft plugin;
    public SpecialtyCommand(RebornCraft plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/specialty list                - 모든 특화 종류");
            Msg.send(p, "&7/specialty recipes <TYPE>      - 해당 특화의 레시피");
            Msg.send(p, "&7/specialty craft <recipeId>    - 제작 시도");
            Msg.send(p, "&7/specialty info <TYPE>         - 내 숙련 + 필요 스탯");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                Msg.send(p, "&6=== 12 특화 제작 종류 ===");
                for (SpecialtyType t : SpecialtyType.values()) {
                    p.sendMessage("§7• §e" + t.name() + " §6(" + t.koreanName + ") §7"
                            + t.description + " §8[" + t.primaryStat + "/" + t.secondaryStat + "]");
                }
            }
            case "recipes" -> {
                if (a.length < 2) { Msg.warn(p, "/specialty recipes <TYPE>"); return true; }
                SpecialtyType t;
                try { t = SpecialtyType.valueOf(a[1].toUpperCase()); }
                catch (IllegalArgumentException e) { Msg.error(p, "잘못된 특화: " + a[1]); return true; }
                var list = plugin.specialty().recipesOf(t);
                Msg.send(p, "&6=== " + t.koreanName + " 레시피 (" + list.size() + ") ===");
                for (SpecialtyRecipe r : list) {
                    p.sendMessage("§7• §e" + r.id + " §6" + r.name
                            + " §c난이도 " + r.difficultyTier
                            + " §7Req " + (int) r.requiredPrimaryStat + "/"
                            + (int) r.requiredSecondaryStat + "/숙련" + r.requiredProficiency);
                }
            }
            case "craft" -> {
                if (a.length < 2) { Msg.warn(p, "/specialty craft <recipeId>"); return true; }
                plugin.specialty().tryCraft(p, a[1]);
            }
            case "info" -> {
                if (a.length < 2) { Msg.warn(p, "/specialty info <TYPE>"); return true; }
                SpecialtyType t;
                try { t = SpecialtyType.valueOf(a[1].toUpperCase()); }
                catch (IllegalArgumentException e) { Msg.error(p, "잘못된 특화."); return true; }
                double prof = plugin.specialty().profOf(p.getUniqueId(), t);
                Msg.send(p, "&6=== " + t.koreanName + " ===");
                p.sendMessage("§7설명: §f" + t.description);
                p.sendMessage("§7필요 스탯: §e" + t.primaryStat + " / " + t.secondaryStat);
                p.sendMessage("§7베이스 성공률: §6" + (int) (t.baseSuccessRate * 100) + "%");
                p.sendMessage("§7내 숙련도: §a" + String.format("%.1f", prof) + " / 100");
            }
            default -> Msg.warn(p, "알 수 없는 하위 명령.");
        }
        return true;
    }
}
