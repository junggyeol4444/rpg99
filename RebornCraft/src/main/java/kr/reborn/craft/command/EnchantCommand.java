package kr.reborn.craft.command;

import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.craft.RebornCraft;
import kr.reborn.craft.data.CustomItem;
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
        if (!plugin.proficiency().learn(p.getUniqueId(), "enchanter")) {
            Msg.warn(p, "마법부여사 직업이 필요합니다.");
            return true;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        CustomItem ci = plugin.items().ofItem(hand);
        if (ci == null) { Msg.error(p, "강화 가능한 아이템 없음"); return true; }

        double base = 0.5;
        if (Rand.chance(base)) {
            Msg.send(p, "&a강화 성공! (&f" + ci.name + "&a)");
        } else {
            double breakChance = plugin.getConfig()
                    .getDouble("enchant.fail-break-chance." + ci.grade.name(), 0);
            if (Rand.chance(breakChance)) {
                p.getInventory().setItemInMainHand(null);
                Msg.error(p, "강화 실패 — 아이템이 파괴되었다!");
            } else {
                Msg.warn(p, "강화 실패. 재료만 소모.");
            }
        }
        return true;
    }
}
