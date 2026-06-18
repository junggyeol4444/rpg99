package kr.reborn.death.listener;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class DeathListener implements Listener {

    private final RebornDeath plugin;

    public DeathListener(RebornDeath p) { this.plugin = p; }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Player killer = e.getEntity().getKiller();
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());

        Bukkit.getPluginManager().callEvent(
                new RebornDeathEvent(p, p.getLocation(), killer, killer == null ? "PVE" : "PVP"));

        // 선계 환령단 부활 시도 — 성공 시 명계 이동 건너뜀
        // PlayerDeathEvent는 cancellable이 아니므로 keepInv + 즉시 respawn 패턴
        if (tryImmortalRevival(p)) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.getDrops().clear();
            e.setDroppedExp(0);
            RebornCore.get().scheduler().runEntityTask(p, () -> {
                try { p.spigot().respawn(); } catch (Throwable ignored) {}
            });
            return;
        }

        if (d != null) d.deaths(d.deaths() + 1);
        // 아이템은 사망 지점에 자연스럽게 드랍됨 (바닐라 동작 유지)

        // 명계 이동
        plugin.underworld().sendToUnderworld(p);

        // PvP면 범죄 처리 + 현상금 지급
        if (killer != null) {
            plugin.crime().onPvpKill(killer, p);
            plugin.bounty().onKilled(killer, p.getUniqueId());
            // 천계 거주자가 살인 = 죄목 (기획서 5-3: 살생 금기). HeavenGrowth.onSinAccrued.
            notifyHeavenSin(killer, 25.0);
        }
        // 보험금 자동 지급 (RebornEconomy 리플렉션)
        try {
            var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep != null) {
                Object ins = ep.getClass().getMethod("insurance").invoke(ep);
                if (ins != null) {
                    ins.getClass().getMethod("payoutOnDeath", Player.class)
                            .invoke(ins, p);
                }
            }
        } catch (Throwable ignored) {}
    }

    /** 천계 거주자의 살인 = 죄목 누적. HeavenGrowth.onSinAccrued 리플렉션 호출. */
    private void notifyHeavenSin(Player killer, double weight) {
        try {
            PlayerData kd = RebornCore.get().api().getPlayerData(killer.getUniqueId());
            if (kd == null || kd.worldKey() != WorldKey.HEAVEN) return;
            var sp = Bukkit.getPluginManager().getPlugin("RebornStat");
            if (sp == null) return;
            Object growth = sp.getClass().getMethod("growth").invoke(sp);
            Object strategy = growth.getClass().getMethod("of", WorldKey.class)
                    .invoke(growth, WorldKey.HEAVEN);
            if (strategy == null) return;
            strategy.getClass().getMethod("onSinAccrued", Player.class, double.class)
                    .invoke(strategy, killer, weight);
        } catch (Throwable ignored) {}
    }

    /** 선계 환령단 — ImmortalGrowth 보유자만 1회 한정 부활. */
    private boolean tryImmortalRevival(Player p) {
        try {
            var sp = Bukkit.getPluginManager().getPlugin("RebornStat");
            if (sp == null) return false;
            Object growth = sp.getClass().getMethod("growth").invoke(sp);
            Object strategy = growth.getClass().getMethod("of", WorldKey.class)
                    .invoke(growth, WorldKey.IMMORTAL);
            if (strategy == null) return false;
            // ImmortalGrowth만 tryRevival 메서드 있음
            Object res = strategy.getClass().getMethod("tryRevival", Player.class).invoke(strategy, p);
            return Boolean.TRUE.equals(res);
        } catch (Throwable t) { return false; }
    }

    /** 결투 중 HP <=1 도달 시 죽지 않게 가로채고 결투 종료. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!plugin.duels().isInActiveDuel(victim.getUniqueId())) return;
        double newHp = victim.getHealth() - e.getFinalDamage();
        if (newHp <= 1) {
            e.setCancelled(true);
            try { victim.setHealth(1.0); } catch (Throwable ignored) {}
            plugin.duels().onDamage(victim);
        }
    }

    /** 결투 상대가 아니면 PvP 면책 (다른 사람이 결투 중 사람 못 침). */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!plugin.duels().isInActiveDuel(victim.getUniqueId())
                && !plugin.duels().isInActiveDuel(attacker.getUniqueId())) return;
        // 결투 중 → 결투 상대만 데미지 입힘
        if (!plugin.duels().areOpponents(attacker.getUniqueId(), victim.getUniqueId())) {
            e.setCancelled(true);
        }
    }
}
