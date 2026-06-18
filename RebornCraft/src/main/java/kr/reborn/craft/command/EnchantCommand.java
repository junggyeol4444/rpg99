package kr.reborn.craft.command;

import kr.reborn.core.util.Msg;
import kr.reborn.craft.RebornCraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class EnchantCommand implements CommandExecutor {
    private final RebornCraft plugin;
    public EnchantCommand(RebornCraft plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] args) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        if (args.length == 0) {
            Msg.send(p, "&7/enchant try [stone] [rune]    - 손에 든 아이템 강화");
            Msg.send(p, "&7/enchant level                 - 손에 든 아이템 강화 정보");
            return true;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        switch (args[0].toLowerCase()) {
            case "try" -> {
                if (!plugin.proficiency().learn(p.getUniqueId(), "enchanter")) {
                    Msg.warn(p, "마법부여사 직업이 필요합니다.");
                    return true;
                }
                String stone = args.length >= 2 ? args[1] : null;
                String rune = args.length >= 3 ? args[2] : null;
                plugin.enchant().tryEnchant(p, hand, stone, rune);
            }
            case "level" -> {
                int lv = plugin.enchant().levelOf(hand);
                String rune = plugin.enchant().runeOf(hand);
                Msg.send(p, "&6강화 레벨: §f+" + lv
                        + (rune != null ? " §b룬: §f" + rune : "")
                        + " §7장착 보너스 +" + (lv * 3));
            }
            default -> Msg.warn(p, "/enchant try|level");
        }
        return true;
    }
}
