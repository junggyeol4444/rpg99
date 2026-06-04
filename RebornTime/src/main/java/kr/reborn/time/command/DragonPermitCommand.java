package kr.reborn.time.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * /dragonpermit grant <player> <chamberId>
 *
 * 5대 드래곤 로드 가문 가주(용왕 히든 클래스 보유) 또는 OP가 다른 플레이어에게
 * 시간의 방 1회 허가증을 부여 (용력 500 미달이어도 진입 가능).
 *
 * 기획서 5-12: "용력 500 이상. 또는 드래곤 로드 가문의 허가."
 */
public final class DragonPermitCommand implements CommandExecutor {

    private static final Set<String> VALID_CHAMBERS = Set.of(
            "dragon_chamber_aurelius",
            "dragon_chamber_ignifer",
            "dragon_chamber_nocterna",
            "dragon_chamber_cerylis",
            "dragon_chamber_silvarex"
    );

    private final RebornTime plugin;

    public DragonPermitCommand(RebornTime p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length < 3 || !"grant".equalsIgnoreCase(a[0])) {
            Msg.send(s, "&7/dragonpermit grant <player> <chamberId>");
            Msg.send(s, "&7chamberId: dragon_chamber_aurelius | ignifer | nocterna | cerylis | silvarex");
            return true;
        }
        // 부여자 권한 검증: OP 또는 용왕 히든 클래스 보유
        if (s instanceof Player issuer) {
            if (!issuer.isOp() && !hasDragonKingClass(issuer)) {
                Msg.error(issuer, "용왕 히든 클래스 또는 관리자만 허가증 발급 가능.");
                return true;
            }
        }
        Player target = Bukkit.getPlayerExact(a[1]);
        if (target == null) { Msg.error(s, "대상 플레이어 없음 또는 오프라인: " + a[1]); return true; }
        String chamberId = a[2].toLowerCase();
        if (!VALID_CHAMBERS.contains(chamberId)) {
            Msg.error(s, "유효한 chamberId 필요: " + VALID_CHAMBERS);
            return true;
        }
        PlayerData d = RebornCore.get().api().getPlayerData(target.getUniqueId());
        if (d == null) { Msg.error(s, "대상 PlayerData 없음."); return true; }
        // 허가증 마커 부여 (TimeChamber.enter에서 1회 사용 후 소비)
        d.status().put("dragon_chamber_permit:" + chamberId,
                new PlayerData.StatusEffect(
                        "dragon_chamber_permit:" + chamberId, "DRAGON_PERMIT",
                        Long.MAX_VALUE, 1));
        Msg.send(s, "&6&l[허가증 발급] §f" + target.getName()
                + " §7← §6" + chamberId);
        Msg.send(target, "&6&l[시간의 방 허가증] §f" + chamberId
                + " §7— 용력 부족해도 1회 진입 가능. /chamber " + chamberId);
        return true;
    }

    /** 용왕 히든 클래스 보유 여부 — RebornHiddenClass 리플렉션. */
    private boolean hasDragonKingClass(Player p) {
        try {
            var hcPlugin = Bukkit.getPluginManager().getPlugin("RebornHiddenClass");
            if (hcPlugin == null) return false;
            Object progress = hcPlugin.getClass().getMethod("progress").invoke(hcPlugin);
            Object res = progress.getClass().getMethod("has", java.util.UUID.class, String.class)
                    .invoke(progress, p.getUniqueId(), "dragon_king");
            return Boolean.TRUE.equals(res);
        } catch (Throwable t) { return false; }
    }
}
