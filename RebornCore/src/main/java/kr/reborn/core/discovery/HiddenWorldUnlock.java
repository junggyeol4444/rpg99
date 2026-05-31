package kr.reborn.core.discovery;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 히든 월드 해금 시스템.
 *
 * 13 정규 세계 외 6 히든 월드:
 *   - ABYSS (심연계)        — DEMON_KI 5000 + 사망 100회
 *   - UNDERWORLD (명계)     — 사망 1회 (자동 해금)
 *   - TIME_REALM (시간계)   — 환생 50회
 *   - DREAM (꿈계)          — MENTAL 500 + INTELLIGENCE 500
 *   - VOID (공허계)         — 모든 13세계 방문 + 신성 1000
 *   - GOD (신계)            — DIVINITY 1000
 *
 * 해금 시:
 *   - 영구 칭호 부여 (RebornTitle)
 *   - 해당 월드 거주 권한 (worldKey 변경 허용)
 *   - broadcast + 보상 스탯
 */
public final class HiddenWorldUnlock {

    private final RebornCore plugin;
    private final Map<UUID, Set<WorldKey>> unlocked = new ConcurrentHashMap<>();

    public HiddenWorldUnlock(RebornCore plugin) {
        this.plugin = plugin;
        // 매 분 모든 온라인 체크
        plugin.scheduler().runTimer(this::tickCheck, 1200L, 1200L);
    }

    public boolean isUnlocked(UUID p, WorldKey w) {
        Set<WorldKey> set = unlocked.get(p);
        return set != null && set.contains(w);
    }

    public Set<WorldKey> unlockedOf(UUID p) {
        return unlocked.getOrDefault(p, java.util.Collections.emptySet());
    }

    private void tickCheck() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = plugin.api().getPlayerData(p.getUniqueId());
            if (d == null) continue;
            checkAll(p, d);
        }
    }

    public void checkAll(Player p, PlayerData d) {
        // ABYSS - 마기 5000 + 사망 100
        checkUnlock(p, d, WorldKey.ABYSS, () ->
                d.getStat(StatType.DEMON_KI) >= 5000 && d.deaths() >= 100);
        // UNDERWORLD - 사망 1회
        checkUnlock(p, d, WorldKey.UNDERWORLD, () -> d.deaths() >= 1);
        // TIME_REALM - 환생 50회
        checkUnlock(p, d, WorldKey.TIME_REALM, () -> d.reincarnations() >= 50);
        // DREAM - 정신 500 + 지능 500
        checkUnlock(p, d, WorldKey.DREAM, () ->
                d.getStat(StatType.MENTAL) >= 500 && d.getStat(StatType.INTELLIGENCE) >= 500);
        // VOID - 13세계 방문 + 신성 1000
        checkUnlock(p, d, WorldKey.VOID, () ->
                d.visited().size() >= 13 && d.getStat(StatType.DIVINITY) >= 1000);
        // GOD - 신성 1000
        checkUnlock(p, d, WorldKey.GOD, () -> d.getStat(StatType.DIVINITY) >= 1000);
    }

    private void checkUnlock(Player p, PlayerData d, WorldKey w,
                             java.util.function.BooleanSupplier cond) {
        if (isUnlocked(p.getUniqueId(), w)) return;
        if (!cond.getAsBoolean()) return;
        unlock(p, w);
    }

    private void unlock(Player p, WorldKey w) {
        Set<WorldKey> set = unlocked.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
        set.add(w);
        String label = labelOf(w);
        Msg.send(p, "&5&l[히든 월드 발견!] §f" + label);
        Bukkit.broadcastMessage("§5§l[히든 월드 발견] §f" + p.getName()
                + " §7가 §6" + label + " §7을 발견했다!");
        // 보상 스탯
        try {
            plugin.api().addStat(p.getUniqueId(), StatType.MENTAL, 50, "hidden-world:" + w);
            plugin.api().addStat(p.getUniqueId(), StatType.LUCK, 30, "hidden-world:" + w);
        } catch (Throwable ignored) {}
        // 칭호 부여 (RebornTitle 리플렉션)
        try {
            var tp = Bukkit.getPluginManager().getPlugin("RebornTitle");
            if (tp != null) {
                Object tm = tp.getClass().getMethod("titles").invoke(tp);
                tm.getClass().getMethod("grant", Player.class, String.class)
                        .invoke(tm, p, "hidden_world_visitor");
                // 6 히든 월드 모두 발견 시 추가 업적
                Object am = tp.getClass().getMethod("achievements").invoke(tp);
                Object set2 = unlocked.get(p.getUniqueId());
                if (set2 instanceof Set<?> ss && ss.size() == 6) {
                    am.getClass().getMethod("grant", Player.class, String.class)
                            .invoke(am, p, "hidden_world_visitor");
                }
            }
        } catch (Throwable ignored) {}
    }

    private String labelOf(WorldKey w) {
        return switch (w) {
            case ABYSS -> "심연계 (深淵界)";
            case UNDERWORLD -> "명계 (冥界)";
            case TIME_REALM -> "시간계 (時間界)";
            case DREAM -> "꿈계 (夢界)";
            case VOID -> "공허계 (虛界)";
            case GOD -> "신계 (神界)";
            default -> w.name();
        };
    }

    public Map<UUID, Set<WorldKey>> allUnlocked() { return unlocked; }
}
