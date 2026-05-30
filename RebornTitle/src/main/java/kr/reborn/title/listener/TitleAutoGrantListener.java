package kr.reborn.title.listener;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.core.event.RebornStatChangeEvent;
import kr.reborn.title.RebornTitle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 다른 플러그인의 이벤트를 받아 칭호 자동 부여.
 *
 * Hooks:
 *   RebornHiddenClassUnlockEvent → 해당 클래스명 칭호
 *   RebornDeathEvent → 사망 후 부활 시 "재림자" 칭호 (10회 사망)
 *   RebornStatChangeEvent → 절대자 (스탯 총합 5000+)
 *   각 신성 등급 도달 → "신" 칭호
 *
 * 다른 플러그인 이벤트는 reflection을 피하고 직접 리스닝 (모듈은 softdepend).
 */
public final class TitleAutoGrantListener implements Listener {

    private final RebornTitle plugin;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    /** 절대자 칭호 1회 부여 추적 */
    private final java.util.Set<UUID> absoluteTitled = new java.util.HashSet<>();

    public TitleAutoGrantListener(RebornTitle plugin) {
        this.plugin = plugin;
        // 1분마다 모든 온라인 절대자 체크
        RebornCore.get().scheduler().runTimer(this::tickAbsoluteCheck, 1200L, 1200L);
    }

    @EventHandler
    public void onDeath(RebornDeathEvent e) {
        Player p = e.getPlayer();
        int n = deathCount.merge(p.getUniqueId(), 1, Integer::sum);
        if (n == 10 && hasTitle("undying")) {
            plugin.titles().grant(p, "undying");
            Bukkit.broadcastMessage("§5§l[재림자] §f" + p.getName()
                    + " §7가 10번 죽고도 살아남았다 — 칭호 획득!");
        }
        if (n == 100 && hasTitle("immortal_soul")) {
            plugin.titles().grant(p, "immortal_soul");
        }
    }

    @EventHandler
    public void onStatChange(RebornStatChangeEvent e) {
        Player p = e.getPlayer();
        if (absoluteTitled.contains(p.getUniqueId())) return;
        try {
            double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
            if (total >= 5000 && hasTitle("absolute")) {
                plugin.titles().grant(p, "absolute");
                absoluteTitled.add(p.getUniqueId());
                Bukkit.broadcastMessage("§6§l[절대자] §f" + p.getName()
                        + " §7가 스탯 총합 5000을 돌파 — 절대자 칭호!");
            }
        } catch (Throwable ignored) {}
    }

    /** 매 1분 절대자/신성 체크 (모든 온라인). */
    private void tickAbsoluteCheck() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                double divinity = RebornCore.get().api().getStat(p.getUniqueId(), StatType.DIVINITY);
                if (divinity >= 1000 && hasTitle("god_tier") && !plugin.titles().owned(p.getUniqueId()).contains("god_tier")) {
                    plugin.titles().grant(p, "god_tier");
                    Bukkit.broadcastMessage("§6§l[신격] §f" + p.getName() + " §7신성 1000 — 신 칭호!");
                }
                double inner = RebornCore.get().api().getStat(p.getUniqueId(), StatType.INNER_KI);
                if (inner >= 5000 && hasTitle("martial_god") && !plugin.titles().owned(p.getUniqueId()).contains("martial_god")) {
                    plugin.titles().grant(p, "martial_god");
                    Bukkit.broadcastMessage("§5§l[무신] §f" + p.getName() + " §7내공 5000 — 무신 칭호!");
                }
                double demon = RebornCore.get().api().getStat(p.getUniqueId(), StatType.DEMON_KI);
                if (demon >= 5000 && hasTitle("demon_god") && !plugin.titles().owned(p.getUniqueId()).contains("demon_god")) {
                    plugin.titles().grant(p, "demon_god");
                }
                double heaven = RebornCore.get().api().getStat(p.getUniqueId(), StatType.HEAVEN_KI);
                if (heaven >= 5000 && hasTitle("heaven_god") && !plugin.titles().owned(p.getUniqueId()).contains("heaven_god")) {
                    plugin.titles().grant(p, "heaven_god");
                }
                double dragon = RebornCore.get().api().getStat(p.getUniqueId(), StatType.DRAGON_POWER);
                if (dragon >= 1000 && hasTitle("dragon_lord_t") && !plugin.titles().owned(p.getUniqueId()).contains("dragon_lord_t")) {
                    plugin.titles().grant(p, "dragon_lord_t");
                }
                double ocean = RebornCore.get().api().getStat(p.getUniqueId(), StatType.OCEAN_POWER);
                if (ocean >= 1000 && hasTitle("sea_king_t") && !plugin.titles().owned(p.getUniqueId()).contains("sea_king_t")) {
                    plugin.titles().grant(p, "sea_king_t");
                }
                double yokai = RebornCore.get().api().getStat(p.getUniqueId(), StatType.YOKAI_KI);
                if (yokai >= 5000 && hasTitle("yokai_emperor_t") && !plugin.titles().owned(p.getUniqueId()).contains("yokai_emperor_t")) {
                    plugin.titles().grant(p, "yokai_emperor_t");
                }
            } catch (Throwable ignored) {}
        }
    }

    private boolean hasTitle(String id) { return plugin.titles().get(id) != null; }
}
