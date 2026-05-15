package kr.reborn.ship.command;

import kr.reborn.core.util.Msg;
import kr.reborn.ship.RebornShip;
import kr.reborn.ship.data.Ship;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ShipCommand implements CommandExecutor {
    private final RebornShip plugin;
    public ShipCommand(RebornShip p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            Msg.send(p, "&7/ship register <name> | list | sail <forward|back|left|right> [n] | turn <left|right> | sink <name> | dismantle <name>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "register":
                if (a.length < 2) return true;
                plugin.ships().register(p, a[1]);
                break;
            case "list":
                Msg.send(p, "&6내 배:");
                plugin.ships().ofOwner(p.getUniqueId()).forEach(sh ->
                        p.sendMessage("§e" + sh.name + " §7등급 " + sh.grade + " HP " + sh.hp + "/" + sh.maxHp));
                break;
            case "sail":
                handleSail(p, a);
                break;
            case "turn":
                handleTurn(p, a);
                break;
            case "sink":
                Ship si = pickByName(p, a, 1);
                if (si != null) plugin.movement().sink(si);
                break;
            case "dismantle":
                Msg.warn(p, "해체 (TODO: 블록 환수)");
                break;
            case "join":
                Msg.send(p, "&7선원으로 승선");
                break;
        }
        return true;
    }

    private Ship pickByName(Player p, String[] a, int idx) {
        if (a.length <= idx) {
            var list = plugin.ships().ofOwner(p.getUniqueId());
            return list.isEmpty() ? null : list.get(0);
        }
        for (Ship s : plugin.ships().ofOwner(p.getUniqueId())) {
            if (s.name.equalsIgnoreCase(a[idx])) return s;
        }
        Msg.error(p, "배 없음: " + a[idx]);
        return null;
    }

    private void handleSail(Player p, String[] a) {
        if (a.length < 2) { Msg.warn(p, "/ship sail <forward|back|left|right> [n]"); return; }
        Ship s = pickByName(p, a, 2);
        if (s == null) return;
        int n = a.length > 2 ? safeInt(a[2], 1) : 1;
        // 플레이어 시선 방향 기준
        int dx = 0, dz = 0;
        float yaw = p.getLocation().getYaw();
        // yaw: 0=South(+Z), 90=West(-X), 180=North(-Z), -90/270=East(+X)
        int[] forward = yawForward(yaw);
        switch (a[1].toLowerCase()) {
            case "forward": dx = forward[0]; dz = forward[1]; break;
            case "back":    dx = -forward[0]; dz = -forward[1]; break;
            case "left":    dx = forward[1]; dz = -forward[0]; break;
            case "right":   dx = -forward[1]; dz = forward[0]; break;
        }
        for (int i = 0; i < n; i++) {
            if (!plugin.movement().translate(s, dx, 0, dz)) {
                Msg.error(p, "이동 차단 (충돌 또는 좌초)."); return;
            }
        }
        Msg.send(p, "&3⛵ " + n + "칸 이동");
    }

    private void handleTurn(Player p, String[] a) {
        if (a.length < 2) { Msg.warn(p, "/ship turn <left|right>"); return; }
        Ship s = pickByName(p, a, 2);
        if (s == null) return;
        int deg = "left".equalsIgnoreCase(a[1]) ? -90 : 90;
        if (!plugin.movement().rotate(s, deg)) {
            Msg.error(p, "회전 차단 (충돌)."); return;
        }
        Msg.send(p, "&3⤴ 90도 회전");
    }

    private int[] yawForward(float yaw) {
        // yaw를 4방위로 양자화
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw < 45 || yaw >= 315) return new int[]{0, 1};   // South
        if (yaw < 135) return new int[]{-1, 0};               // West
        if (yaw < 225) return new int[]{0, -1};               // North
        return new int[]{1, 0};                                // East
    }

    private int safeInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
