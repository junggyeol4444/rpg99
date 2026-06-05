package kr.reborn.time.travel;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornWorldChangeEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.time.RebornTime;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class WorldTravelManager {

    private final RebornTime plugin;

    /** 히든 6세계 — HiddenWorldUnlock과 동일. */
    private static final Set<WorldKey> HIDDEN = EnumSet.of(
            WorldKey.ABYSS, WorldKey.UNDERWORLD, WorldKey.TIME_REALM,
            WorldKey.DREAM, WorldKey.VOID, WorldKey.GOD);

    public WorldTravelManager(RebornTime p) { this.plugin = p; }

    public boolean travel(Player p, WorldKey to) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        WorldKey from = d.worldKey();
        if (!canTravel(p, d, from, to)) {
            Msg.error(p, "이동 불가 — 연결권/포탈/절대자 권한/히든 월드 해금 중 하나가 필요하다.");
            return false;
        }
        World w = Bukkit.getWorld(to.name().toLowerCase());
        if (w == null && HIDDEN.contains(to)) {
            // 히든 월드는 lazy 생성 (해금 시점에 처음 진입)
            w = ensureHiddenWorld(to);
        }
        if (w == null) { Msg.error(p, "월드 없음: " + to); return false; }
        // 페이드 아웃 (블라인드)
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 1));
        // Folia: 엔티티 스케줄러로 텔레포트 — 글로벌 스케줄러 사용 시 thread-affinity 위반
        // 단, 엔티티 스케줄러에는 delay 지원이 제한적이므로, 글로벌에서 2초 후 엔티티에 위임
        RebornCore.get().scheduler().runTaskLater(() -> {
            RebornCore.get().scheduler().runEntityTask(p, () -> {
                p.teleport(w.getSpawnLocation());
                d.worldKey(to);
                d.visited().add(to);
                p.sendTitle("§6" + to + " §f도착", "§7" + System.currentTimeMillis(), 5, 40, 10);
                Bukkit.getPluginManager().callEvent(new RebornWorldChangeEvent(p, from, to));
            });
        }, 40L);
        return true;
    }

    private boolean canTravel(Player p, PlayerData d, WorldKey from, WorldKey to) {
        if (from == to) return false;
        // 히든 월드는 해금 시 자유 이동 가능
        if (HIDDEN.contains(to)) {
            try {
                var hidden = RebornCore.get().hiddenWorld();
                if (hidden != null && hidden.isUnlocked(p.getUniqueId(), to)) return true;
            } catch (Throwable ignored) {}
            // fallback: 절대자만 진입
            return RebornCore.get().api().getTotalStats(d.uuid()) >= 5000;
        }
        Set<WorldKey> g1 = group("group1"), g2 = group("group2");
        if (g1.contains(from) && g1.contains(to)) return true;
        if (g2.contains(from) && g2.contains(to)) return true;
        // 절대자 이상이면 자유 이동
        double total = RebornCore.get().api().getTotalStats(d.uuid());
        return total >= 5000;
    }

    private Set<WorldKey> group(String key) {
        Set<WorldKey> s = new HashSet<>();
        for (String n : plugin.getConfig().getStringList("realm-groups." + key)) {
            try { s.add(WorldKey.valueOf(n)); } catch (Exception ignored) {}
        }
        return s;
    }

    /** 히든 월드 lazy 생성 — 해금된 플레이어가 처음 진입할 때만 호출. */
    private World ensureHiddenWorld(WorldKey key) {
        String name = key.name().toLowerCase();
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        try {
            World w = new WorldCreator(name).createWorld();
            if (w == null) return null;
            // 히든 월드 공통 규칙
            try { w.setGameRule(GameRule.KEEP_INVENTORY, true); } catch (Throwable ignored) {}
            try { w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); } catch (Throwable ignored) {}
            try { w.setGameRule(GameRule.DO_MOB_SPAWNING, true); } catch (Throwable ignored) {}
            // 시간계는 시간 고정
            switch (key) {
                case TIME_REALM -> { try { w.setTime(6000); } catch (Throwable ignored) {} }
                case UNDERWORLD -> { try { w.setTime(14000); } catch (Throwable ignored) {} }
                case ABYSS -> { try { w.setTime(18000); } catch (Throwable ignored) {} }
                case DREAM -> { try { w.setTime(13000); } catch (Throwable ignored) {} }
                case VOID -> { try { w.setTime(12500); } catch (Throwable ignored) {} }
                case GOD -> { try { w.setTime(0); w.setStorm(false); } catch (Throwable ignored) {} }
                default -> {}
            }
            plugin.getLogger().info("히든 월드 lazy 생성: " + name);
            return w;
        } catch (Throwable t) {
            plugin.getLogger().warning("히든 월드 생성 실패 " + name + ": " + t.getMessage());
            return null;
        }
    }
}
