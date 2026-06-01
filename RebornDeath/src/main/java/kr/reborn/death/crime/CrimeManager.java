package kr.reborn.death.crime;

import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrimeManager {

    private final RebornDeath plugin;
    private final ConcurrentHashMap<UUID, Double> crime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> fame = new ConcurrentHashMap<>();

    public CrimeManager(RebornDeath p) { this.plugin = p; }

    public double crime(UUID id) { return crime.getOrDefault(id, 0.0); }
    public double fame(UUID id) { return fame.getOrDefault(id, 0.0); }

    public void onPvpKill(Player killer, Player victim) {
        double inc = plugin.getConfig().getDouble("crime.pk-crime", 100);
        double dec = plugin.getConfig().getDouble("crime.pk-fame", -50);
        // 전시·광폭화 면책 적용
        double reduction = computeImmunityReduction(killer);
        inc *= (1.0 - reduction);
        dec *= (1.0 - reduction);
        crime.merge(killer.getUniqueId(), inc, Double::sum);
        fame.merge(killer.getUniqueId(), dec, Double::sum);
        if (reduction > 0) {
            Bukkit.getPlayer(killer.getUniqueId()).sendMessage(
                "§7전시/광폭 면책 — 범죄 -" + (int)(reduction * 100) + "%");
        }
        announceLevel(killer);
    }

    /** RebornClan 전시 면책 + RebornCurse 광폭화 면책 + Duel 면책 합산. */
    private double computeImmunityReduction(Player killer) {
        double reduction = 0;
        // Duel 결투 중 PvP는 완전 면책
        try {
            if (plugin.duels().isInActiveDuel(killer.getUniqueId())) reduction = 1.0;
        } catch (Throwable ignored) {}
        // RebornClan ClanWar 면책
        try {
            var cp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornClan");
            if (cp != null) {
                Object wars = cp.getClass().getMethod("wars").invoke(cp);
                if (wars != null) {
                    Object res = wars.getClass().getMethod("isImmuneByWar", java.util.UUID.class)
                            .invoke(wars, killer.getUniqueId());
                    if (Boolean.TRUE.equals(res)) reduction = Math.max(reduction, 1.0);
                }
            }
        } catch (Throwable ignored) {}
        // RebornCurse 광폭화 PK 감면
        try {
            var cp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object berserk = cp.getClass().getMethod("berserk").invoke(cp);
                if (berserk != null) {
                    Object res = berserk.getClass().getMethod("pkReductionFor", java.util.UUID.class)
                            .invoke(berserk, killer.getUniqueId());
                    if (res instanceof Number n) reduction = Math.max(reduction, n.doubleValue());
                }
            }
        } catch (Throwable ignored) {}
        return Math.min(1.0, reduction);
    }

    public void tickHourlyDecay() {
        double decay = plugin.getConfig().getDouble("crime.hourly-decay", 10);
        for (UUID id : crime.keySet()) {
            crime.merge(id, -decay, (a, b) -> Math.max(0, a + b));
        }
    }

    public int level(UUID id) {
        var thresholds = plugin.getConfig().getMapList("crime.thresholds");
        double c = crime(id);
        int lv = 0;
        for (Map<?, ?> t : thresholds) {
            double m = ((Number) t.get("min")).doubleValue();
            int l = ((Number) t.get("level")).intValue();
            if (c >= m) lv = l;
        }
        return lv;
    }

    public String label(UUID id) {
        var thresholds = plugin.getConfig().getMapList("crime.thresholds");
        double c = crime(id);
        String label = "일반";
        for (Map<?, ?> t : thresholds) {
            double m = ((Number) t.get("min")).doubleValue();
            String l = String.valueOf(t.get("label"));
            if (c >= m) label = l;
        }
        return label;
    }

    private void announceLevel(Player killer) {
        int lv = level(killer.getUniqueId());
        String label = label(killer.getUniqueId());
        // 범죄 레벨별 다른 헤더 + 메시지
        String header;
        String flavor;
        switch (lv) {
            case 0 -> {
                header = "&7[범죄 기록]";
                flavor = "한 번의 실수일까.";
            }
            case 1 -> {
                header = "&e[범죄 기록]";
                flavor = "경비병이 너의 행적을 주시하기 시작했다.";
            }
            case 2 -> {
                header = "&6[수배 중]";
                flavor = "범행이 누적되어 수배가 떨어졌다.";
            }
            case 3 -> {
                header = "&c[현상수배]";
                flavor = "도시들이 너의 머리에 가격을 매겼다.";
            }
            case 4 -> {
                header = "&c&l[고위 현상수배]";
                flavor = "헌터들이 너를 쫓는다.";
            }
            case 5 -> {
                header = "&4&l[악명 높은 수배자]";
                flavor = "너의 이름이 모든 도시의 공고판에 박혔다.";
            }
            default -> {
                header = "&4&l[전설적 흉포자]";
                flavor = "역사에 남을 만한 악행자.";
            }
        }
        Bukkit.broadcastMessage(kr.reborn.core.util.Msg.PREFIX
                + kr.reborn.core.util.Msg.c(header + " &f" + killer.getName()
                + " — " + label + " §8(" + flavor + ")"));
    }
}
