package kr.reborn.death.abyss;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 심연계 진입·체류 처리.
 * - 진입 시 모든 스탯 50% 즉시 감소
 * - 체류 중 매 분마다 심연 내성 -1, 0이 되면 영혼 소멸
 * - 모든 에너지 오염 (-30%)
 */
public final class AbyssWorld implements Listener {

    private final RebornDeath plugin;
    private final Set<UUID> insideAbyss = new HashSet<>();

    public AbyssWorld(RebornDeath p) {
        this.plugin = p;
        // 1분마다 체류자 정신 잠식
        RebornCore.get().scheduler().runTimer(this::tickAbyssResidents, 1200L, 1200L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) return;
        boolean wasIn = isAbyss(e.getFrom().getWorld());
        boolean nowIn = isAbyss(e.getTo().getWorld());
        if (!wasIn && nowIn) onEnter(e.getPlayer());
        else if (wasIn && !nowIn) onExit(e.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getWorld() == e.getTo().getWorld()) return;
        boolean wasIn = isAbyss(e.getFrom().getWorld());
        boolean nowIn = isAbyss(e.getTo().getWorld());
        if (!wasIn && nowIn) onEnter(e.getPlayer());
        else if (wasIn && !nowIn) onExit(e.getPlayer());
    }

    private boolean isAbyss(World w) {
        return w != null && "abyss".equalsIgnoreCase(w.getName());
    }

    private void onEnter(Player p) {
        insideAbyss.add(p.getUniqueId());
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        // 모든 공통 스탯 50% 즉시 감소 (영구 아닌 buff로 처리하면 좋지만 간이로는 즉시 감소)
        for (StatType t : StatType.COMMON_8) {
            double cur = d.getStat(t);
            d.setStat(t, cur * 0.5);
        }
        // 심연 내성 초기 100 부여
        if (d.getStat(StatType.ABYSS_RESISTANCE) <= 0) {
            d.setStat(StatType.ABYSS_RESISTANCE, 100);
        }
        Bukkit.broadcastMessage("§0§l[심연] §f" + p.getName() + "이(가) 심연에 발을 들였다.");
        Msg.error(p, "&0심연 — 모든 스탯 50% 감소. 매 분 정신 잠식.");
        d.worldKey(WorldKey.ABYSS);
    }

    private void onExit(Player p) {
        insideAbyss.remove(p.getUniqueId());
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        // 스탯 복원 (50% 다시 더해 = 원복)
        for (StatType t : StatType.COMMON_8) {
            d.setStat(t, d.getStat(t) * 2.0);
        }
        Msg.send(p, "&7심연을 벗어났다. 스탯 복원.");
    }

    private void tickAbyssResidents() {
        for (UUID id : insideAbyss) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            PlayerData d = RebornCore.get().api().getPlayerData(id);
            if (d == null) continue;
            d.addStat(StatType.ABYSS_RESISTANCE, -1);
            d.addStat(StatType.MENTAL, -2);
            if (d.getStat(StatType.ABYSS_RESISTANCE) <= 0) {
                // 영혼 흡수 — 영구 사망
                Bukkit.broadcastMessage("§0§l[심연 흡수] §f" + p.getName() + "의 영혼이 심연에 흡수되었다.");
                Bukkit.getPluginManager().callEvent(
                        new RebornDeathEvent(p, p.getLocation(), null, "ABYSS_CONSUMED"));
                p.setHealth(0);
                // 심연계는 윤회 강제 (RebornDeath UnderworldManager에서 처리)
            }
        }
    }
}
