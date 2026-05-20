package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ChildCommand implements CommandExecutor {
    private final RebornClan plugin;
    public ChildCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length == 0) return true;
        if ("request".equalsIgnoreCase(a[0])) {
            if (plugin.marriages().of(p.getUniqueId()) == null) {
                Msg.error(p, "결혼한 상태여야 한다.");
                return true;
            }
            double chance = plugin.getConfig().getDouble("child.request-success-chance", 0.30);
            if (Rand.chance(chance)) Msg.send(p, "&d자녀가 태어났다!");
            else Msg.warn(p, "이번에는 임신되지 않았다.");
        } else if ("play".equalsIgnoreCase(a[0])) {
            Msg.send(p, "&7자녀로 전환은 향후 구현 예정 (현재 캐릭터 은퇴 처리 필요).");
        }
        return true;
    }
}
